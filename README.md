# didwebvh-java

[![Java CI](https://github.com/IVIR3zaM/didwebvh-java/actions/workflows/ci.yml/badge.svg)](https://github.com/IVIR3zaM/didwebvh-java/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/IVIR3zaM/didwebvh-java/branch/main/graph/badge.svg)](https://codecov.io/gh/IVIR3zaM/didwebvh-java)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.ivir3zam/didwebvh-java.svg)](https://central.sonatype.com/artifact/io.github.ivir3zam/didwebvh-java)
[![Java Version](https://img.shields.io/badge/Java-11%2B-blue)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=IVIR3zaM_didwebvh-java&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=IVIR3zaM_didwebvh-java)

A Java 11+ library for the [did:webvh](https://didwebvh.info/) (DID Web + Verifiable History) DID Method, v1.0.

**Create**, **resolve**, **update**, and **deactivate** `did:webvh` DIDs with pluggable key management (local keys, AWS KMS, external APIs, and more).

## Features

- Full did:webvh v1.0 specification support
- **Create** DIDs with SCID generation, authorization keys, and Data Integrity proofs
- **Resolve** DIDs from HTTPS URLs or local files with full log chain verification
- **Update** DID documents, rotate keys, migrate to new domains, and deactivate
- **Witness** support with threshold-based approval
- **Pre-rotation** key commitment for forward security
- **DID portability** across domains while preserving verifiable history
- **Parallel did:web** document publishing for backward compatibility
- **Pluggable signing** via `Signer` interface (local Ed25519, AWS KMS, HSM, external API)
- **Interactive wizard** CLI for guided DID management
- **Java 11+** compatible, tested on Java 11, 17, 21, and 25

## Quick Start

### Maven

```xml
<dependency>
    <groupId>io.github.ivir3zam</groupId>
    <artifactId>didwebvh-java</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.ivir3zam:didwebvh-java:0.1.0'
```

### Create a DID

```java
import io.github.ivir3zam.didwebvh.core.DidWebVh;
import io.github.ivir3zam.didwebvh.core.create.CreateDidResult;
import io.github.ivir3zam.didwebvh.signing.local.LocalKeySigner;

// Generate a signing key (or load one from disk with LocalKeySigner.load(path))
LocalKeySigner signer = LocalKeySigner.generate();

CreateDidResult result = DidWebVh.create("example.com", signer)
        .portable(true)
        .ttl(3600)
        .execute();

System.out.println("DID: " + result.getDid());
System.out.println("Log line: " + result.getLogLine());
```

### Resolve a DID

```java
import io.github.ivir3zam.didwebvh.core.DidWebVh;
import io.github.ivir3zam.didwebvh.core.model.ResolveResult;

ResolveResult result = DidWebVh.resolve("did:webvh:QmSCID:example.com");

System.out.println("DID Document: " + result.getDidDocument());
System.out.println("Metadata:    : " + result.getResolutionMetadata());
```

### Update, Migrate, or Deactivate

```java
import io.github.ivir3zam.didwebvh.core.DidWebVh;
import io.github.ivir3zam.didwebvh.core.DidWebVhState;
import io.github.ivir3zam.didwebvh.core.update.UpdateDidResult;

// Reload state from the on-disk did.jsonl (or reuse a freshly created one):
DidWebVhState state = DidWebVhState.fromDidLog(did, didLogJsonl);

UpdateDidResult updated = DidWebVh.update(state, signer)
        .newDocument(updatedDoc)
        .execute();

// Migrate a portable DID to a new domain:
DidWebVh.migrate(state, signer, "new.example.com").execute();

// Deactivate:
DidWebVh.deactivate(state, signer).execute();
```

## Pluggable Key Management

The library uses a `Signer` interface that can be implemented for any key management system:

```java
public interface Signer {
    String keyType();
    String verificationMethod();
    byte[] sign(byte[] data) throws SigningException;
}
```

Built-in implementations:
- `LocalKeySigner` - Ed25519 keys from local JSON key files
- More adapters (AWS KMS, external API) can be added by implementing `Signer`

## Wizard CLI

Build the shaded (uber) jar, then run the interactive wizard:

```bash
./mvnw -pl didwebvh-wizard -am package
java -jar didwebvh-wizard/target/didwebvh-wizard-0.1.0-SNAPSHOT-shaded.jar
```

(`-am` builds the `didwebvh-core` and `didwebvh-signing-local` dependencies first; the
`-shaded` classifier is the self-contained jar produced by `maven-shade-plugin`.)

The wizard supports:
1. **Create** a new did:webvh DID
2. **Update** an existing DID (modify, migrate, deactivate)
3. **Resolve** a did:webvh DID

## Project Structure

```
didwebvh-java/
  didwebvh-core/           # Core library (model, create, resolve, update, validate)
  didwebvh-signing-local/  # Local key file signer adapter
  didwebvh-wizard/         # Interactive CLI wizard
```

## Building

```bash
./mvnw clean verify
```

Contributors should run the full build with JDK 21 for the closest local match to CI. The project supports
Java 11+, and CI also checks Java 11, 17, and 25, but SpotBugs is skipped on JDK 22+ in this repository.

## Running Tests

```bash
./mvnw test
```

## Specification

This library implements the [did:webvh DID Method v1.0](https://identity.foundation/didwebvh/v1.0/) specification.

## Changelog

See [CHANGELOG.md](CHANGELOG.md).

## Contributing

Contributions are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md)
for the development workflow, and
[docs/AGENTS.md](docs/AGENTS.md) /
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for contributor (and AI agent)
guidelines and the technical design.

Before opening a PR, run:

```bash
./mvnw clean verify
```

This runs the full test suite, Checkstyle, SpotBugs, and JaCoCo coverage
checks across all modules.

## Security

If you believe you've found a security vulnerability, please follow the
instructions in [SECURITY.md](SECURITY.md) — **do not** open a public
GitHub issue.

## License

Licensed under the [Apache License, Version 2.0](LICENSE). By contributing
to this project you agree to license your contributions under the same
terms.
