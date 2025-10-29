package com.example.grade.config;

import com.example.grade.model.*;
import com.example.grade.repo.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

@Configuration
public class DataInit {

    @Bean
    CommandLineRunner seedData(
            UserRepo userRepo,
            TeacherRepo teacherRepo,
            StudentRepo studentRepo,
            CourseRepo courseRepo,
            GradeItemRepo gradeItemRepo,
            GradeRepo gradeRepo,
            PasswordEncoder encoder
    ) {
        return args -> {

            System.out.println("🚀 数据初始化开始...");

            // ====================================================
            // 1️⃣ 用户初始化
            // ====================================================
            createUserIfAbsent(userRepo, encoder, "admin", "ADMIN", "admin@example.com");

            for (int i = 1; i <= 3; i++) {
                createUserIfAbsent(userRepo, encoder, "teacher" + i, "TEACHER", "teacher" + i + "@example.com");
            }
            for (int i = 1; i <= 10; i++) {
                createUserIfAbsent(userRepo, encoder, "student" + i, "STUDENT", "student" + i + "@example.com");
            }

            // ====================================================
            // 2️⃣ 教师表
            // ====================================================
            Teacher t1 = createTeacherIfAbsent(teacherRepo, "张老师", "T001", userRepo.findByUsername("teacher1").orElseThrow());
            Teacher t2 = createTeacherIfAbsent(teacherRepo, "李老师", "T002", userRepo.findByUsername("teacher2").orElseThrow());
            Teacher t3 = createTeacherIfAbsent(teacherRepo, "王老师", "T003", userRepo.findByUsername("teacher3").orElseThrow());

            // ====================================================
            // 3️⃣ 学生表（补充班级和年级信息）
            // ====================================================
            // 定义班级和年级数据（将10个学生分配到2个班级）
            String[][] classes = {
                    {"1班", "一"},
                    {"2班", "一"}
            };

            for (int i = 1; i <= 10; i++) {
                String studentNo = "S00" + i;
                // 分配班级（前5名学生到1班，后5名到2班）
                int classIndex = (i <= 5) ? 0 : 1;

                // 先查询学生是否存在
                Optional<Student> studentOpt = studentRepo.findByStudentNo(studentNo);
                if (studentOpt.isPresent()) {
                    // 如果存在，更新班级和年级信息
                    Student existingStudent = studentOpt.get();
                    // 仅在字段为空时更新，避免重复操作
                    if (existingStudent.getClassName() == null || existingStudent.getGrade() == null) {
                        existingStudent.setClassName(classes[classIndex][0]);
                        existingStudent.setGrade(classes[classIndex][1]);
                        studentRepo.save(existingStudent);
                        System.out.println("更新学生：" + existingStudent.getFullName() + " 的班级信息为：" + existingStudent.getClassName());
                    } else {
                        System.out.println("学生 " + studentNo + " 已存在且班级信息完整，跳过更新");
                    }
                    continue;
                }

                // 如果不存在，创建新学生（保留原逻辑）
                Student s = new Student();
                s.setFullName("学生" + i);
                s.setStudentNo(studentNo);
                s.setUser(userRepo.findByUsername("student" + i).orElseThrow());
                s.setClassName(classes[classIndex][0]);  // 新增班级信息
                s.setGrade(classes[classIndex][1]);      // 新增年级信息
                studentRepo.save(s);
                System.out.println("创建学生：" + s.getFullName() + "，班级：" + s.getClassName());
            }

            List<Student> allStudents = studentRepo.findAll();

            // ====================================================
            // 4️⃣ 课程（与教师绑定，防重复）
            // ====================================================
            createCourseIfAbsent(courseRepo, "CHN101", "语文", "2025秋", t1);
            createCourseIfAbsent(courseRepo, "MAT101", "数学", "2025秋", t2);
            createCourseIfAbsent(courseRepo, "ENG101", "英语", "2025秋", t3);
            createCourseIfAbsent(courseRepo, "SCI101", "科学", "2025秋", t1);

            List<Course> allCourses = courseRepo.findAll();

            // ====================================================
            // 5️⃣ 成绩项（每门课程两个，防重复）
            // ====================================================
            for (Course c : allCourses) {
                if (gradeItemRepo.findByCourseAndName(c, "平时成绩").isEmpty()) {
                    GradeItem normal = new GradeItem();
                    normal.setCourse(c);
                    normal.setName("平时成绩");
                    normal.setWeight(40);
                    normal.setIsFinal(false);
                    gradeItemRepo.save(normal);
                }
                if (gradeItemRepo.findByCourseAndName(c, "期末成绩").isEmpty()) {
                    GradeItem finalExam = new GradeItem();
                    finalExam.setCourse(c);
                    finalExam.setName("期末成绩");
                    finalExam.setWeight(60);
                    finalExam.setIsFinal(true);
                    gradeItemRepo.save(finalExam);
                }
            }

            List<GradeItem> allGradeItems = gradeItemRepo.findAll();

            // ====================================================
            // 6️⃣ 成绩（防重复）
            // ====================================================
            Random random = new Random();
            for (Student s : allStudents) {
                for (GradeItem item : allGradeItems) {
                    if (gradeRepo.findByStudentAndGradeItem(s, item).isPresent()) {
                        continue; // 已有成绩则跳过
                    }
                    Grade g = new Grade();
                    g.setStudent(s);
                    g.setGradeItem(item);
                    g.setCourse(item.getCourse());
                    g.setScore(60.0 + random.nextInt(41));
                    gradeRepo.save(g);
                }
            }

            // ====================================================
            // ✅ 完成日志（补充班级统计）
            // ====================================================
            System.out.println("✅ 数据初始化完成");
            System.out.println("用户数量: " + userRepo.count());
            System.out.println("教师数量: " + teacherRepo.count());
            System.out.println("学生数量: " + studentRepo.count());
            System.out.println("1班学生数: " + studentRepo.findByClassName("1班").size());
            System.out.println("2班学生数: " + studentRepo.findByClassName("2班").size());
            System.out.println("课程数量: " + courseRepo.count());
            System.out.println("成绩项数量: " + gradeItemRepo.count());
            System.out.println("成绩数量: " + gradeRepo.count());
        };
    }

    // ====================================================
    // 🔧 工具方法：防重复创建
    // ====================================================

    private User createUserIfAbsent(UserRepo repo, PasswordEncoder encoder, String username, String role, String email) {
        return repo.findByUsername(username).orElseGet(() -> {
            User u = new User();
            u.setUsername(username);
            u.setPasswordHash(encoder.encode("password"));
            u.setEmail(email);
            u.setRole(role);
            u.setActive(true);
            System.out.println("创建用户：" + username);
            return repo.save(u);
        });
    }

    private Teacher createTeacherIfAbsent(TeacherRepo repo, String fullName, String no, User user) {
        return repo.findByTeacherNo(no).orElseGet(() -> {
            Teacher t = new Teacher();
            t.setFullName(fullName);
            t.setTeacherNo(no);
            t.setUser(user);
            System.out.println("创建教师：" + fullName);
            return repo.save(t);
        });
    }

    private Course createCourseIfAbsent(CourseRepo repo, String code, String name, String term, Teacher teacher) {
        return repo.findByCode(code).orElseGet(() -> {
            Course c = new Course();
            c.setCode(code);
            c.setName(name);
            c.setTerm(term);
            c.setTeacher(teacher);
            System.out.println("创建课程：" + name + "（教师：" + teacher.getFullName() + "）");
            return repo.save(c);
        });
    }
}