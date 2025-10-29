package com.example.grade.repo;

import com.example.grade.model.Teacher;
import com.example.grade.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherRepo extends JpaRepository<Teacher, Long> {

    // 通过教师编号查询（唯一字段）
    Optional<Teacher> findByTeacherNo(String teacherNo);

    // 通过用户ID查询（唯一字段）
    Optional<Teacher> findByUser_Id(Long userId);

    // 通过教师姓名模糊查询（可选，用于搜索）
    Optional<Teacher> findByFullNameContaining(String fullName);


    Optional<Object> findByUser(User user);
    Optional<Teacher> findByUserUsername(String username);
}
