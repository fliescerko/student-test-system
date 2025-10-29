package com.example.grade.service;


import com.example.grade.model.Course;
import com.example.grade.model.Teacher;
import com.example.grade.model.User;
import com.example.grade.repo.CourseRepo;
import com.example.grade.repo.TeacherRepo;
import com.example.grade.repo.UserRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeacherService {

    private final TeacherRepo teacherRepo;
    private final CourseRepo courseRepo;

    private final UserRepo userRepo;

    public TeacherService(TeacherRepo teacherRepo, CourseRepo courseRepo, UserRepo userRepo) {
        this.teacherRepo = teacherRepo;
        this.courseRepo = courseRepo;
        this.userRepo = userRepo; // 添加注入
    }

    // 获取当前教师对象
    public Teacher getTeacherByUsername(String username) {
        // 先通过用户名查询用户，再通过用户ID查询教师
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return teacherRepo.findByUser_Id(user.getId())
                .orElseThrow(() -> new RuntimeException("教师不存在"));
    }


    // 获取教师负责的课程
    public List<Course> getCoursesByTeacher(String username) {
        Teacher teacher = getTeacherByUsername(username);
        return courseRepo.findByTeacher(teacher);
    }
}
