package io.github.ivir3zam.didwebvh.core.crypto;

import com.google.gson.JsonObject;
import io.github.ivir3zam.didwebvh.core.ValidationException;
import org.erdtman.jcs.JsonCanonicalizer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class Jcs {

    private Jcs() {
    }

    public static byte[] canonicalize(String json) {
        try {
            JsonCanonicalizer canonicalizer = new JsonCanonicalizer(json);
            return canonicalizer.getEncodedString().getBytes(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ValidationException("JCS canonicalization failed: " + e.getMessage(), e);
        }
    }

    public static byte[] canonicalize(JsonObject json) {
        return canonicalize(json.toString());
    }
}
