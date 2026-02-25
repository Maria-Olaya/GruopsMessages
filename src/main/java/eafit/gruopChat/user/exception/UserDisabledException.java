package eafit.gruopChat.user.exception;

public class UserDisabledException extends RuntimeException {

    public UserDisabledException(Long userId) {
        super("User account is disabled. userId: " + userId);
    }
}