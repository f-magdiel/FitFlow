package com.fitflow.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private FitnessClass fitnessClass;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant cancelledAt;

    public static Booking confirmed(UUID userId, FitnessClass fitnessClass) {
        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setFitnessClass(fitnessClass);
        booking.setStatus(BookingStatus.CONFIRMED);
        return booking;
    }

    public void cancel() {
        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
