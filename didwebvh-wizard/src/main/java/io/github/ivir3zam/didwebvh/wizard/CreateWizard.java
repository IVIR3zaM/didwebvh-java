package io.github.ivir3zam.didwebvh.wizard;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.github.ivir3zam.didwebvh.core.DidWebVh;
import io.github.ivir3zam.didwebvh.core.create.CreateDidConfig;
import io.github.ivir3zam.didwebvh.core.create.CreateDidResult;
import io.github.ivir3zam.didwebvh.core.crypto.PreRotationHashGenerator;
import io.github.ivir3zam.didwebvh.core.witness.WitnessConfig;
import io.github.ivir3zam.didwebvh.core.witness.WitnessEntry;
import io.github.ivir3zam.didwebvh.signing.local.LocalKeySigner;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Interactive "create a new DID" wizard. */
public final class CreateWizard {

    private final WizardIo io;
    private final WizardPrompts ask;

    public CreateWizard(WizardIo io) {
        this.io = io;
        this.ask = new WizardPrompts(io);
    }

    /**
     * Run the create flow, writing outputs to {@code workDir}.  Returns the created DID string.
     */
    public String run(Path workDir) {
        io.println("=== Create a new did:webvh DID ===");

        String domain = ask.askRequired("Web domain (e.g. example.com): ");
        String path = ask.askOptional("Path (optional, 'dids:issuer' style, blank to skip): ", null);

        LocalKeySigner signer = loadOrGenerateSigner(workDir);
        io.println("Authorization key multikey: " + signer.getPublicKeyMultikey());

        CreateDidConfig config = DidWebVh.create(domain, signer);
        if (path != null) {
            config.path(path);
        }

        List<String> alsoKnownAs = readList(
                "alsoKnownAs (comma-separated, blank for none): ");
        if (!alsoKnownAs.isEmpty()) {
            config.alsoKnownAs(alsoKnownAs);
        }

        JsonObject additional = buildAdditionalDocumentContent();
        if (additional.size() > 0) {
            config.additionalDocumentContent(additional);
        }

        if (ask.askYesNo("Make this DID portable?", false)) {
            config.portable(true);
        }

        LocalKeySigner nextKeySigner = null;
        if (ask.askYesNo("Enable pre-rotation (generate a next key now)?", false)) {
            nextKeySigner = LocalKeySigner.generate();
            WizardFiles.write(workDir.resolve(WizardFiles.NEXT_KEY_SECRETS),
                    nextKeySigner.toJson());
            io.println("Next-rotation key saved to "
                    + WizardFiles.NEXT_KEY_SECRETS
                    + " (multikey: " + nextKeySigner.getPublicKeyMultikey() + ")");
            String hash = PreRotationHashGenerator.generateHash(
                    nextKeySigner.getPublicKeyMultikey());
            config.nextKeyHashes(Collections.singletonList(hash));
        }

        WitnessConfig witness = readWitnessConfig();
        if (witness != null) {
            config.witness(witness);
        }

        List<String> watchers = readList("Watchers (comma-separated URLs, blank for none): ");
        if (!watchers.isEmpty()) {
            config.watchers(watchers);
        }

        int ttl = ask.askInt("TTL seconds [default 3600, 0 = no cache]: ", 3600);
        if (ttl != 3600) {
            config.ttl(ttl);
        }

        CreateDidResult result = config.execute();
        WizardFiles.write(workDir.resolve(WizardFiles.DID_LOG), result.getLogLine());

        io.println("");
        io.println("DID created: " + result.getDid());
        io.println("Files written to " + workDir.toAbsolutePath() + ":");
        io.println("  - " + WizardFiles.DID_LOG);
        io.println("  - " + WizardFiles.DID_SECRETS + "   (KEEP SECURE)");
        if (nextKeySigner != null) {
            io.println("  - " + WizardFiles.NEXT_KEY_SECRETS + "  (KEEP SECURE)");
        }
        return result.getDid();
    }

    private LocalKeySigner loadOrGenerateSigner(Path workDir) {
        io.println("");
        io.println("Authorization key:");
        io.println("  1) Generate a new Ed25519 key");
        io.println("  2) Import from an existing " + WizardFiles.DID_SECRETS + " JSON file");
        int choice = ask.askChoice("Choose 1 or 2: ", 2);
        LocalKeySigner signer;
        if (choice == 1) {
            signer = LocalKeySigner.generate();
        } else {
            String pathStr = ask.askRequired("Path to existing key JSON: ");
            signer = LocalKeySigner.fromJson(WizardFiles.read(Path.of(pathStr)));
        }
        WizardFiles.write(workDir.resolve(WizardFiles.DID_SECRETS), signer.toJson());
        return signer;
    }

    private List<String> readList(String prompt) {
        String line = ask.askOptional(prompt, null);
        if (line == null) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        for (String part : line.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
    }

    private JsonObject buildAdditionalDocumentContent() {
        JsonObject extra = new JsonObject();

        String controllerLine = ask.askOptional(
                "Additional controller DIDs (comma-separated, blank to skip): ", null);
        if (controllerLine != null) {
            JsonArray arr = new JsonArray();
            for (String part : controllerLine.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    arr.add(trimmed);
                }
            }
            if (arr.size() > 0) {
                extra.add("controller", arr);
            }
        }

        String services = ask.askOptional(
                "Services JSON array (paste one line, blank to skip): ", null);
        if (services != null) {
            try {
                JsonElement parsed = JsonParser.parseString(services);
                if (!parsed.isJsonArray()) {
                    throw new WizardException("Services must be a JSON array");
                }
                extra.add("service", parsed.getAsJsonArray());
            } catch (JsonSyntaxException e) {
                throw new WizardException("Invalid services JSON: " + e.getMessage(), e);
            }
        }
        return extra;
    }

    private WitnessConfig readWitnessConfig() {
        if (!ask.askYesNo("Configure witnesses?", false)) {
            return null;
        }
        List<WitnessEntry> entries = new ArrayList<>();
        io.println("Enter witness DIDs one per line ('done' to finish).");
        while (true) {
            String line = ask.askOptional("Witness DID: ", null);
            if (line == null || line.equalsIgnoreCase("done")) {
                break;
            }
            entries.add(new WitnessEntry(line));
        }
        if (entries.isEmpty()) {
            io.println("No witnesses entered, skipping witness config.");
            return null;
        }
        int defaultThreshold = (entries.size() / 2) + 1;
        int threshold = ask.askInt(
                "Witness threshold [default " + defaultThreshold
                        + ", max " + entries.size() + "]: ",
                defaultThreshold);
        if (threshold < 1 || threshold > entries.size()) {
            throw new WizardException("Threshold must be between 1 and " + entries.size());
        }
        return new WitnessConfig(threshold, new ArrayList<>(Arrays.asList(
                entries.toArray(new WitnessEntry[0]))));
    }
}
