package io.github.ivir3zam.didwebvh.core.witness;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Witness configuration: a threshold and a list of witnesses. */
public final class WitnessConfig {

    private final int threshold;
    private final List<WitnessEntry> witnesses;

    public WitnessConfig(int threshold, List<WitnessEntry> witnesses) {
        this.threshold = threshold;
        this.witnesses = witnesses == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(witnesses);
    }

    public int getThreshold() {
        return threshold;
    }

    public List<WitnessEntry> getWitnesses() {
        return witnesses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WitnessConfig)) {
            return false;
        }
        WitnessConfig that = (WitnessConfig) o;
        return threshold == that.threshold && Objects.equals(witnesses, that.witnesses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(threshold, witnesses);
    }
}
