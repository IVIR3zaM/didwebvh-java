package io.github.ivir3zam.didwebvh.core.crypto;

import java.nio.charset.StandardCharsets;

public final class PreRotationHashGenerator {

    private PreRotationHashGenerator() {
    }

    public static String generateHash(String multikeyPublicKey) {
        byte[] keyBytes = multikeyPublicKey.getBytes(StandardCharsets.UTF_8);
        byte[] multihash = MultihashUtil.hashAndEncode(keyBytes);
        return Base58Btc.encodeMultibase(multihash);
    }
}
