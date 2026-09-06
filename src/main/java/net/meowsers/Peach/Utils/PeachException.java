package net.meowsers.Peach.Utils;

public class PeachException extends RuntimeException {
    public PeachException(String message) {
        super(message);
    }
    public PeachException(String message, Throwable cause) {
        super(message, cause);
    }
}
