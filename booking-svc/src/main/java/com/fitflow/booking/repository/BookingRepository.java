package com.fitflow.booking.repository;

import com.fitflow.booking.entity.Booking;
import com.fitflow.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    @Query("""
            select count(b) > 0
            from Booking b
            where b.userId = :userId
              and b.fitnessClass.id = :classId
              and b.status = :status
            """)
    boolean existsBooking(@Param("userId") UUID userId,
                          @Param("classId") UUID classId,
                          @Param("status") BookingStatus status);

    List<Booking> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
