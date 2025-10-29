package com.example.grade.service;

import com.example.grade.model.User;
import com.example.grade.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service

public class UserService {

    @Autowired
    private UserRepo userRepo;

    // 注入Spring Security的密码编码器
    @Autowired
    private PasswordEncoder passwordEncoder;

    // 修改密码加密方式（删除原PasswordUtil调用）
    public void updateUserPassword(Long userId, String plainPassword) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        // 使用PasswordEncoder加密密码
        String encryptedPassword = passwordEncoder.encode(plainPassword);
        user.setPasswordHash(encryptedPassword); // 保持与数据库字段一致
        userRepo.save(user);
    }
}

