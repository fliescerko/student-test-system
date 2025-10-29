package com.example.grade.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="teachers")
@Getter @Setter
public class Teacher {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne @JoinColumn(name="user_id", unique=true)
    private User user;
    @Column(nullable=false, unique=true, length=32)
    private String teacherNo;
    @Column(nullable=false, length=64)
    private String fullName;
}
