package com.example.grade.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CourseTotalDTO {
    private Long courseId;
    private String courseCode;
    private String courseName;
    private Double weightedSum;
    private Double weightSum;

    public double getTotal() {
        if (weightSum == null || weightSum == 0.0) return 0.0;
        return weightedSum; // 已经是加权结果
    }
}
