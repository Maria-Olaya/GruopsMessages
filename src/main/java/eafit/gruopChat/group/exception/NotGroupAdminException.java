package eafit.gruopChat.group.exception;

public class NotGroupAdminException extends RuntimeException {
    public NotGroupAdminException() {
        super("Only group admins can perform this action");
    }
}