package eafit.gruopChat.user.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eafit.gruopChat.infrastructure.security.JwtService;
import eafit.gruopChat.shared.enums.Role;
import eafit.gruopChat.user.dto.*;
import eafit.gruopChat.user.exception.*;
import eafit.gruopChat.user.model.User;
import eafit.gruopChat.user.repository.UserRepository;
import eafit.gruopChat.user.service.UserService;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // ================= REGISTER =================

    @Override
    public UserResponseDTO register(UserRequestDTO request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.ROLE_USER);
        user.setEnabled(true);
        user.setPhoneNumber(request.phoneNumber());
        user.setProfilePictureUrl(request.profilePictureUrl());

        return mapToDTO(userRepository.save(user));
    }

    // ================= LOGIN =================

    @Override
    public AuthResponseDTO login(LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isEnabled()) {
            throw new UserDisabledException(user.getUserId());
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        user.setLastLoginAt(LocalDateTime.now());

        // Generar token JWT real
        String token = jwtService.generateToken(user);

        return new AuthResponseDTO(
                token,
                user.getUserId(),
                user.getName(),
                user.getRole(),
                jwtService.getExpirationSeconds()
        );
    }

    // ================= GETTERS =================

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        if (!user.isEnabled()) throw new UserNotFoundException(id);
        return mapToDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserByEmail(String email) {
        User user = userRepository.findByEmailAndEnabledTrue(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        return mapToDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findByEnabledTrue()
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllActiveUsers() {
        return userRepository.findByEnabledTrue()
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // ================= UPDATE =================

    @Override
    public UserResponseDTO updateUser(Long id, UserRequestDTO request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (!user.getEmail().equals(request.email())
                && userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        user.setName(request.name());
        user.setEmail(request.email());
        user.setPhoneNumber(request.phoneNumber());
        user.setProfilePictureUrl(request.profilePictureUrl());
        return mapToDTO(user);
    }

    // ================= PASSWORD =================

    @Override
    public void changePassword(Long id, String oldPassword, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
    }

    // ================= STATE =================

    @Override
    public void disableUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setEnabled(false);
    }

    @Override
    public void enableUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setEnabled(true);
    }

    // ================= ROLE =================

    @Override
    public void changeRole(Long id, Role newRole) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setRole(newRole);
    }

    // ================= DELETE =================

    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setEnabled(false);
    }

    // ================= MAPPER =================

    private UserResponseDTO mapToDTO(User user) {
        return new UserResponseDTO(
                user.getUserId(), user.getName(), user.getEmail(),
                user.getRole(), user.isEnabled(), user.getCreatedAt(),
                user.getProfilePictureUrl(), user.getPhoneNumber());
    }
}