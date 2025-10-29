package com.example.grade.repo;

import com.example.grade.model.Course;
import com.example.grade.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CourseRepo extends JpaRepository<Course, Long> {
    Optional<Course> findByCodeAndTerm(String code, String term);
    Optional<Course> findFirstByOrderByIdAsc();
    Optional<Course> findByCode(String code);
    List<Course> findByTeacher(Teacher teacher);

    @Query("select c from Course c where " +
            "(:now between c.gradeViewStart and c.gradeViewEnd) or " +
            "(c.gradeViewStart is null and c.gradeViewEnd is null)")
    List<Course> findAvailableForView(@Param("now") LocalDateTime now);
}
