package com.fitflow.booking.service;

import com.fitflow.booking.client.NotificationClient;
import com.fitflow.booking.client.UsersClient;
import com.fitflow.booking.dto.BookingResponse;
import com.fitflow.booking.dto.CreateBookingRequest;
import com.fitflow.booking.entity.Booking;
import com.fitflow.booking.entity.BookingStatus;
import com.fitflow.booking.entity.FitnessClass;
import com.fitflow.booking.exception.BookingAlreadyCancelledException;
import com.fitflow.booking.exception.BookingNotFoundException;
import com.fitflow.booking.exception.ClassNotAvailableException;
import com.fitflow.booking.exception.ClassNotFoundException;
import com.fitflow.booking.exception.DuplicateBookingException;
import com.fitflow.booking.repository.BookingRepository;
import com.fitflow.booking.repository.FitnessClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final FitnessClassRepository fitnessClassRepository;
    private final UsersClient usersClient;
    private final NotificationClient notificationClient;

    @Transactional
    public BookingResponse create(CreateBookingRequest request) {
        usersClient.validateUserExists(request.userId());

        FitnessClass fitnessClass = fitnessClassRepository.findByIdForUpdate(request.classId())
                .orElseThrow(() -> new ClassNotFoundException(request.classId()));

        if (!fitnessClass.isAvailableForBooking(Instant.now())) {
            throw new ClassNotAvailableException(request.classId());
        }

        if (bookingRepository.existsBooking(
                request.userId(), request.classId(), BookingStatus.CONFIRMED)) {
            throw new DuplicateBookingException();
        }

        fitnessClass.reserveSpot();
        Booking booking = bookingRepository.save(Booking.confirmed(request.userId(), fitnessClass));
        BookingResponse response = BookingResponse.from(booking);

        notificationClient.sendBookingConfirmed(response);
        return response;
    }

    @Transactional(readOnly = true)
    public BookingResponse getById(UUID id) {
        return bookingRepository.findById(id)
                .map(BookingResponse::from)
                .orElseThrow(() -> new BookingNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getByUser(UUID userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(BookingResponse::from)
                .toList();
    }

    @Transactional
    public BookingResponse cancel(UUID id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingAlreadyCancelledException(id);
        }

        FitnessClass fitnessClass = fitnessClassRepository.findByIdForUpdate(booking.getFitnessClass().getId())
                .orElseThrow(() -> new ClassNotFoundException(booking.getFitnessClass().getId()));

        booking.cancel();
        fitnessClass.releaseSpot();
        BookingResponse response = BookingResponse.from(booking);

        notificationClient.sendBookingCancelled(response);
        return response;
    }
}
