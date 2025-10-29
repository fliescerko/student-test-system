package com.example.grade.service;

import com.example.grade.repo.UserRepo;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DbUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepo userRepo;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 修改 DbUserDetailsService 的 loadUserByUsername 方法
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        com.example.grade.model.User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // 使用用户实际角色（添加 ROLE_ 前缀，Spring Security 要求）
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                user.getActive(), // 使用数据库中的激活状态
                true, true, true,
                AuthorityUtils.createAuthorityList("ROLE_" + user.getRole())
        );
    }

    // 验证密码是否匹配
    public boolean checkPassword(String rawPassword, String hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }
}

