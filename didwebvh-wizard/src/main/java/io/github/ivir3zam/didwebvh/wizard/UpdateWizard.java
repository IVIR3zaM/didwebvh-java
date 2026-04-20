package io.github.ivir3zam.didwebvh.wizard;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.github.ivir3zam.didwebvh.core.DidWebVh;
import io.github.ivir3zam.didwebvh.core.DidWebVhState;
import io.github.ivir3zam.didwebvh.core.model.LogEntry;
import io.github.ivir3zam.didwebvh.core.model.Parameters;
import io.github.ivir3zam.didwebvh.core.update.UpdateDidResult;
import io.github.ivir3zam.didwebvh.core.validate.ValidationResult;
import io.github.ivir3zam.didwebvh.signing.local.LocalKeySigner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Interactive "update / migrate / deactivate" wizard. */
public final class UpdateWizard {

    private final WizardIo io;
    private final WizardPrompts ask;

    public UpdateWizard(WizardIo io) {
        this.io = io;
        this.ask = new WizardPrompts(io);
    }

    /** Run the update flow against files in {@code workDir}. */
    public void run(Path workDir) {
        io.println("=== Update an existing did:webvh DID ===");

        Path logPath = workDir.resolve(WizardFiles.DID_LOG);
        Path secretsPath = workDir.resolve(WizardFiles.DID_SECRETS);
        if (!Files.exists(logPath)) {
            throw new WizardException("No " + WizardFiles.DID_LOG + " found in "
                    + workDir.toAbsolutePath());
        }
        if (!Files.exists(secretsPath)) {
            throw new WizardException("No " + WizardFiles.DID_SECRETS + " found in "
                    + workDir.toAbsolutePath());
        }

        LocalKeySigner signer = LocalKeySigner.fromJson(WizardFiles.read(secretsPath));
        String rawLog = WizardFiles.read(logPath);
        List<LogEntry> entries = parseLog(rawLog);
        if (entries.isEmpty()) {
            throw new WizardException("DID log is empty");
        }
        String did = entries.get(0).getState().get("id").getAsString();
        DidWebVhState state = DidWebVhState.fromDidLog(did, rawLog);
        ValidationResult existing = state.validate();
        if (!existing.isValid()) {
            throw new WizardException("Existing log failed validation: "
                    + existing.getFailureReason());
        }
        if (state.isDeactivated()) {
            throw new WizardException("DID is already deactivated; no further updates allowed.");
        }

        io.println("Loaded " + entries.size() + " existing log entries for " + did);
        io.println("");
        io.println("Operation:");
        io.println("  1) Modify the DID document or parameters");
        io.println("  2) Migrate to a new domain");
        io.println("  3) Deactivate the DID");
        int choice = ask.askChoice("Choose 1-3: ", 3);

        UpdateDidResult result;
        switch (choice) {
            case 1:
                result = runModify(state, signer);
                break;
            case 2:
                result = runMigrate(state, signer);
                break;
            case 3:
                result = runDeactivate(state, signer, workDir);
                break;
            default:
                throw new WizardException("Unexpected choice: " + choice);
        }

        for (LogEntry entry : result.getNewEntries()) {
            WizardFiles.appendLine(logPath, entry.toJsonLine());
        }
        io.println("");
        io.println("Wrote " + result.getNewEntries().size() + " new entr"
                + (result.getNewEntries().size() == 1 ? "y" : "ies")
                + " to " + WizardFiles.DID_LOG);
        io.println("New DID state id: " + result.getLogEntry().getState().get("id").getAsString());
    }

    private UpdateDidResult runModify(DidWebVhState state, LocalKeySigner signer) {
        JsonObject newDocument = null;
        if (ask.askYesNo("Replace the DID Document?", false)) {
            String json = ask.askRequired("New DID Document JSON (single line): ");
            try {
                newDocument = JsonParser.parseString(json).getAsJsonObject();
            } catch (JsonSyntaxException | IllegalStateException e) {
                throw new WizardException("Invalid DID Document JSON: " + e.getMessage(), e);
            }
        }

        Parameters changed = null;
        if (ask.askYesNo("Change any parameters (ttl, watchers)?", false)) {
            changed = new Parameters();
            String ttlLine = ask.askOptional("New TTL (blank to keep current): ", null);
            if (ttlLine != null) {
                try {
                    changed.setTtl(Integer.parseInt(ttlLine));
                } catch (NumberFormatException e) {
                    throw new WizardException("Invalid TTL: " + ttlLine, e);
                }
            }
            String watchersLine = ask.askOptional(
                    "New watchers (comma-separated, 'clear' to empty, blank to keep): ", null);
            if (watchersLine != null) {
                if (watchersLine.equalsIgnoreCase("clear")) {
                    changed.setWatchers(java.util.Collections.emptyList());
                } else {
                    java.util.List<String> list = new java.util.ArrayList<>();
                    for (String part : watchersLine.split(",")) {
                        String trimmed = part.trim();
                        if (!trimmed.isEmpty()) {
                            list.add(trimmed);
                        }
                    }
                    changed.setWatchers(list);
                }
            }
        }

        return DidWebVh.update(state, signer)
                .newDocument(newDocument)
                .changedParameters(changed)
                .execute();
    }

    private UpdateDidResult runMigrate(DidWebVhState state, LocalKeySigner signer) {
        if (!Boolean.TRUE.equals(state.accumulateParameters().getPortable())) {
            throw new WizardException(
                    "DID is not portable; cannot migrate. Migration requires portable=true.");
        }
        String newDomain = ask.askRequired("New domain: ");
        String newPath = ask.askOptional("New path (blank for none): ", null);
        return DidWebVh.migrate(state, signer, newDomain)
                .newPath(newPath)
                .execute();
    }

    private UpdateDidResult runDeactivate(DidWebVhState state, LocalKeySigner signer,
                                          Path workDir) {
        io.println("");
        io.println("WARNING: Deactivation is PERMANENT.  The DID cannot be re-activated.");
        String confirm = ask.askRequired("Type 'DEACTIVATE' to confirm: ");
        if (!"DEACTIVATE".equals(confirm)) {
            throw new WizardException("Deactivation not confirmed; aborting.");
        }
        LocalKeySigner nextSigner = null;
        List<String> nextHashes = state.accumulateParameters().getNextKeyHashes();
        if (nextHashes != null && !nextHashes.isEmpty()) {
            Path nextKeyPath = workDir.resolve(WizardFiles.NEXT_KEY_SECRETS);
            if (!Files.exists(nextKeyPath)) {
                throw new WizardException("Pre-rotation is active but "
                        + WizardFiles.NEXT_KEY_SECRETS + " was not found in "
                        + workDir.toAbsolutePath());
            }
            nextSigner = LocalKeySigner.fromJson(WizardFiles.read(nextKeyPath));
        }
        return DidWebVh.deactivate(state, signer)
                .nextRotationSigner(nextSigner)
                .execute();
    }

    private List<LogEntry> parseLog(String rawLog) {
        java.util.List<LogEntry> list = new java.util.ArrayList<>();
        for (String line : rawLog.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                list.add(LogEntry.fromJsonLine(trimmed));
            }
        }
        return list;
    }
}
