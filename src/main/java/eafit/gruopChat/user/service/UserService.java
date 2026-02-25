package eafit.gruopChat.user.service;

import java.util.List;

import eafit.gruopChat.shared.enums.Role;
import eafit.gruopChat.user.dto.AuthResponseDTO;
import eafit.gruopChat.user.dto.LoginRequestDTO;
import eafit.gruopChat.user.dto.UserRequestDTO;
import eafit.gruopChat.user.dto.UserResponseDTO;

public interface UserService {

    UserResponseDTO register(UserRequestDTO request);

    AuthResponseDTO login(LoginRequestDTO request);

    UserResponseDTO getUserById(Long id);

    UserResponseDTO getUserByEmail(String email);

    List<UserResponseDTO> getAllUsers();

    List<UserResponseDTO> getAllActiveUsers();

    UserResponseDTO updateUser(Long id, UserRequestDTO request);

    void changePassword(Long id, String oldPassword, String newPassword);

    void disableUser(Long id);

    void enableUser(Long id);

    void changeRole(Long id, Role newRole);

    void deleteUser(Long id);
}