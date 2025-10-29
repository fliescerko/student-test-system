package com.example.grade.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity @Table(name="users")
@Getter @Setter
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, unique=true, length=64)
    private String username;
    @Column(nullable=false, length=255)
    private String passwordHash;
    @Column(nullable=false, length=128)
    private String email;
    @Column(nullable=false, length=16)
    private String role; // ADMIN/TEACHER/STUDENT
    @Column(nullable=false)
    private Boolean active = true;
    private Instant createdAt = Instant.now();
}
