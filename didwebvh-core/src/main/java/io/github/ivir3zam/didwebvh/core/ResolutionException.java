package io.github.ivir3zam.didwebvh.core;

/** Thrown when DID resolution fails. */
public class ResolutionException extends DidWebVhException {

    private static final long serialVersionUID = 1L;

    public ResolutionException(String message) {
        super(message);
    }

    public ResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
