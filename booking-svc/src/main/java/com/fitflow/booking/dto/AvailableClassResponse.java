package com.fitflow.booking.dto;

import com.fitflow.booking.entity.FitnessClass;

import java.time.Instant;
import java.util.UUID;

public record AvailableClassResponse(
        UUID id,
        String name,
        String description,
        String instructorName,
        Instant startTime,
        Instant endTime,
        int capacity,
        int reservedCount,
        int availableSpots
) {
    public static AvailableClassResponse from(FitnessClass fitnessClass) {
        return new AvailableClassResponse(
                fitnessClass.getId(),
                fitnessClass.getName(),
                fitnessClass.getDescription(),
                fitnessClass.getInstructorName(),
                fitnessClass.getStartTime(),
                fitnessClass.getEndTime(),
                fitnessClass.getCapacity(),
                fitnessClass.getReservedCount(),
                fitnessClass.availableSpots()
        );
    }
}
