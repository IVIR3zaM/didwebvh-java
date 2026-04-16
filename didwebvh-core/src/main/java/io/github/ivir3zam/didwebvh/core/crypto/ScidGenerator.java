package io.github.ivir3zam.didwebvh.core.crypto;

import io.github.ivir3zam.didwebvh.core.model.LogEntry;

public final class ScidGenerator {

    static final String SCID_PLACEHOLDER = "{SCID}";

    private ScidGenerator() {
    }

    public static String generate(LogEntry preliminaryEntry) {
        return deriveScid(preliminaryEntry.toJsonLine());
    }

    public static boolean verify(String scid, LogEntry firstEntry) {
        LogEntry withoutProof = new LogEntry()
                .setVersionId(firstEntry.getVersionId())
                .setVersionTime(firstEntry.getVersionTime())
                .setParameters(firstEntry.getParameters())
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
