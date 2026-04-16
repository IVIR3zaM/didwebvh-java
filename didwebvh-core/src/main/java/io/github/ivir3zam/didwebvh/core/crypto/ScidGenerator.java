package io.github.ivir3zam.didwebvh.core.crypto;

import io.github.ivir3zam.didwebvh.core.model.LogEntry;
import io.github.ivir3zam.didwebvh.core.model.Parameters;

public final class ScidGenerator {

    static final String SCID_PLACEHOLDER = "{SCID}";

    private ScidGenerator() {
    }

    public static String generate(LogEntry preliminaryEntry) {
        return deriveScid(preliminaryEntry.toJsonLine());
    }

    public static boolean verify(String scid, LogEntry firstEntry) {
        // Spec section 3.7.3 verification:
        // 1. Remove proof
        // 2. Replace versionId with "{SCID}"
        // 3. Replace scid in parameters with "{SCID}"
        // 4. String-replace all remaining SCID occurrences with "{SCID}"
        Parameters params = firstEntry.getParameters();
        Parameters resetParams = params.merge(
                new Parameters().setScid(SCID_PLACEHOLDER));
        LogEntry withoutProof = new LogEntry()
                .setVersionId(SCID_PLACEHOLDER)
                .setVersionTime(firstEntry.getVersionTime())
                .setParameters(resetParams)
                .setState(firstEntry.getState());
        String json = withoutProof.toJsonLine();
        String preliminary = json.replace(scid, SCID_PLACEHOLDER);
        return scid.equals(deriveScid(preliminary));
    }

    public static String placeholder() {
        return SCID_PLACEHOLDER;
    }

    private static String deriveScid(String json) {
        byte[] canonical = Jcs.canonicalize(json);
        byte[] multihash = MultihashUtil.hashAndEncode(canonical);
        return Base58Btc.encodeMultibase(multihash);
    }
}
