package com.fitflow.booking.config;

import com.fitflow.booking.entity.ClassStatus;
import com.fitflow.booking.entity.FitnessClass;
import com.fitflow.booking.repository.FitnessClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final FitnessClassRepository fitnessClassRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (fitnessClassRepository.count() > 0) {
            return;
        }

        Instant now = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        fitnessClassRepository.saveAll(List.of(
                FitnessClass.create(
                        "Yoga Flow Online",
                        "Clase de yoga para movilidad, respiracion y fuerza base.",
                        "Andrea Lopez",
                        now.plus(1, ChronoUnit.DAYS),
                        now.plus(1, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS),
                        12,
                        ClassStatus.AVAILABLE
                ),
                FitnessClass.create(
                        "HIIT Express",
                        "Entrenamiento intenso de 30 minutos para todo el cuerpo.",
                        "Carlos Mendez",
                        now.plus(2, ChronoUnit.DAYS),
                        now.plus(2, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES),
                        10,
                        ClassStatus.AVAILABLE
                ),
                FitnessClass.create(
                        "Pilates Core",
                        "Sesion enfocada en postura, abdomen y control corporal.",
                        "Mariana Ruiz",
                        now.plus(3, ChronoUnit.DAYS),
                        now.plus(3, ChronoUnit.DAYS).plus(45, ChronoUnit.MINUTES),
                        8,
                        ClassStatus.AVAILABLE
                )
        ));
    }
}
