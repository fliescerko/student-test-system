package com.example.grade.repo;
import com.example.grade.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserRepo extends JpaRepository<User, Long> {

    // 通过用户名查找用户
    Optional<User> findByUsername(String username);

    // 通过电子邮件查找用户
    Optional<User> findByEmail(String email);

    // 查找所有激活的用户
    List<User> findByActiveTrue();

    // 通过用户名和激活状态查找用户
    Optional<User> findByUsernameAndActiveTrue(String username);

    // 通过角色查找用户，假设 User 中有 role 字段
    List<User> findByRole(String role);
}

