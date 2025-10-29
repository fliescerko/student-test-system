package com.example.grade.web.dto;
public class GradeDTO {
    private Long studentId;      // 学生ID
    private Long gradeItemId;    // 评分项ID
    private Double score;        // 分数

    // getter 和 setter
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Long getGradeItemId() { return gradeItemId; }
    public void setGradeItemId(Long gradeItemId) { this.gradeItemId = gradeItemId; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
}