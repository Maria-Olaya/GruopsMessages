package eafit.gruopChat.group.exception;

public class DuplicateChannelNameException extends RuntimeException {
    public DuplicateChannelNameException(String name) {
        super("Channel with name '" + name + "' already exists in this group");
    }
}