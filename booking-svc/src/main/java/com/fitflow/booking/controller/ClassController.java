package com.fitflow.booking.controller;

import com.fitflow.booking.dto.AvailableClassResponse;
import com.fitflow.booking.service.ClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    @GetMapping("/available")
    public List<AvailableClassResponse> getAvailableClasses() {
        return classService.getAvailableClasses();
    }
}
