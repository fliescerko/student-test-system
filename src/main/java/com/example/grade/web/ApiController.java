package com.example.grade.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class ApiController {
    @GetMapping("/api/healthz")
    public Map<String, String> healthz(){
        return Map.of("status","ok");
    }
}
