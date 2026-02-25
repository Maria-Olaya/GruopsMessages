package eafit.gruopChat.group.exception;

public class AlreadyMemberException extends RuntimeException {
    public AlreadyMemberException(Long userId, Long groupId) {
        super("User " + userId + " is already a member of group " + groupId);
    }
}