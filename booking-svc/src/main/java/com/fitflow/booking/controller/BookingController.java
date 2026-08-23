package com.fitflow.booking.controller;

import com.fitflow.booking.dto.BookingResponse;
import com.fitflow.booking.dto.CreateBookingRequest;
import com.fitflow.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.create(request));
    }

    @GetMapping("/{id}")
    public BookingResponse getById(@PathVariable UUID id) {
        return bookingService.getById(id);
    }

    @GetMapping("/users/{userId}")
    public List<BookingResponse> getByUser(@PathVariable UUID userId) {
        return bookingService.getByUser(userId);
    }

    @DeleteMapping("/{id}")
    public BookingResponse cancel(@PathVariable UUID id) {
        return bookingService.cancel(id);
    }
}
