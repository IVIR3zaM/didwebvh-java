package io.github.ivir3zam.didwebvh.core.signing;

import com.google.gson.JsonObject;
import io.github.ivir3zam.didwebvh.core.crypto.Base58Btc;
import io.github.ivir3zam.didwebvh.core.crypto.Jcs;
import io.github.ivir3zam.didwebvh.core.model.DataIntegrityProof;
import io.github.ivir3zam.didwebvh.core.model.JsonSupport;
import io.github.ivir3zam.didwebvh.core.model.LogEntry;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** Generates Data Integrity proofs for log entries using eddsa-jcs-2022. */
public final class ProofGenerator {

    private static final DateTimeFormatter ISO_UTC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    .withZone(ZoneOffset.UTC);

    private ProofGenerator() {
    }

    /**
     * Generate a Data Integrity proof over a log entry.
     *
     * <p>The proof field is stripped internally before canonicalization.
     *
     * @param signer   the signer to use
     * @param logEntry the log entry to sign
     * @return a populated {@link DataIntegrityProof}
     */
    public static DataIntegrityProof generate(Signer signer, LogEntry logEntry) {
        JsonObject json = toJsonWithoutProof(logEntry);
        byte[] canonical = Jcs.canonicalize(json);
        byte[] signature = signer.sign(canonical);
        String proofValue = Base58Btc.encodeMultibase(signature);

        return DataIntegrityProof.defaults()
                .setVerificationMethod(signer.verificationMethod())
                .setCreated(ISO_UTC.format(Instant.now()))
                .setProofValue(proofValue);
    }

    /** Serialize a LogEntry to JsonObject with the {@code proof} key removed. */
    static JsonObject toJsonWithoutProof(LogEntry logEntry) {
        JsonObject json = JsonSupport.compact().toJsonTree(logEntry)
                .getAsJsonObject();
        json.remove("proof");
        return json;
    }
}
