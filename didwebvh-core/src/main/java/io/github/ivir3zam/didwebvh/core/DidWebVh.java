package io.github.ivir3zam.didwebvh.core;

import io.github.ivir3zam.didwebvh.core.create.CreateDidConfig;
import io.github.ivir3zam.didwebvh.core.model.LogEntry;
import io.github.ivir3zam.didwebvh.core.signing.Signer;
import io.github.ivir3zam.didwebvh.core.validate.LogChainValidator;
import io.github.ivir3zam.didwebvh.core.validate.ValidationResult;

import java.util.List;

/**
 * Main entry point for the did:webvh library.
 *
 * <p>Usage:
 * <pre>{@code
 * CreateDidResult result = DidWebVh.create("example.com", signer)
 *         .portable(true)
 *         .ttl(3600)
 *         .execute();
 * }</pre>
 */
public final class DidWebVh {

    private DidWebVh() {
    }

    /**
     * Begin configuring a new DID creation for the given domain and signer.
     *
     * @param domain the web domain (e.g. {@code "example.com"})
     * @param signer the signing key to use
     * @return a builder for further configuration
     */
    public static CreateDidConfig create(String domain, Signer signer) {
        return new CreateDidConfig(domain, signer);
    }

    /**
     * Validate a log chain against an expected DID.
     *
     * @param entries     the ordered list of log entries
     * @param expectedDid the DID being resolved (may be {@code null} to skip id check)
     * @return the validation result
     */
    public static ValidationResult validate(List<LogEntry> entries, String expectedDid) {
        return new LogChainValidator().validate(entries, expectedDid);
    }
}
