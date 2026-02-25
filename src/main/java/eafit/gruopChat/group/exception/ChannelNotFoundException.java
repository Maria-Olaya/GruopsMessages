package eafit.gruopChat.group.exception;

public class ChannelNotFoundException extends RuntimeException {
    public ChannelNotFoundException(Long id) {
        super("Channel not found with id: " + id);
    }
}