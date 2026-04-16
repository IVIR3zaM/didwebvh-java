package io.github.ivir3zam.didwebvh.core;

import io.github.ivir3zam.didwebvh.core.create.CreateDidConfig;
import io.github.ivir3zam.didwebvh.core.signing.Signer;

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
}
