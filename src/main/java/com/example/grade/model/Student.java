package com.example.grade.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="students")
@Getter @Setter
public class Student {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne @JoinColumn(name="user_id", unique=true)
    private User user;
    @Column(nullable=false, unique=true, length=32)
    private String studentNo;
    @Column(nullable=false, length=64)
    private String fullName;
    @Column(length=32)
    private String className;

    // 新增年级字段
    @Column(length=16)
    private String grade;
}
