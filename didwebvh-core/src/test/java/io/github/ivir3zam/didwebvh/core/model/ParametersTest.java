package io.github.ivir3zam.didwebvh.core.model;

import com.google.gson.Gson;
import io.github.ivir3zam.didwebvh.core.witness.WitnessConfig;
import io.github.ivir3zam.didwebvh.core.witness.WitnessEntry;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class ParametersTest {

    private final Gson gson = JsonSupport.compact();

    @Test
    void emptyParametersRoundTripToEmptyObject() {
        Parameters p = new Parameters();
        String json = gson.toJson(p);
        assertThat(json).isEqualTo("{}");
        assertThat(gson.fromJson(json, Parameters.class)).isEqualTo(p);
    }

    @Test
    void fullParametersRoundTrip() {
        WitnessConfig wc = new WitnessConfig(2, Arrays.asList(
                new WitnessEntry("did:key:z6Mk1"),
                new WitnessEntry("did:key:z6Mk2")));

        Parameters p = new Parameters()
                .setMethod("did:webvh:1.0")
                .setScid("QmSCID")
                .setUpdateKeys(Arrays.asList("z6MkA", "z6MkB"))
                .setNextKeyHashes(Collections.singletonList("Qm1"))
                .setWitness(wc)
                .setWatchers(Collections.singletonList("https://watcher.example/"))
                .setPortable(Boolean.TRUE)
                .setDeactivated(Boolean.FALSE)
                .setTtl(3600);

        String json = gson.toJson(p);
        Parameters decoded = gson.fromJson(json, Parameters.class);
        assertThat(decoded).isEqualTo(p);
    }

    @Test
    void mergeAppliesNonNullFieldsFromOther() {
        Parameters base = new Parameters()
                .setMethod("did:webvh:1.0")
                .setScid("QmSCID")
                .setUpdateKeys(Collections.singletonList("z6MkA"))
                .setPortable(Boolean.TRUE);

        Parameters delta = new Parameters()
                .setUpdateKeys(Collections.singletonList("z6MkZ"))
                .setDeactivated(Boolean.TRUE);

        Parameters merged = base.merge(delta);

        assertThat(merged.getMethod()).isEqualTo("did:webvh:1.0");
        assertThat(merged.getScid()).isEqualTo("QmSCID");
        assertThat(merged.getUpdateKeys()).containsExactly("z6MkZ");
        assertThat(merged.getPortable()).isTrue();
        assertThat(merged.getDeactivated()).isTrue();
        assertThat(base.getDeactivated()).isNull();
        assertThat(base.getUpdateKeys()).containsExactly("z6MkA");
    }

    @Test
    void mergeWithNullReturnsCopyOfBase() {
        Parameters base = new Parameters().setMethod("did:webvh:1.0");
        Parameters merged = base.merge(null);
        assertThat(merged).isEqualTo(base);
        assertThat(merged).isNotSameAs(base);
    }
}
