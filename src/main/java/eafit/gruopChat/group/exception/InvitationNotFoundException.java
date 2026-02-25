package eafit.gruopChat.group.exception;

public class InvitationNotFoundException extends RuntimeException {
    public InvitationNotFoundException(Long id) {
        super("Invitation not found with id: " + id);
    }
}