package eafit.gruopChat.group.exception;

public class NotMemberException extends RuntimeException {
    public NotMemberException(Long userId, Long groupId) {
        super("User " + userId + " is not a member of group " + groupId);
    }
}