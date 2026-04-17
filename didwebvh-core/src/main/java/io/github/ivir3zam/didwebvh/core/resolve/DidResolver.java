package io.github.ivir3zam.didwebvh.core.resolve;

import io.github.ivir3zam.didwebvh.core.ResolutionException;
import io.github.ivir3zam.didwebvh.core.model.ResolveResult;
import io.github.ivir3zam.didwebvh.core.url.DidToHttpsTransformer;
import io.github.ivir3zam.didwebvh.core.url.DidWebVhUrl;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/** High-level did:webvh resolver for HTTP, file, and in-memory logs. */
public class DidResolver {

    private final RemoteDidFetcher httpFetcher;
    private final FileDidFetcher fileFetcher;
    private final LogProcessor logProcessor;

    public DidResolver() {
        this(new HttpDidFetcher(), new FileDidFetcher(), new LogProcessor());
    }

    public DidResolver(Duration timeout) {
        this(new HttpDidFetcher(timeout, HttpDidFetcher.DEFAULT_MAX_RESPONSE_SIZE),
                new FileDidFetcher(), new LogProcessor());
    }

    public DidResolver(Duration timeout, int maxResponseSize) {
        this(new HttpDidFetcher(timeout, maxResponseSize),
                new FileDidFetcher(), new LogProcessor());
    }

    public DidResolver(HttpDidFetcher httpFetcher, FileDidFetcher fileFetcher,
                       LogProcessor logProcessor) {
        this((RemoteDidFetcher) httpFetcher, fileFetcher, logProcessor);
    }

    DidResolver(RemoteDidFetcher httpFetcher, FileDidFetcher fileFetcher,
                LogProcessor logProcessor) {
        this.httpFetcher = httpFetcher;
        this.fileFetcher = fileFetcher;
        this.logProcessor = logProcessor;
    }

    public ResolveResult resolve(String did) {
        return resolve(did, ResolveOptions.defaults());
    }

    public ResolveResult resolve(String did, ResolveOptions options) {
        DidWebVhUrl parsed = DidWebVhUrl.parse(did);
        String baseDid = parsed.toBaseDid();
        ResolveOptions effectiveOptions = (options == null ? ResolveOptions.defaults() : options)
                .withFallbacks(optionsFromQuery(parsed.getQueryParams()));
        if (effectiveOptions.hasMultipleVersionSelectors()) {
            throw new ResolutionException(
                    "Only one of versionId, versionTime, and versionNumber may be used",
                    "invalidDid");
        }

        String didLog = httpFetcher.fetchDidLog(DidToHttpsTransformer.toHttpsUrl(baseDid));
        String witness = null;
        if (effectiveOptions.getWitnessFetchMode()
                == ResolveOptions.WitnessFetchMode.PROACTIVE) {
            witness = fetchWitnessIfPresent(baseDid);
        }
        return logProcessor.process(didLog, witness, baseDid, effectiveOptions,
                () -> fetchRequiredWitness(baseDid));
    }

    public ResolveResult resolveFromFile(Path didLogPath) {
        return logProcessor.process(fileFetcher.fetchDidLog(didLogPath), null, null,
                ResolveOptions.defaults());
    }

    public ResolveResult resolveFromLog(String rawJsonl, String did) {
        return resolveFromLog(rawJsonl, did, ResolveOptions.defaults());
    }

    public ResolveResult resolveFromLog(String rawJsonl, String did, ResolveOptions options) {
        return logProcessor.process(rawJsonl, null, did, options);
    }

    private String fetchWitnessIfPresent(String did) {
        try {
            return httpFetcher.fetchWitnessProofs(DidToHttpsTransformer.toWitnessUrl(did));
        } catch (ResolutionException e) {
            if ("notFound".equals(e.getError())) {
                return null;
            }
            throw e;
        }
    }

    private String fetchRequiredWitness(String did) {
        try {
            return httpFetcher.fetchWitnessProofs(DidToHttpsTransformer.toWitnessUrl(did));
        } catch (ResolutionException e) {
            if ("notFound".equals(e.getError())) {
                throw new ResolutionException("Witness proofs are required but were not found",
                        "invalidDid", e);
            }
            throw e;
        }
    }

    private ResolveOptions optionsFromQuery(Map<String, String> queryParams) {
        ResolveOptions.Builder builder = ResolveOptions.builder();
        if (queryParams.containsKey("versionId")) {
            builder.versionId(queryParams.get("versionId"));
        }
        if (queryParams.containsKey("versionTime")) {
            builder.versionTime(queryParams.get("versionTime"));
        }
        if (queryParams.containsKey("versionNumber")) {
            try {
                builder.versionNumber(Integer.valueOf(queryParams.get("versionNumber")));
            } catch (NumberFormatException e) {
                throw new ResolutionException("Invalid versionNumber: "
                        + queryParams.get("versionNumber"), "invalidDid", e);
            }
        }
        return builder.build();
    }
}
