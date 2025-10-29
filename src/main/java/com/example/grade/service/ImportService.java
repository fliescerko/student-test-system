package com.example.grade.service;

import com.example.grade.model.*;
import com.example.grade.repo.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImportService {

    private final StudentRepo studentRepo;
    private final CourseRepo courseRepo;
    private final GradeItemRepo gradeItemRepo;
    private final GradeRepo gradeRepo;
    private final TeacherRepo teacherRepo;
    @Transactional
    public ImportResult importCsv(MultipartFile file, String teacherUsername) {
        int success = 0, fail = 0;
        Teacher currentTeacher = teacherRepo.findByUserUsername(teacherUsername)
                .orElseThrow(() -> new SecurityException("未找到教师信息"));

        try (var br = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            var parser = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(br);

            for (CSVRecord r : parser) {
                try {
                    String studentNo = r.get("studentNo").trim();
                    String courseCode = r.get("courseCode").trim();
                    String term = r.get("term").trim();
                    String itemName = r.get("itemName").trim();
                    int weight = Integer.parseInt(r.get("weight").trim());
                    double score = Double.parseDouble(r.get("score").trim());

                    // === 1️⃣ 查学生 ===
                    Student student = studentRepo.findByStudentNo(studentNo)
                            .orElseThrow(() -> new IllegalArgumentException("学生不存在: " + studentNo));

                    // === 2️⃣ 查课程 ===
                    Course course = courseRepo.findByCodeAndTerm(courseCode, term)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "课程不存在: " + courseCode + " (" + term + ")"));

                    if (!course.getTeacher().getId().equals(currentTeacher.getId())) {
                        throw new SecurityException("无权操作课程: " + courseCode + " (" + term + ")");
                    }


                    // === 3️⃣ 查评分项 ===
                    GradeItem gradeItem = gradeItemRepo.findByCourseIdAndName(course.getId(), itemName)
                            .orElseGet(() -> {
                                GradeItem gi = new GradeItem();
                                gi.setCourse(course);
                                gi.setName(itemName);
                                gi.setWeight(weight);
                                gi.setIsFinal(itemName.contains("期末"));
                                return gradeItemRepo.save(gi);
                            });

                    // === 4️⃣ 查成绩记录 ===
                    Grade grade = gradeRepo.findByStudentAndGradeItem(student, gradeItem)
                            .orElse(null);

                    if (grade == null) {
                        // INSERT
                        grade = new Grade();
                        grade.setCourse(course);
                        grade.setStudent(student);
                        grade.setGradeItem(gradeItem);
                        grade.setScore(score);
                        gradeRepo.save(grade);
                    } else {
                        // UPDATE
                        grade.setScore(score);
                        gradeRepo.save(grade);
                    }

                    success++;
                } catch (SecurityException e) {
                    // 安全验证失败，终止整个导入
                    throw e;
                } catch (Exception e) {
                    fail++;
                    System.err.println("导入失败：" + e.getMessage());
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("CSV文件读取失败", e);
        }

        return new ImportResult(success, fail);
    }
    @Transactional
    public ImportResult importExcel(MultipartFile file, String teacherUsername) {
        int success = 0, fail = 0;

        Teacher currentTeacher = teacherRepo.findByUserUsername(teacherUsername)
                .orElseThrow(() -> new SecurityException("未找到教师信息"));

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) throw new IllegalArgumentException("Excel 内容为空");

            // 读取表头 -> 列索引（大小写不敏感）
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) throw new IllegalArgumentException("缺少表头行");

            Map<String, Integer> col = new HashMap<>();
            for (Cell c : header) {
                String key = cellString(c).trim().toLowerCase();
                if (!key.isEmpty()) col.put(key, c.getColumnIndex());
            }
            // 必需列
            requireCols(col, "studentno", "coursecode", "term", "itemname", "weight", "score");

            // 数据行
            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                try {
                    String studentNo = cellString(row.getCell(col.get("studentno"))).trim();
                    String courseCode = cellString(row.getCell(col.get("coursecode"))).trim();
                    String term = cellString(row.getCell(col.get("term"))).trim();
                    String itemName = cellString(row.getCell(col.get("itemname"))).trim();
                    int weight = cellInt(row.getCell(col.get("weight")));
                    double score = cellDouble(row.getCell(col.get("score")));

                    // 1) 学生
                    Student student = studentRepo.findByStudentNo(studentNo)
                            .orElseThrow(() -> new IllegalArgumentException("学生不存在: " + studentNo));

                    // 2) 课程（含学期）
                    Course course = courseRepo.findByCodeAndTerm(courseCode, term)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "课程不存在: " + courseCode + " (" + term + ")"));

                    // 仅能导入自己负责的课程
                    if (!course.getTeacher().getId().equals(currentTeacher.getId())) {
                        throw new SecurityException("无权操作课程: " + courseCode + " (" + term + ")");
                    }

                    // 3) 评分项（按课程+名称；无则创建）
                    GradeItem gradeItem = gradeItemRepo.findByCourseIdAndName(course.getId(), itemName)
                            .orElseGet(() -> {
                                GradeItem gi = new GradeItem();
                                gi.setCourse(course);
                                gi.setName(itemName);
                                gi.setWeight(weight);
                                gi.setIsFinal(itemName.contains("期末"));
                                return gradeItemRepo.save(gi);
                            });

                    // 4) 成绩 upsert
                    Grade grade = gradeRepo.findByStudentAndGradeItem(student, gradeItem).orElse(null);
                    if (grade == null) {
                        grade = new Grade();
                        grade.setCourse(course);
                        grade.setStudent(student);
                        grade.setGradeItem(gradeItem);
                        grade.setScore(score);
                        gradeRepo.save(grade);
                    } else {
                        grade.setScore(score);
                        gradeRepo.save(grade);
                    }

                    success++;
                } catch (SecurityException se) {
                    // 与 CSV 保持一致：权限失败直接中断整个导入
                    throw se;
                } catch (Exception e) {
                    fail++;
                    System.err.println("第 " + (r + 1) + " 行导入失败：" + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Excel 文件读取失败", e);
        }

        return new ImportResult(success, fail);
    }

    /* ---------- 辅助方法，与 CSV 版风格一致 ---------- */
    private static void requireCols(Map<String, Integer> idx, String... keys) {
        for (String k : keys) {
            if (!idx.containsKey(k)) {
                throw new IllegalArgumentException("缺少必需列：" + k);
            }
        }
    }

    private static String cellString(Cell c) {
        if (c == null) return "";
        if (c.getCellType() == CellType.STRING) return c.getStringCellValue();
        if (c.getCellType() == CellType.NUMERIC) return String.valueOf(c.getNumericCellValue());
        if (c.getCellType() == CellType.BOOLEAN) return String.valueOf(c.getBooleanCellValue());
        if (c.getCellType() == CellType.FORMULA) {
            try { return c.getStringCellValue(); } catch (Exception ignored) {}
            try { return String.valueOf(c.getNumericCellValue()); } catch (Exception ignored) {}
        }
        return "";
    }

    private static int cellInt(Cell c) {
        if (c == null) throw new IllegalArgumentException("整数列为空");
        if (c.getCellType() == CellType.NUMERIC) return (int) Math.round(c.getNumericCellValue());
        String s = cellString(c).trim();
        return Integer.parseInt(s);
    }

    private static double cellDouble(Cell c) {
        if (c == null) throw new IllegalArgumentException("数字列为空");
        if (c.getCellType() == CellType.NUMERIC) return c.getNumericCellValue();
        String s = cellString(c).trim();
        return Double.parseDouble(s);
    }


    public record ImportResult(int success, int fail) {}
}
