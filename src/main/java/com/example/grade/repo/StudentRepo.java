package com.example.grade.repo;

import com.example.grade.model.Course;
import com.example.grade.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepo extends JpaRepository<Student, Long> {
    Optional<Student> findByStudentNo(String studentNo);
    Optional<Student> findByUserUsername(String username);

    // 按班级查询学生
    List<Student> findByClassName(String className);

    // 按年级查询学生
    List<Student> findByGrade(String grade);

    // 模糊查询
    @Query("SELECT s FROM Student s WHERE s.fullName LIKE %:keyword% OR s.studentNo LIKE %:keyword%")
    List<Student> searchStudents(@Param("keyword") String keyword);
}
