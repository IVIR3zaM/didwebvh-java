package io.github.ivir3zam.didwebvh.core.resolve;

/** Options that select a specific DID log version during resolution. */
public final class ResolveOptions {

    private final String versionId;
    private final String versionTime;
    private final Integer versionNumber;
    private final WitnessFetchMode witnessFetchMode;

    private ResolveOptions(Builder builder) {
        this.versionId = builder.versionId;
        this.versionTime = builder.versionTime;
        this.versionNumber = builder.versionNumber;
        this.witnessFetchMode = builder.witnessFetchMode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ResolveOptions defaults() {
        return builder().build();
    }

    public String getVersionId() {
        return versionId;
    }

    public String getVersionTime() {
        return versionTime;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public WitnessFetchMode getWitnessFetchMode() {
        return witnessFetchMode;
    }

    ResolveOptions withFallbacks(ResolveOptions fallback) {
        if (fallback == null) {
            return this;
        }
        return builder()
                .versionId(versionId != null ? versionId : fallback.versionId)
                .versionTime(versionTime != null ? versionTime : fallback.versionTime)
                .versionNumber(versionNumber != null ? versionNumber : fallback.versionNumber)
                .witnessFetchMode(witnessFetchMode)
                .build();
    }

    boolean hasMultipleVersionSelectors() {
        int count = 0;
        if (versionId != null) {
            count++;
        }
        if (versionTime != null) {
            count++;
        }
        if (versionNumber != null) {
            count++;
        }
        return count > 1;
    }

    /** Controls when HTTP resolution retrieves {@code did-witness.json}. */
    public enum WitnessFetchMode {
        /** Fetch witness proofs opportunistically and ignore 404 unless the log requires them. */
        PROACTIVE,
        /** Fetch witness proofs only after the log is parsed and witness parameters are active. */
        WHEN_REQUIRED
    }

    /** Builder for {@link ResolveOptions}. */
    public static final class Builder {
        private String versionId;
        private String versionTime;
        private Integer versionNumber;
        private WitnessFetchMode witnessFetchMode = WitnessFetchMode.PROACTIVE;

        private Builder() {
        }

        public Builder versionId(String versionId) {
            this.versionId = versionId;
            return this;
        }

        public Builder versionTime(String versionTime) {
            this.versionTime = versionTime;
            return this;
        }

        public Builder versionNumber(Integer versionNumber) {
            this.versionNumber = versionNumber;
            return this;
        }

        public Builder witnessFetchMode(WitnessFetchMode witnessFetchMode) {
            this.witnessFetchMode = witnessFetchMode == null
                    ? WitnessFetchMode.PROACTIVE : witnessFetchMode;
            return this;
        }

        public ResolveOptions build() {
            return new ResolveOptions(this);
        }
    }
}
