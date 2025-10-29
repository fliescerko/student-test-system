package com.example.grade.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity @Table(name="courses")
@Getter @Setter
public class Course {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, length=32)
    private String code;
    @Column(nullable=false, length=128)
    private String name;
    @Column(nullable=false, length=16)
    private String term; // e.g. 2025-SPR
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(name = "grade_view_start")
    private LocalDateTime gradeViewStart;

    @Column(name = "grade_view_end")
    private LocalDateTime gradeViewEnd;
}
