package com.example.grade.web;

import com.example.grade.model.Student;
import com.example.grade.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/students")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // 仅管理员可访问
public class AdminStudentController {
    private final StudentService studentService;

    // 学生列表页面
    @GetMapping
    public String listStudents(@RequestParam(required = false) String keyword, Model model) {
        if (keyword != null && !keyword.isEmpty()) {
            model.addAttribute("students", studentService.searchStudents(keyword));
            model.addAttribute("keyword", keyword);
        } else {
            model.addAttribute("students", studentService.getAllStudents());
        }
        return "admin/student/list";
    }

    // 新增学生页面
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("student", new Student());
        model.addAttribute("title", "新增学生");
        return "admin/student/form";
    }

    // 编辑学生页面
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("student", studentService.getStudentById(id));
        model.addAttribute("title", "编辑学生");
        return "admin/student/form";
    }

    // 保存学生信息
    @PostMapping
    public String saveStudent(@ModelAttribute Student student, RedirectAttributes redirect) {
        studentService.saveStudent(student);
        redirect.addFlashAttribute("msg", "学生信息已保存");
        return "redirect:/admin/students";
    }

    // 删除学生
    @GetMapping("/delete/{id}")
    public String deleteStudent(@PathVariable Long id, RedirectAttributes redirect) {
        studentService.deleteStudent(id);
        redirect.addFlashAttribute("msg", "学生已删除");
        return "redirect:/admin/students";
    }
}