package com.fitflow.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fitness_classes")
@Getter
@Setter
@NoArgsConstructor
public class FitnessClass {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, length = 120)
    private String instructorName;

    @Column(nullable = false)
    private Instant startTime;

    @Column(nullable = false)
    private Instant endTime;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private int reservedCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClassStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public static FitnessClass create(String name, String description, String instructorName,
                                      Instant startTime, Instant endTime, int capacity,
                                      ClassStatus status) {
        FitnessClass fitnessClass = new FitnessClass();
        fitnessClass.setName(name);
        fitnessClass.setDescription(description);
        fitnessClass.setInstructorName(instructorName);
        fitnessClass.setStartTime(startTime);
        fitnessClass.setEndTime(endTime);
        fitnessClass.setCapacity(capacity);
        fitnessClass.setReservedCount(0);
        fitnessClass.setStatus(status);
        return fitnessClass;
    }

    public boolean isAvailableForBooking(Instant now) {
        return status == ClassStatus.AVAILABLE && startTime.isAfter(now) && reservedCount < capacity;
    }

    public int availableSpots() {
        return capacity - reservedCount;
    }

    public void reserveSpot() {
        this.reservedCount++;
    }

    public void releaseSpot() {
        if (reservedCount > 0) {
            this.reservedCount--;
        }
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
