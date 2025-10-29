package com.example.grade.web;

import com.example.grade.model.Course;
import com.example.grade.model.Grade;
import com.example.grade.model.GradeItem;
import com.example.grade.model.Student;
import com.example.grade.model.Teacher;
import com.example.grade.repo.CourseRepo;
import com.example.grade.repo.GradeItemRepo;
import com.example.grade.repo.GradeRepo;
import com.example.grade.repo.StudentRepo;
import com.example.grade.service.ImportService;
import com.example.grade.service.TeacherService;
import com.example.grade.web.dto.GradeDTO;
import com.example.grade.web.dto.GradeListDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/teacher")
public class TeacherController {

    private static final Logger log = LoggerFactory.getLogger(TeacherController.class);

    private final TeacherService teacherService;
    private final CourseRepo courseRepo;
    private final StudentRepo studentRepo;
    private final GradeItemRepo gradeItemRepo;
    private final GradeRepo gradeRepo;
    private final ImportService importService;
    public TeacherController(TeacherService teacherService, CourseRepo courseRepo,
                             StudentRepo studentRepo, GradeItemRepo gradeItemRepo,
                             GradeRepo gradeRepo,ImportService importService) {
        this.teacherService = teacherService;
        this.courseRepo = courseRepo;
        this.studentRepo = studentRepo;
        this.gradeItemRepo = gradeItemRepo;
        this.gradeRepo = gradeRepo;
        this.importService=importService;
    }

    // 显示成绩录入页面
    @GetMapping("/input-grade")
    public String showInputPage(@RequestParam(required = false) Long courseId,
                                Model model, Principal principal) {
        String username = principal.getName();
        log.info("教师[{}]访问成绩录入页面", username);

        // 获取教师教授的所有课程
        List<Course> courses = teacherService.getCoursesByTeacher(username);
        model.addAttribute("courses", courses);

        if (courseId != null) {
            // 获取选中的课程
            Course course = courseRepo.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("课程不存在：" + courseId));
            model.addAttribute("selectedCourse", course);

            // 通过成绩表查询选了该课程的学生（去重）
            List<Grade> grades = gradeRepo.findByCourse(course);
            List<Student> students = grades.stream()
                    .map(Grade::getStudent)
                    .distinct()
                    .collect(Collectors.toList()); // 使用collect确保兼容性
            model.addAttribute("students", students);
            log.info("课程[{}]的学生数量：{}", course.getName(), students.size());

            // 获取该课程的评分项
            List<GradeItem> gradeItems = gradeItemRepo.findByCourse(course);
            model.addAttribute("gradeItems", gradeItems);

            // 构建成绩映射（学生ID_评分项ID -> 成绩）
            Map<String, Grade> gradeMap = new HashMap<>();
            for (Student student : students) {
                for (GradeItem item : gradeItems) {
                    gradeRepo.findByStudentAndGradeItem(student, item).ifPresent(grade -> {
                        gradeMap.put(student.getId() + "_" + item.getId(), grade);
                    });
                }
            }
            model.addAttribute("gradeMap", gradeMap);
        }
        return "teacher/inputGrade";
    }

    // 批量更新成绩（表格提交）
    @PostMapping("/update-grades")
    public String updateGrades(@RequestParam Long courseId,
                               @ModelAttribute GradeListDTO gradeList,
                               Principal principal,
                               RedirectAttributes redirect) {

        String username = principal.getName();  // ✅ 修复：定义变量
        List<GradeDTO> grades = gradeList.getGrades();
        log.info("教师[{}]批量更新课程[{}]的成绩，共{}条记录", username, courseId, grades.size());

        // 1. 验证课程和教师权限（逻辑不变）
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("课程不存在：" + courseId));
        Teacher teacher = teacherService.getTeacherByUsername(username);
        if (!course.getTeacher().equals(teacher)) {
            throw new SecurityException("教师[" + username + "]无权操作课程[" + courseId + "]");
        }

        // 2. 处理每条成绩
        for (GradeDTO dto : grades) {
            try {
                Student student = studentRepo.findById(dto.getStudentId())
                        .orElseThrow(() -> new IllegalArgumentException("学生不存在：" + dto.getStudentId()));
                GradeItem item = gradeItemRepo.findById(dto.getGradeItemId())
                        .orElseThrow(() -> new IllegalArgumentException("评分项不存在：" + dto.getGradeItemId()));

                Grade grade = gradeRepo.findByStudentAndGradeItem(student, item)
                        .orElseGet(Grade::new);
                grade.setStudent(student);
                grade.setGradeItem(item);
                grade.setCourse(course);
                grade.setScore(dto.getScore());

                gradeRepo.save(grade);
            } catch (Exception e) {
                log.error("处理成绩失败：{}", e.getMessage());
            }
        }

        redirect.addFlashAttribute("msg", "成绩更新成功！");
        return "redirect:/teacher/input-grade?courseId=" + courseId;

    }
    @GetMapping("/upload")
    public String showUploadPage() {
        // 跳转到上传表单页面
        return "teacher/upload";
    }
    // 在 TeacherController 中添加
    @PostMapping("/upload")
    public String handleUpload(@RequestParam("file") MultipartFile file,
                               Principal principal,
                               RedirectAttributes redirect) {
        if (file.isEmpty()) {
            redirect.addFlashAttribute("error", "请选择要上传的文件");
            return "redirect:/teacher/upload";
        }

        try {
            // 传递当前教师用户名进行权限验证
            ImportService.ImportResult result = importService.importCsv(file, principal.getName());
            log.info("文件上传完成：成功{}条，失败{}条", result.success(), result.fail());
            redirect.addFlashAttribute("msg",
                    String.format("上传成功！成功导入%d条，失败%d条", result.success(), result.fail()));
        } catch (SecurityException e) {
            log.error("权限验证失败", e);
            redirect.addFlashAttribute("error", "上传失败：" + e.getMessage() + "，您只能上传自己负责课程的成绩");
        } catch (Exception e) {
            log.error("文件上传失败", e);
            redirect.addFlashAttribute("error", "上传失败：" + e.getMessage());
        }

        return "redirect:/teacher/upload";
    }

    // 显示导入方式选择页面
    @GetMapping("/select-import-method")
    public String showImportMethodSelection(Principal principal) {
        log.info("教师[{}]访问导入方式选择页面", principal.getName());
        return "teacher/selectImportMethod";
    }


    // TeacherController.java 中显示设置页面的方法
    @GetMapping("/course-settings")
    public String showCourseSettings(
            @RequestParam(required = false) Long courseId,
            Model model,
            Principal principal) {

        // 只查询当前教师的课程
        String teacherUsername = principal.getName();
        Teacher currentTeacher = teacherService.getTeacherByUsername(teacherUsername);
        List<Course> teacherCourses = courseRepo.findByTeacher(currentTeacher); // 关键：只查自己的课程

        model.addAttribute("courses", teacherCourses); // 前端只能看到自己的课程

        // 后续逻辑...
        if (courseId != null) {
            // 即使手动传入courseId，也要再次验证是否属于当前教师
            Course course = courseRepo.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("课程不存在"));
            if (!course.getTeacher().getId().equals(currentTeacher.getId())) {
                throw new AccessDeniedException("无权访问此课程");
            }
            model.addAttribute("selectedCourse", course);
        }

        return "teacher/courseSettings";
    }

    // TeacherController.java 中保存时间设置的方法
    @PostMapping("/save-course-settings")
    public String saveCourseSettings(
            @RequestParam Long courseId,
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime,
            Principal principal,
            RedirectAttributes redirect) {

        // 1. 获取当前登录教师信息
        String teacherUsername = principal.getName();
        Teacher currentTeacher = teacherService.getTeacherByUsername(teacherUsername);
        if (currentTeacher == null) {
            throw new SecurityException("未找到教师信息");
        }

        // 2. 获取课程信息并验证所有权
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("课程不存在：" + courseId));

        // 3. 验证权限
        if (!course.getTeacher().getId().equals(currentTeacher.getId())) {
            throw new AccessDeniedException("您没有权限设置此课程的成绩查看时间");
        }

        // 4. 保存设置（关键修改：允许同时为空）
        course.setGradeViewStart(startTime);
        course.setGradeViewEnd(endTime);
        courseRepo.save(course);

        // 5. 根据是否设置时间显示不同提示
        String message;
        if (startTime != null && endTime != null) {
            message = "成绩查看时间范围已设置";
        } else {
            message = "成绩查看时间限制已取消，学生可随时查看";
        }
        redirect.addFlashAttribute("msg", message);
        return "redirect:/teacher/course-settings?courseId=" + courseId;
    }
}