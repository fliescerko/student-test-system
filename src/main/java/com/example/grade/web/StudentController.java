package com.example.grade.web;

import com.example.grade.repo.GradeRepo;
import com.example.grade.repo.StudentRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {
    private final StudentRepo studentRepo;
    private final GradeRepo gradeRepo;

    // StudentController.java 修改成绩查询方法
    @GetMapping("/grades")
    public String myGrades(@AuthenticationPrincipal UserDetails ud,
                           @RequestParam String term, Model model) {

        var stu = studentRepo.findByUserUsername(ud.getUsername())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No student profile linked to this account"));

        LocalDateTime now = LocalDateTime.now();
        model.addAttribute("term", term);
        // 使用带时间范围过滤的查询方法
        model.addAttribute("totals", gradeRepo.computeTotalsWithinRange(stu.getId(), term, now));
        model.addAttribute("details", gradeRepo.findDetailsByStudentAndTermWithinRange(stu.getId(), term, now));

        return "student/grades";
    }

}

