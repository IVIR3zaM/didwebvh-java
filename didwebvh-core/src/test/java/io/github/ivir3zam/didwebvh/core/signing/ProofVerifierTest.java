package io.github.ivir3zam.didwebvh.core.signing;

import io.github.ivir3zam.didwebvh.core.ValidationException;
import io.github.ivir3zam.didwebvh.core.model.DataIntegrityProof;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProofVerifierTest {

    @Test
    void isAuthorizedWithMatchingKey() {
        DataIntegrityProof proof = DataIntegrityProof.defaults()
                .setVerificationMethod("did:key:z6MkKey1#z6MkKey1");

        assertThat(ProofVerifier.isAuthorized(proof,
                Arrays.asList("z6MkKey1", "z6MkKey2"))).isTrue();
    }

    @Test
    void isAuthorizedRejectsNonMatchingKey() {
        DataIntegrityProof proof = DataIntegrityProof.defaults()
                .setVerificationMethod("did:key:z6MkKeyX#z6MkKeyX");

        assertThat(ProofVerifier.isAuthorized(proof,
                Arrays.asList("z6MkKey1", "z6MkKey2"))).isFalse();
    }

    @Test
    void isAuthorizedRejectsEmptyKeyList() {
        DataIntegrityProof proof = DataIntegrityProof.defaults()
                .setVerificationMethod("did:key:z6MkKey1#z6MkKey1");

        assertThat(ProofVerifier.isAuthorized(proof,
                Collections.emptyList())).isFalse();
    }

    @Test
    void extractMultikeyFromFragment() {
        assertThat(ProofVerifier.extractMultikey("did:key:z6Mk123#z6Mk123"))
                .isEqualTo("z6Mk123");
    }

    @Test
    void extractMultikeyFromDidKeyWithoutFragment() {
        assertThat(ProofVerifier.extractMultikey("did:key:z6Mk456"))
                .isEqualTo("z6Mk456");
    }

    @Test
    void extractMultikeyThrowsOnNull() {
        assertThatThrownBy(() -> ProofVerifier.extractMultikey(null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void extractMultikeyThrowsOnInvalidFormat() {
        assertThatThrownBy(() -> ProofVerifier.extractMultikey("invalid"))
                .isInstanceOf(ValidationException.class);
    }
}
