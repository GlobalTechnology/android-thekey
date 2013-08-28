package me.thekey.android;

public class TheKeySocketException extends TheKeyException {
    private static final long serialVersionUID = -8970831070058243609L;

    public TheKeySocketException() {
        super();
    }

    public TheKeySocketException(final String detailMessage, final Throwable throwable) {
        super(detailMessage, throwable);
    }

    public TheKeySocketException(final String detailMessage) {
        super(detailMessage);
    }

    public TheKeySocketException(final Throwable throwable) {
        super(throwable);
    }
}
