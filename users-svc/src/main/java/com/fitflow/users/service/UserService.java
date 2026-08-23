package com.fitflow.users.service;

import com.fitflow.users.dto.AuthResponse;
import com.fitflow.users.dto.LoginRequest;
import com.fitflow.users.dto.RegisterRequest;
import com.fitflow.users.dto.UserProfileResponse;
import com.fitflow.users.entity.User;
import com.fitflow.users.exception.EmailAlreadyExistsException;
import com.fitflow.users.exception.InvalidCredentialsException;
import com.fitflow.users.exception.UserNotFoundException;
import com.fitflow.users.repository.UserRepository;
import com.fitflow.users.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        User saved = userRepository.save(user);
        return UserProfileResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return AuthResponse.bearer(token, jwtService.getExpirationMs());
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return UserProfileResponse.from(user);
    }
}
