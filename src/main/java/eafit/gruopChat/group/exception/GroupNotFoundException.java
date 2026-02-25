package eafit.gruopChat.group.exception;

public class GroupNotFoundException extends RuntimeException {
    public GroupNotFoundException(Long id) {
        super("Group not found with id: " + id);
    }
}