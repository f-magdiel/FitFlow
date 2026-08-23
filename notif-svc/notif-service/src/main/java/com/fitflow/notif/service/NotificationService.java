package com.fitflow.notif.service;

import com.fitflow.notif.dao.entity.NotificationEntity;
import com.fitflow.notif.dao.entity.NotificationStatus;
import com.fitflow.notif.dao.repository.NotificationRepository;
import com.fitflow.notif.service.model.CreateNotificationCommand;
import com.fitflow.notif.service.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification send(CreateNotificationCommand command) {
        NotificationEntity entity = new NotificationEntity(
                command.userId(),
                command.type(),
                command.message(),
                NotificationStatus.SENT,
                Instant.now()
        );

        NotificationEntity saved = notificationRepository.save(entity);

        // Task 1 allows the actual notification delivery to be represented by a log.
        LOGGER.info(
                "Notification sent: id={}, userId={}, type={}, message={}",
                saved.getId(),
                saved.getUserId(),
                saved.getType(),
                saved.getMessage()
        );

        return toDomain(saved);
    }

    @Transactional(readOnly = true)
    public List<Notification> getHistory(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private Notification toDomain(NotificationEntity entity) {
        return new Notification(
                entity.getId(),
                entity.getUserId(),
                entity.getType(),
                entity.getMessage(),
                entity.getStatus().name(),
                entity.getCreatedAt()
        );
    }
}
