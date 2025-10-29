package com.example.grade.service;

import com.example.grade.model.Student;
import com.example.grade.repo.StudentRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {
    private final StudentRepo studentRepo;

    // 获取所有学生
    public List<Student> getAllStudents() {
        return studentRepo.findAll();
    }

    // 根据ID获取学生
    public Student getStudentById(Long id) {
        return studentRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("学生不存在: " + id));
    }

    // 创建或更新学生
    @Transactional
    public Student saveStudent(Student student) {
        return studentRepo.save(student);
    }

    // 删除学生
    @Transactional
    public void deleteStudent(Long id) {
        studentRepo.deleteById(id);
    }

    // 搜索学生
    public List<Student> searchStudents(String keyword) {
        return studentRepo.searchStudents(keyword);
    }
}