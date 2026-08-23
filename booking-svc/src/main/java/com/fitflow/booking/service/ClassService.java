package com.fitflow.booking.service;

import com.fitflow.booking.dto.AvailableClassResponse;
import com.fitflow.booking.entity.ClassStatus;
import com.fitflow.booking.repository.FitnessClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassService {

    private final FitnessClassRepository fitnessClassRepository;

    @Transactional(readOnly = true)
    public List<AvailableClassResponse> getAvailableClasses() {
        return fitnessClassRepository.findAvailableClasses(ClassStatus.AVAILABLE, Instant.now()).stream()
                .map(AvailableClassResponse::from)
                .toList();
    }
}
