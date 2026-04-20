package io.github.ivir3zam.didwebvh.wizard;

import io.github.ivir3zam.didwebvh.core.DidWebVh;
import io.github.ivir3zam.didwebvh.core.DidWebVhState;
import io.github.ivir3zam.didwebvh.core.create.CreateDidResult;
import io.github.ivir3zam.didwebvh.core.validate.ValidationResult;
import io.github.ivir3zam.didwebvh.signing.local.LocalKeySigner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpdateWizardTest {

    private static CreateDidResult seedDid(Path tmp, boolean portable) {
        LocalKeySigner signer = LocalKeySigner.generate();
        CreateDidResult result = DidWebVh.create("example.com", signer)
                .portable(portable)
                .execute();
        WizardFiles.write(tmp.resolve(WizardFiles.DID_LOG), result.getLogLine());
        WizardFiles.write(tmp.resolve(WizardFiles.DID_SECRETS), signer.toJson());
        return result;
    }

    @Test
    void modifyAppendsNewEntryWithChangedTtl(@TempDir Path tmp) {
        seedDid(tmp, false);
        ScriptedWizardIo io = new ScriptedWizardIo(
                "1",                 // modify
                "n",                 // don't replace document
                "y",                 // change parameters
                "120",               // new TTL
                ""                   // watchers unchanged
        );

        new UpdateWizard(io).run(tmp);

        String log = WizardFiles.read(tmp.resolve(WizardFiles.DID_LOG));
        String[] lines = log.split("\n");
        assertThat(lines).hasSize(2);

        DidWebVhState reloaded = DidWebVhState.fromDidLog(
                extractDid(lines[0]), log);
        ValidationResult res = reloaded.validate();
        assertThat(res.isValid()).isTrue();
        assertThat(reloaded.getActiveParameters().getTtl()).isEqualTo(120);
    }

    @Test
    void migrateRequiresPortable(@TempDir Path tmp) {
        seedDid(tmp, false);
        ScriptedWizardIo io = new ScriptedWizardIo("2");
        assertThatThrownBy(() -> new UpdateWizard(io).run(tmp))
                .isInstanceOf(WizardException.class)
                .hasMessageContaining("portable");
    }

    @Test
    void migrateAppendsEntryWhenPortable(@TempDir Path tmp) {
        seedDid(tmp, true);
        ScriptedWizardIo io = new ScriptedWizardIo(
                "2",                        // migrate
                "new.example.com",          // new domain
                ""                          // no path
        );
        new UpdateWizard(io).run(tmp);
        String log = WizardFiles.read(tmp.resolve(WizardFiles.DID_LOG));
        assertThat(log.split("\n")).hasSize(2);
        assertThat(log).contains("new.example.com");
    }

    @Test
    void deactivateRequiresConfirmation(@TempDir Path tmp) {
        seedDid(tmp, false);
        ScriptedWizardIo io = new ScriptedWizardIo(
                "3",
                "nope"            // wrong confirmation
        );
        assertThatThrownBy(() -> new UpdateWizard(io).run(tmp))
                .isInstanceOf(WizardException.class)
                .hasMessageContaining("not confirmed");
    }

    @Test
    void deactivateAppendsFinalEntry(@TempDir Path tmp) {
        seedDid(tmp, false);
        ScriptedWizardIo io = new ScriptedWizardIo(
                "3",
                "DEACTIVATE"
        );
        new UpdateWizard(io).run(tmp);
        String log = WizardFiles.read(tmp.resolve(WizardFiles.DID_LOG));
        DidWebVhState state = DidWebVhState.fromDidLog(extractDid(log.split("\n")[0]), log);
        assertThat(state.validate().isValid()).isTrue();
        assertThat(state.isDeactivated()).isTrue();
    }

    @Test
    void errorsWhenNoLogInDirectory(@TempDir Path tmp) {
        ScriptedWizardIo io = new ScriptedWizardIo();
        assertThatThrownBy(() -> new UpdateWizard(io).run(tmp))
                .isInstanceOf(WizardException.class)
                .hasMessageContaining(WizardFiles.DID_LOG);
    }

    private static String extractDid(String line) {
        int idx = line.indexOf("\"id\":\"");
        int start = idx + 6;
        int end = line.indexOf("\"", start);
        return line.substring(start, end);
    }
}
