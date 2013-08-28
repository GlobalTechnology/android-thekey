package me.thekey.android;

public class TheKeyException extends Exception {
    private static final long serialVersionUID = -5964985435313974517L;

    public TheKeyException() {
        super();
    }

    public TheKeyException(final String detailMessage, final Throwable throwable) {
        super(detailMessage, throwable);
    }

    public TheKeyException(final String detailMessage) {
        super(detailMessage);
    }

    public TheKeyException(final Throwable throwable) {
        super(throwable);
    }
}
