# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-04-20

Initial public release of `didwebvh-java`, a Java 11+ implementation of the
[did:webvh v1.0](https://identity.foundation/didwebvh/v1.0/) DID method.

### Added
- **Core API** (`didwebvh-core`):
  - `DidWebVh.create(domain, signer)` — DID creation with SCID generation,
    authorization keys, `eddsa-jcs-2022` Data Integrity proofs, optional
    pre-rotation, witness configuration, `portable`, `ttl`, `watchers`,
    `alsoKnownAs`, and arbitrary `additionalDocumentContent`.
  - `DidWebVh.update(state, signer)` — standard DID update (document
    replacement, parameter rotation, key rotation).
  - `DidWebVh.migrate(state, signer, newDomain)` — portable-DID migration
    to a new domain while preserving the SCID and the full log chain.
  - `DidWebVh.deactivate(state, signer)` — DID deactivation per spec §3.6.4.
  - `DidWebVh.resolve(did)` — HTTPS resolution, JSONL log fetch, optional
    witness-proof fetch, full chain validation.
  - `DidWebVh.validate(entries, expectedDid)` — offline log-chain validation.
  - `LogEntry` / `DidWebVhState` / `ResolveResult` model types with Gson
    serialization and JCS canonicalization.
  - `DidUrlParser` and DID-to-HTTPS transformation (spec §3.4), plus
    `toDidWebUrl()` for parallel `did:web` lookup.
  - `DidWebPublisher` — parallel `did:web` document emission.
- **Signing SPI** (`didwebvh-core`) — pluggable `Signer` interface;
  built-in `Ed25519Suite` / JCS hashing utilities.
- **Local-key adapter** (`didwebvh-signing-local`) — `LocalKeySigner` that
  loads Ed25519 keys from JSON key files.
- **Interactive wizard** (`didwebvh-wizard`) — picocli + JLine CLI for
  guided create, update (modify / migrate / deactivate), and resolve
  flows; shipped as a shaded uber-jar on the GitHub Release.
- **Test vectors** under `didwebvh-core/src/test/resources/test-vectors/`
  (first-log-entry-good / tampered, multi-entry + witness, deactivated,
  migrated, pre-rotation) and `SpecComplianceIT` covering 18 spec MUSTs.
- **CI** — GitHub Actions matrix across JDK 11, 17, 21, and 25, with
  Checkstyle, SpotBugs, JaCoCo, and Codecov upload.
- **Release pipeline** — `release` Maven profile and
  `.github/workflows/release.yml` that publishes to Sonatype Central on
  tag push (`v*`) and attaches JARs to the GitHub Release.

[Unreleased]: https://github.com/IVIR3zaM/didwebvh-java/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/IVIR3zaM/didwebvh-java/releases/tag/v0.1.0
