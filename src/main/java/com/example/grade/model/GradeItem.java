package com.example.grade.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="grade_items")
@Getter @Setter
public class GradeItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
    @Column(nullable=false, length=64)
    private String name; // 期末/总评
    @Column(nullable=false)
    private Integer weight = 100;
    @Column(nullable=false)
    private Boolean isFinal = true;
}
