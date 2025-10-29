package com.example.grade.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="grades",
  uniqueConstraints = @UniqueConstraint(name="uk_student_item", columnNames={"student_id","grade_item_id"}))
@Getter @Setter
public class Grade {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne @JoinColumn(name="student_id")
    private Student student;
    @ManyToOne @JoinColumn(name="grade_item_id")
    private GradeItem gradeItem;
    @Column(nullable=false)
    private Double score;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public GradeItem getGradeItem() {
        return gradeItem;
    }

    public void setGradeItem(GradeItem gradeItem) {
        this.gradeItem = gradeItem;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }
}
