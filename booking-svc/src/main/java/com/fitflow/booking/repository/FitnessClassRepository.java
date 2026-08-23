package com.fitflow.booking.repository;

import com.fitflow.booking.entity.ClassStatus;
import com.fitflow.booking.entity.FitnessClass;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FitnessClassRepository extends JpaRepository<FitnessClass, UUID> {

    @Query("""
            select c
            from FitnessClass c
            where c.status = :status
              and c.startTime > :now
              and c.reservedCount < c.capacity
            order by c.startTime asc
            """)
    List<FitnessClass> findAvailableClasses(@Param("status") ClassStatus status, @Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from FitnessClass c where c.id = :id")
    Optional<FitnessClass> findByIdForUpdate(@Param("id") UUID id);
}
