package com.example.grade.repo;
import com.example.grade.model.Course;
import com.example.grade.model.Grade;
import com.example.grade.model.GradeItem;
import com.example.grade.model.Student;
import com.example.grade.web.dto.CourseTotalDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GradeRepo extends JpaRepository<Grade, Long> {

    // 学期内学生的明细（含课程与评分项）
    @Query("""
    select g from Grade g
      join fetch g.gradeItem gi
      join fetch gi.course c
    where g.student.id = :sid and c.term = :term
  """)
    List<Grade> findDetailsByStudentAndTerm(Long sid, String term);

    // 学期内学生的课程总评（按权重聚合）
    @Query("""
select new com.example.grade.web.dto.CourseTotalDTO(
  c.id, c.code, c.name,
  cast(sum(g.score * (gi.weight/100.0)) as double),
  cast(sum(gi.weight) as double)
)
from Grade g
  join g.gradeItem gi
  join gi.course c
where g.student.id = :sid and c.term = :term
group by c.id, c.code, c.name
""")


    List<CourseTotalDTO> computeTotals(Long sid, String term);
    List<Grade> findByCourse(Course course);
;
    Optional<Grade> findByStudentAndGradeItem(Student student, GradeItem gradeItem);

    // GradeRepo.java
// 修改成绩查询方法，增加时间范围过滤
    @Query("""
select g from Grade g
  join fetch g.gradeItem gi
  join fetch gi.course c
where g.student.id = :sid and c.term = :term and
  (:now between c.gradeViewStart and c.gradeViewEnd or 
   c.gradeViewStart is null and c.gradeViewEnd is null)
""")
    List<Grade> findDetailsByStudentAndTermWithinRange(
            @Param("sid") Long sid,
            @Param("term") String term,
            @Param("now") LocalDateTime now);

    @Query("""
select new com.example.grade.web.dto.CourseTotalDTO(
  c.id, c.code, c.name,
  cast(sum(g.score * (gi.weight/100.0)) as double),
  cast(sum(gi.weight) as double)
)
from Grade g
  join g.gradeItem gi
  join gi.course c
where g.student.id = :sid and c.term = :term and
  (:now between c.gradeViewStart and c.gradeViewEnd or 
   c.gradeViewStart is null and c.gradeViewEnd is null)
group by c.id, c.code, c.name
""")
    List<CourseTotalDTO> computeTotalsWithinRange(
            @Param("sid") Long sid,
            @Param("term") String term,
            @Param("now") LocalDateTime now);
}


