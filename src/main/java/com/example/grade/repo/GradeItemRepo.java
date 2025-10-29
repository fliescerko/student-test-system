package com.example.grade.repo;

import com.example.grade.model.Course;
import com.example.grade.model.GradeItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GradeItemRepo extends JpaRepository<GradeItem, Long> {
    Optional<GradeItem> findByCourseIdAndName(Long courseId, String name);
    Optional<GradeItem> findByCourseAndName(Course course, String name);

    List<GradeItem> findByCourse(Course course);
}
