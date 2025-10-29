package com.example.grade.service;

import com.example.grade.model.*;
import com.example.grade.repo.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

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

    public record ImportResult(int success, int fail) {}
}
