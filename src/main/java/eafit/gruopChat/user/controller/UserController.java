package eafit.gruopChat.user.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import eafit.gruopChat.shared.enums.Role;
import eafit.gruopChat.user.dto.AuthResponseDTO;
import eafit.gruopChat.user.dto.LoginRequestDTO;
import eafit.gruopChat.user.dto.UserRequestDTO;
import eafit.gruopChat.user.dto.UserResponseDTO;
import eafit.gruopChat.user.service.UserService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ================= REGISTER (público) =================
    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody UserRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }

    // ================= LOGIN (público) =================
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(userService.login(request));
    }

    // ================= GETTERS =================
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponseDTO> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/active")
    public ResponseEntity<List<UserResponseDTO>> getAllActiveUsers() {
        return ResponseEntity.ok(userService.getAllActiveUsers());
    }

    // ================= UPDATE (usa token) =================
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Long id,
            @AuthenticationPrincipal Long requestingUserId,  // 👈 del token
            @Valid @RequestBody UserRequestDTO request) {

        // Solo puede editarse a sí mismo
        if (!id.equals(requestingUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(
            @PathVariable Long id,
            @AuthenticationPrincipal Long requestingUserId,  // 👈 del token
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {

        if (!id.equals(requestingUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        userService.changePassword(id, oldPassword, newPassword);
        return ResponseEntity.noContent().build();
    }

    // ================= STATE (solo ADMIN de app) =================
    @PatchMapping("/{id}/disable")
    public ResponseEntity<Void> disableUser(@PathVariable Long id) {
        userService.disableUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/enable")
    public ResponseEntity<Void> enableUser(@PathVariable Long id) {
        userService.enableUser(id);
        return ResponseEntity.noContent().build();
    }

    // ================= ROLE =================
    @PatchMapping("/{id}/role")
    public ResponseEntity<Void> changeRole(@PathVariable Long id, @RequestParam Role role) {
        userService.changeRole(id, role);
        return ResponseEntity.noContent().build();
    }

    // ================= DELETE =================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal Long requestingUserId) {  // 👈 del token

        if (!id.equals(requestingUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}