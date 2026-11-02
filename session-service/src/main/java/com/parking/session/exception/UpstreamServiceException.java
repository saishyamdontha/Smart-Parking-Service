package com.parking.session.exception;

/** A failed call to another service, with which service failed and how (used as a diagnostic code). */
public class UpstreamServiceException extends RuntimeException {

    public enum Target { PARKING, ACTIVITY }
    public enum Kind { UNREACHABLE, TIMEOUT, HTTP_5XX, HTTP_4XX }

    private final Target target;
    private final Kind kind;
    private final Integer status;

    public UpstreamServiceException(Target target, Kind kind, Integer status, String message) {
        super(message);
        this.target = target;
        this.kind = kind;
        this.status = status;
    }

    /** Kept for compatibility with older call sites. */
    public UpstreamServiceException(String message) {
        this(null, null, null, message);
    }

    public String code() {
        return target == null ? "SES-UPSTREAM" : "SES-" + target + "-" + kind;
    }

    public Target target() { return target; }
    public Kind kind() { return kind; }
    public Integer status() { return status; }
}
