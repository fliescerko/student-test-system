package com.example.grade.web;

import com.example.grade.model.Course;
import com.example.grade.repo.CourseRepo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final CourseRepo courseRepo;  // 添加 final 修饰符并通过构造函数注入

    // 构造函数注入 CourseRepo
    public HomeController(CourseRepo courseRepo) {
        this.courseRepo = courseRepo;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails user, Model model) {
        // 确保用户对象不为 null
        if (user != null) {
            model.addAttribute("user", user);
            model.addAttribute("userEmail", user.getUsername());
            String role = user.getAuthorities().iterator().next().getAuthority();
            model.addAttribute("userRole", role.replace("ROLE_", ""));
        }

        // 添加课程信息
        addCourseInfo(model);

        return "index";
    }

    @GetMapping("/")
    public String home(@AuthenticationPrincipal UserDetails user, Model model) {
        // 同样处理首页的用户和课程信息
        if (user != null) {
            model.addAttribute("user", user);
            model.addAttribute("userEmail", user.getUsername());
            String role = user.getAuthorities().iterator().next().getAuthority();
            model.addAttribute("userRole", role.replace("ROLE_", ""));
        }

        addCourseInfo(model);

        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // 提取共用的课程信息添加逻辑
    private void addCourseInfo(Model model) {
        Course defaultCourse = courseRepo.findFirstByOrderByIdAsc().orElse(null);
        model.addAttribute("course", defaultCourse);
        model.addAttribute("courses", courseRepo.findAll());
    }
}