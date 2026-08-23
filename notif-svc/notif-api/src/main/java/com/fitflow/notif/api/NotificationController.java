package com.fitflow.notif.api;

import com.fitflow.notif.api.dto.CreateNotificationRequest;
import com.fitflow.notif.api.dto.NotificationResponse;
import com.fitflow.notif.service.NotificationService;
import com.fitflow.notif.service.model.CreateNotificationCommand;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse send(@Valid @RequestBody CreateNotificationRequest request) {
        return NotificationResponse.from(notificationService.send(
                new CreateNotificationCommand(request.userId(), request.type(), request.message())
        ));
    }

    @GetMapping("/users/{userId}")
    public List<NotificationResponse> getHistory(@PathVariable String userId) {
        return notificationService.getHistory(userId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }
}
