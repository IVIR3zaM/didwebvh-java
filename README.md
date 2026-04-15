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
import io.github.ivir3zam.didwebvh.signing.LocalKeySigner;

// Generate a signing key
LocalKeySigner signer = LocalKeySigner.generate();

// Create a new DID
CreateDidResult result = DidWebVh.create("example.com")
    .withSigner(signer)
    .execute();

System.out.println("DID: " + result.getDid());
System.out.println("Log: " + result.getLogEntry().toJsonLine());
```

### Resolve a DID

```java
import io.github.ivir3zam.didwebvh.core.DidWebVh;

ResolveResult result = DidWebVh.resolve("did:webvh:QmSCID:example.com");

System.out.println("DID Document: " + result.getDidDocument());
System.out.println("Metadata: " + result.getMetadata());
```

### Update a DID

```java
import io.github.ivir3zam.didwebvh.core.DidWebVh;

UpdateResult result = DidWebVh.update(existingState)
    .withDocument(updatedDocument)
    .withSigner(signer)
    .execute();
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

Run the interactive wizard to create and manage DIDs:

```bash
java -jar didwebvh-wizard.jar
```

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

## Running Tests

```bash
./mvnw test
```

## License

[Apache License 2.0](LICENSE)

## Specification

This library implements the [did:webvh DID Method v1.0](https://identity.foundation/didwebvh/v1.0/) specification.

## Contributing

See [AGENTS.md](AGENTS.md) for contributor and AI agent guidelines, and [ARCHITECTURE.md](ARCHITECTURE.md) for technical design decisions.
