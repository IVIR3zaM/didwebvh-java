# Implementation Iterations

This file contains the ordered, detailed prompts for each step of building `didwebvh-java` from scratch to production-ready. Each iteration builds on the previous ones. An AI agent or human can execute them sequentially.

Read `AGENTS.md` and `ARCHITECTURE.md` before starting. The spec PDF (`Webvh v1.0.pdf`) in the repo root is the authoritative reference.

### Status Key

| Status | Meaning |
|--------|---------|
| `[NOT STARTED]` | Work has not begun |
| `[IN PROGRESS]` | Currently being worked on |
| `[DONE]` | Completed and verified |

---

## Iteration 1: Project Scaffolding `[DONE]`

### Goal
Set up the multi-module Maven project structure, CI pipeline, and quality tooling. After this iteration, `./mvnw clean verify` passes with zero code (empty modules).

### Tasks

1. **Create the Maven Wrapper** (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/`). Use Maven 3.9.x.

2. **Create the parent POM** (`pom.xml`) with:
   - `groupId`: `io.github.ivir3zam`
   - `artifactId`: `didwebvh-java`
   - `version`: `0.1.0-SNAPSHOT`
   - `packaging`: `pom`
   - `<modules>`: `didwebvh-core`, `didwebvh-signing-local`, `didwebvh-wizard`
   - Java 11 source/target via `maven-compiler-plugin`
   - `<dependencyManagement>` for all shared dependency versions:
     - `com.google.code.gson:gson:2.10.1`
     - `io.github.erdtman:java-json-canonicalization:1.1`
     - `com.github.multiformats:java-multihash:1.3.3`
     - `io.github.novacrypto:Base58:2022.01.17`
     - `org.bouncycastle:bcprov-jdk15on:1.70`
     - `org.bouncycastle:bcpkix-jdk15on:1.70`
     - `com.squareup.okhttp3:okhttp:4.12.0`
     - `info.picocli:picocli:4.7.5`
     - `org.jline:jline:3.25.1`
     - `org.junit.jupiter:junit-jupiter:5.10.2`
     - `org.assertj:assertj-core:3.25.3`
     - `org.mockito:mockito-core:5.10.0`
     - `com.squareup.okhttp3:mockwebserver:4.12.0`
   - Plugin management:
     - `maven-compiler-plugin` (Java 11)
     - `maven-surefire-plugin` (3.2.x, for JUnit 5)
     - `maven-failsafe-plugin` (3.2.x)
     - `jacoco-maven-plugin` (0.8.11) with report goal bound to `verify` phase
     - `maven-checkstyle-plugin` (3.3.x) with Google Java Style checks
     - `spotbugs-maven-plugin` (4.8.x)
     - `maven-source-plugin` and `maven-javadoc-plugin` for release

3. **Create child module POMs**:
   - `didwebvh-core/pom.xml`: depends on gson, java-json-canonicalization, java-multihash, Base58, bouncycastle, okhttp. Test deps: junit-jupiter, assertj, mockito, mockwebserver.
   - `didwebvh-signing-local/pom.xml`: depends on `didwebvh-core`, gson, bouncycastle. Test deps: junit-jupiter, assertj.
   - `didwebvh-wizard/pom.xml`: depends on `didwebvh-core`, `didwebvh-signing-local`, picocli, jline. Test deps: junit-jupiter, assertj.

4. **Create empty source directories** for each module:
   - `src/main/java/io/github/ivir3zam/didwebvh/...`
   - `src/test/java/io/github/ivir3zam/didwebvh/...`
   - `src/test/resources/`
   - Add a placeholder class in each module so the compiler has something to compile (e.g., `package-info.java`).

5. **Create `.gitignore`** for Java/Maven (target/, *.class, .idea/, *.iml, .DS_Store, etc.)

6. **Create `LICENSE`** file (Apache 2.0).

7. **Create GitHub Actions CI** (`.github/workflows/ci.yml`):
   - Trigger on push to `main` and all PRs
   - Matrix: Java 11, 17, 21, 25 on `ubuntu-latest`
   - Steps: checkout, setup-java (temurin), cache maven, `./mvnw clean verify -B`
   - Upload JaCoCo coverage to Codecov (using `codecov/codecov-action@v4`)
   - Run SonarCloud analysis (using `sonarsource/sonarcloud-github-action` or maven sonar plugin)

8. **Create `checkstyle.xml`** at project root based on Google Java Style with minor relaxations (line length 120).

9. **Create `sonar-project.properties`** for SonarCloud integration.

### Acceptance Criteria
- `./mvnw clean verify` passes with zero errors
- CI workflow file exists and is valid YAML
- All three modules are recognized by Maven
- JaCoCo, Checkstyle, SpotBugs plugins are configured (they'll do nothing with no code yet)
- `.gitignore` and `LICENSE` exist

### Implementation Notes
- Maven wrapper pre-existed (3.9.9); reused as-is
- JitPack repository added for `com.github.multiformats:java-multihash` (not on Maven Central)
- SpotBugs 4.9.3 used; auto-skipped on JDK >= 22 via profile (ASM doesn't support class file major version 69)
- `./mvnw clean verify` passes on local JDK 25 (SpotBugs skipped); CI matrix targets JDK 11/17/21/25

---

## Iteration 2: Model Classes `[NOT STARTED]`

### Goal
Implement all data model classes that represent the did:webvh spec's data structures. These are pure data holders with JSON serialization. No business logic yet.

### Tasks

1. **`DidWebVhException`** and its subclasses in `core` package:
   - `DidWebVhException extends RuntimeException` (message, cause constructors)
   - `ValidationException extends DidWebVhException`
   - `ResolutionException extends DidWebVhException`
   - `SigningException extends DidWebVhException`
   - `UrlParseException extends DidWebVhException`

2. **`VersionId`** in `model` package:
   - Fields: `int versionNumber`, `String entryHash`
   - Parse from string: `"1-QmHash..."` -> `VersionId(1, "QmHash...")`
   - `toString()` returns `"1-QmHash..."`
   - For first entry preliminary: use `"{SCID}"` as the full string
   - Validation: version number >= 1, entry hash is non-empty

3. **`Parameters`** in `model` package:
   - Fields matching spec section 3.7.1:
     - `String method` (e.g., "did:webvh:1.0")
     - `String scid`
     - `List<String> updateKeys` (multikey format)
     - `List<String> nextKeyHashes` (nullable/empty list)
     - `WitnessConfig witness` (nullable)
     - `List<String> watchers` (nullable/empty list)
     - `Boolean portable`
     - `Boolean deactivated`
     - `Integer ttl`
   - JSON serialization/deserialization with Gson
   - `merge(Parameters other)` method: non-null fields in `other` override `this`, returns new `Parameters`
   - Handle the spec rule about `null` values: gracefully accept null and convert to default

4. **`WitnessConfig`** in `witness` package:
   - Fields: `int threshold`, `List<WitnessEntry> witnesses`
   - `WitnessEntry`: just `String id` (a did:key DID)
   - JSON shape: `{"threshold": n, "witnesses": [{"id": "did:key:..."}]}`

5. **`DataIntegrityProof`** in `model` package:
   - Fields: `String type`, `String cryptosuite`, `String verificationMethod`, `String proofPurpose`, `String created`, `String proofValue`
   - Defaults for did:webvh:1.0: type="DataIntegrityProof", cryptosuite="eddsa-jcs-2022", proofPurpose="assertionMethod"

6. **`LogEntry`** in `model` package:
   - Fields: `String versionId`, `String versionTime`, `Parameters parameters`, `JsonObject state`, `List<DataIntegrityProof> proof`
   - `toJsonLine()`: serialize to compact JSON (no whitespace), as required by JSONL spec
   - `fromJsonLine(String line)`: parse a single JSONL line
   - `getVersionNumber()`: parse version number from versionId
   - `getEntryHash()`: parse entry hash from versionId
   - Note: `parameters` may be empty `{}` for entries with no parameter changes
   - Note: `state` is the full DID Document as a generic JSON object

7. **`DidDocument`** in `model` package:
   - Thin wrapper around `JsonObject`
   - Convenience: `getId()`, `getController()`, `getAlsoKnownAs()`, `getVerificationMethod()`, `getService()`
   - `withId(String id)` returns new instance with updated id
   - This class does NOT validate DID Document structure -- it's just a container

8. **`WitnessProofEntry`** and **`WitnessProofCollection`** in `witness` package:
   - `WitnessProofEntry`: `String versionId`, `List<DataIntegrityProof> proof`
   - `WitnessProofCollection`: `List<WitnessProofEntry> entries`
   - JSON shape matches `did-witness.json` format from spec

9. **`ResolutionMetadata`** in `model` package:
   - Fields: `String versionId`, `String versionTime`, `String created`, `String updated`, `String scid`, `Boolean portable`, `Boolean deactivated`, `String ttl`, `WitnessConfig witness`, `List<String> watchers`

10. **`ResolveResult`** in `model` package:
    - Fields: `DidDocument didDocument`, `ResolutionMetadata metadata`, `String error`, `JsonObject problemDetails`

### Tests

- For each model class:
  - Construction and getter tests
  - JSON round-trip: serialize to JSON, deserialize back, assert equality
  - `VersionId` parsing from various valid/invalid strings
  - `Parameters.merge()` with various combinations of null/non-null fields
  - `LogEntry.toJsonLine()` and `fromJsonLine()` round-trip
  - Edge cases: empty parameters `{}`, missing optional fields

### Acceptance Criteria
- All model classes exist with JSON serialization support
- All model tests pass
- `./mvnw clean verify` passes
- Checkstyle passes (Google style)

---

## Iteration 3: Crypto Primitives `[NOT STARTED]`

### Goal
Implement the cryptographic building blocks: JCS canonicalization, multihash, base58btc, SCID generation, entry hash generation, and multikey utilities.

### Tasks

1. **`Jcs`** in `crypto` package:
   - `static byte[] canonicalize(String json)` - takes JSON string, returns JCS-canonicalized bytes
   - `static byte[] canonicalize(JsonObject json)` - takes Gson JsonObject
   - Uses `java-json-canonicalization` library
   - Test with known inputs/outputs from RFC 8785 examples

2. **`MultihashUtil`** in `crypto` package:
   - `static byte[] encode(HashAlgorithm algorithm, byte[] data)` - hash data with algorithm, return multihash-encoded result
   - `static HashAlgorithm extractAlgorithm(byte[] multihash)` - extract algorithm from multihash prefix
   - `static byte[] extractDigest(byte[] multihash)` - extract raw digest bytes
   - `HashAlgorithm` enum: `SHA2_256` (only one needed for v1.0)

3. **`Base58Btc`** in `crypto` package:
   - `static String encode(byte[] data)`
   - `static byte[] decode(String encoded)`
   - Wraps the Base58 library

4. **`ScidGenerator`** in `crypto` package:
   - `static String generate(String preliminaryEntryJson)` - implements SCID generation from spec section 3.7.3:
     1. JCS-canonicalize the preliminary entry
     2. SHA-256 hash
     3. Multihash-encode
     4. Base58btc-encode
   - `static boolean verify(String scid, String firstEntryJson)` - implements SCID verification from spec section 3.7.3:
     1. Remove proof from entry
     2. Replace versionId with `"{SCID}"`
     3. Replace scid value in parameters with `"{SCID}"`
     4. Treat as string, replace all occurrences of actual SCID with `{SCID}`
     5. Generate SCID from modified entry
     6. Compare with provided scid

5. **`EntryHashGenerator`** in `crypto` package:
   - `static String generate(String entryJson, String predecessorVersionId)` - implements entry hash generation from spec section 3.7.4:
     1. Remove "proof" from entry JSON
     2. Set "versionId" to predecessorVersionId
     3. JCS-canonicalize
     4. SHA-256 hash
     5. Multihash-encode
     6. Base58btc-encode
   - `static boolean verify(LogEntry entry, String predecessorVersionId)` - verify an entry's hash

6. **`MultikeyUtil`** in `crypto` package:
   - `static String encode(String keyType, byte[] publicKeyBytes)` - encode public key to multikey string (e.g., `z6Mk...` for Ed25519)
   - `static byte[] decode(String multikey)` - extract raw public key bytes
   - `static String keyTypeFromMultikey(String multikey)` - determine key type from multicodec prefix
   - Ed25519 multicodec prefix: `0xed01`

7. **`PreRotationHashGenerator`** in `crypto` package:
   - `static String generateHash(String multikeyPublicKey)` - implements pre-rotation key hash from spec section 3.7.7:
     1. Take multikey string bytes
     2. SHA-256 hash
     3. Multihash-encode
     4. Base58btc-encode

### Tests

- **JCS**: Test with multiple JSON inputs (nested objects, arrays, unicode, numbers) and verify output matches expected canonical form
- **MultihashUtil**: Test SHA-256 encoding/decoding, algorithm extraction
- **Base58Btc**: Round-trip tests, known vector tests
- **ScidGenerator**: Create a known preliminary entry, verify SCID output matches expected value. Test verification with valid and tampered entries.
- **EntryHashGenerator**: Create known entries, verify hash output. Test verification positive and negative.
- **MultikeyUtil**: Encode/decode Ed25519 keys, verify multicodec prefix handling
- **PreRotationHashGenerator**: Generate hash for known key, verify output

### Acceptance Criteria
- All crypto classes implemented and tested
- SCID generation matches the spec's algorithm exactly
- Entry hash generation matches the spec's algorithm exactly
- `./mvnw clean verify` passes
- No crypto operations depend on the `Signer` interface (that's in the signing package)

---

## Iteration 4: Signing Interface and Proof Generation `[NOT STARTED]`

### Goal
Implement the `Signer` interface, `ProofGenerator`, `ProofVerifier`, and the `LocalKeySigner` adapter.

### Tasks

1. **`Signer`** interface in `signing` package:
   ```java
   public interface Signer {
       String keyType();
       String verificationMethod();
       byte[] sign(byte[] data) throws SigningException;
   }
   ```

2. **`ProofGenerator`** in `signing` package:
   - `static DataIntegrityProof generate(Signer signer, JsonObject logEntryWithoutProof)`:
     1. JCS-canonicalize the log entry JSON (proof field must not be present)
     2. Call `signer.sign(canonicalizedBytes)`
     3. Multibase-encode the signature (base58btc with 'z' prefix)
     4. Construct `DataIntegrityProof` with:
        - `type`: "DataIntegrityProof"
        - `cryptosuite`: "eddsa-jcs-2022"
        - `verificationMethod`: from signer
        - `proofPurpose`: "assertionMethod"
        - `created`: current UTC ISO8601
        - `proofValue`: multibase-encoded signature

3. **`ProofVerifier`** in `signing` package:
   - `static boolean verify(DataIntegrityProof proof, JsonObject logEntryWithoutProof)`:
     1. Extract public key from `proof.verificationMethod` (it's a `did:key:z6Mk...#z6Mk...` URI)
     2. Decode the multikey to get raw Ed25519 public key bytes
     3. JCS-canonicalize the log entry JSON
     4. Decode the `proofValue` (multibase base58btc)
     5. Verify Ed25519 signature over canonicalized bytes using public key (BouncyCastle)
   - `static boolean isAuthorized(DataIntegrityProof proof, List<String> activeUpdateKeys)`:
     1. Extract the multikey from proof.verificationMethod
     2. Check if it's in the activeUpdateKeys list

4. **`LocalKeySigner`** in `didwebvh-signing-local` module:
   - `static LocalKeySigner generate()` - generate new Ed25519 keypair using BouncyCastle
   - `static LocalKeySigner fromJson(String json)` - load from JSON format: `{"kty":"OKP","crv":"Ed25519","x":"<base64url>","d":"<base64url>"}`
   - `static LocalKeySigner fromPrivateKey(byte[] privateKeyBytes)` - load from raw bytes
   - `String toJson()` - serialize keypair to JSON
   - `String getPublicKeyMultikey()` - return the multikey-encoded public key (for use in updateKeys)
   - Implement `Signer` interface methods:
     - `keyType()` returns "Ed25519"
     - `verificationMethod()` returns `"did:key:<multikey>#<multikey>"` format
     - `sign(byte[])` signs with Ed25519 private key via BouncyCastle

### Tests

- **ProofGenerator**: Generate a proof with a test signer, verify the structure is correct
- **ProofVerifier**: Verify a known-good proof, verify rejection of tampered data, verify authorized key check
- **Round-trip**: Generate proof -> verify proof (should pass), tamper with data -> verify (should fail)
- **LocalKeySigner**: Generate keypair, sign data, verify signature. JSON round-trip. Load from known JSON.
- **Authorization**: Test `isAuthorized` with matching and non-matching keys

### Acceptance Criteria
- `Signer` interface exists in core module
- `ProofGenerator` and `ProofVerifier` work with Ed25519 eddsa-jcs-2022
- `LocalKeySigner` can generate keys, sign, and verify
- All tests pass
- No dependency from core on `didwebvh-signing-local` (only the interface is in core)

---

## Iteration 5: DID Creation `[NOT STARTED]`

### Goal
Implement the full DID creation flow as specified in spec section 3.6.1. After this iteration, a user can create a new did:webvh DID with a valid first log entry.

### Tasks

1. **`CreateDidConfig`** builder in `create` package:
   - Required: `String domain`, `Signer signer`
   - Optional: `String path`, `Boolean portable`, `Integer ttl`, `List<String> alsoKnownAs`, `WitnessConfig witness`, `List<String> watchers`, `List<String> nextKeyHashes` (pre-rotation), `JsonObject additionalDocumentContent` (services, extra verification methods, etc.)
   - `execute()` method that runs the creation

2. **`CreateDidOperation`** in `create` package. The `execute()` flow:
   1. Build the DID string with `{SCID}` placeholder: `did:webvh:{SCID}:<domain>[:<path>]`
   2. Build initial DID Document with `{SCID}` placeholders in all references
      - `id`: the DID string with placeholder
      - Add verification methods from signer's public key if requested
      - Add services if provided
      - Add controller
      - Add alsoKnownAs if provided
   3. Build initial Parameters:
      - `method`: "did:webvh:1.0"
      - `scid`: "{SCID}"
      - `updateKeys`: [signer's public multikey]
      - `portable`, `ttl`, `witness`, `watchers`, `nextKeyHashes` from config
   4. Build preliminary log entry:
      - `versionId`: "{SCID}"
      - `versionTime`: current UTC ISO8601
      - `parameters`: from step 3
      - `state`: DID Document from step 2
      - No proof field
   5. Generate SCID from preliminary entry using `ScidGenerator`
   6. Replace all `{SCID}` placeholders with actual SCID in the entire JSON (string replacement)
   7. Generate entry hash using `EntryHashGenerator` with SCID as predecessor
   8. Set `versionId` to `"1-<entryHash>"`
   9. Generate Data Integrity proof using `ProofGenerator` with the signer
   10. Attach proof to log entry
   11. Return `CreateDidResult` with: DID string, first `LogEntry`, signer's public key info

3. **`CreateDidResult`** in `create` package:
   - `String did` - the full DID string
   - `LogEntry logEntry` - the first log entry
   - `String logLine` - the JSONL line for `did.jsonl`

4. **`DidWebVh.create(String domain)`** facade method that returns `CreateDidConfig` builder.

### Tests

- Create a DID with minimal config (just domain + signer), verify:
  - DID string format is valid
  - SCID is present and correctly placed
  - versionId is "1-<hash>"
  - versionTime is valid ISO8601
  - Parameters contain method, scid, updateKeys
  - State contains DID Document with correct id
  - Proof is valid (verify with ProofVerifier)
  - Entry hash is valid (verify with EntryHashGenerator)
  - SCID is valid (verify with ScidGenerator)
- Create with all options (portable, ttl, witness, watchers, pre-rotation, services)
- Create with path in domain (e.g., `example.com:dids:issuer`)
- Create with port (e.g., `example.com%3A3000`)
- Verify JSON line output is compact (no whitespace)
- Round-trip: create -> serialize to JSONL -> parse back -> verify all fields

### Acceptance Criteria
- Full DID creation flow works end-to-end
- Created log entries pass SCID verification
- Created log entries pass entry hash verification
- Created log entries have valid Data Integrity proofs
- All JSONL output is compact single-line JSON
- Tests cover normal and edge cases

---

## Iteration 6: DID URL Parsing and DID-to-HTTPS Transformation `[NOT STARTED]`

### Goal
Implement DID URL parsing and the DID-to-HTTPS transformation algorithm from spec section 3.4.

### Tasks

1. **`DidWebVhUrl`** in `url` package:
   - Parse `did:webvh:<SCID>:<domain>[:<path>...]` into components:
     - `String scid`
     - `String domain` (decoded, with port if present)
     - `List<String> pathSegments`
     - `String fragment` (optional, after `#`)
     - `Map<String, String> queryParams` (optional, after `?`)
   - Validate against spec ABNF:
     - Must start with `did:webvh:`
     - SCID must be 46 characters base58btc
     - Domain must be valid
     - Port must be percent-encoded (`%3A`)
   - `toString()` reconstructs the DID string

2. **`DidToHttpsTransformer`** in `url` package:
   - `static String toHttpsUrl(String did)` - full implementation of spec section 3.4:
     1. Remove `did:webvh:` prefix
     2. Remove SCID segment
     3. Transform domain segment (decode percent-encoded port, Unicode normalization, IDNA/Punycode)
     4. Transform path segments (replace `:` with `/`, percent-encode each segment)
     5. Reconstruct HTTPS URL:
        - With port: `https://<domain>:<port>/<path>/did.jsonl`
        - With path: `https://<domain>/<path>/did.jsonl`
        - No path: `https://<domain>/.well-known/did.jsonl`
   - `static String toWitnessUrl(String did)` - same as above but ending in `did-witness.json`
   - `static String toDidWebUrl(String didWebVh)` - convert did:webvh to equivalent did:web URL

3. **Handle DID URL query parameters** for resolution:
   - `?versionId=<full versionId>` - resolve specific version
   - `?versionTime=<ISO8601>` - resolve version active at that time
   - `?versionNumber=<int>` - resolve specific version number (did:webvh extension)

### Tests

- Parse all example DIDs from the spec:
  - `did:webvh:{SCID}:example.com` -> `https://example.com/.well-known/did.jsonl`
  - `did:webvh:{SCID}:issuer.example.com` -> `https://issuer.example.com/.well-known/did.jsonl`
  - `did:webvh:{SCID}:example.com:dids:issuer` -> `https://example.com/dids/issuer/did.jsonl`
  - `did:webvh:{SCID}:example.com%3A3000:dids:issuer` -> `https://example.com:3000/dids/issuer/did.jsonl`
- Parse invalid DIDs and verify `UrlParseException` is thrown
- Round-trip: construct DidWebVhUrl, toString(), parse again, verify equality
- Test witness URL generation
- Test did:web conversion

### Acceptance Criteria
- All DID URL examples from the spec are correctly parsed and transformed
- Invalid URLs throw `UrlParseException` with descriptive messages
- Port percent-encoding is handled correctly
- Path segments are handled correctly
- `.well-known` is used when there's no path

---

## Iteration 7: Log Chain Validation `[NOT STARTED]`

### Goal
Implement the full log chain validation logic from spec section 3.6.2. This is the core security logic.

### Tasks

1. **`LogChainValidator`** in `validate` package:
   - `ValidationResult validate(List<LogEntry> entries, String expectedDid)`:
     For each entry in order:
     1. Parse and validate `versionId` format
     2. Verify version number increments by 1 (starting from 1)
     3. Merge parameters with accumulated active parameters
     4. Validate parameters conform to spec (section 3.7.1):
        - First entry MUST have `method`, `scid`, `updateKeys`
        - `scid` MUST NOT appear in later entries
        - `portable` can ONLY be set to `true` in first entry, cannot change from `false` to `true`
        - `method` must be valid semver, >= previous
     5. For first entry: verify SCID using `ScidGenerator.verify()`
     6. Verify entry hash using `EntryHashGenerator.verify()`
     7. Verify Data Integrity proof:
        - Determine active `updateKeys` (depends on pre-rotation state)
        - Verify proof signature using `ProofVerifier.verify()`
        - Verify signing key is in active `updateKeys` using `ProofVerifier.isAuthorized()`
     8. Verify `versionTime`:
        - Valid ISO8601 UTC
        - Greater than previous entry's versionTime
        - Last entry's versionTime <= current time
     9. Verify DID Document `id` matches `expectedDid` in at least one entry
     10. If pre-rotation is active:
         - All `updateKeys` multikey hashes must match `nextKeyHashes` from previous entry
     11. If deactivated: no further entries allowed after deactivation
     12. If validation fails for an entry: record last valid entry index, stop processing

   - Returns `ValidationResult`:
     - `boolean valid`
     - `int lastValidEntryIndex`
     - `String failureReason` (null if all valid)
     - `int failedEntryIndex` (-1 if all valid)
     - `Parameters activeParameters` (accumulated at last valid entry)

2. **`WitnessValidator`** in `validate` package:
   - `WitnessValidationResult validate(List<LogEntry> entries, WitnessProofCollection witnessProofs, int fromEntryIndex)`:
     For each entry that requires witnessing (from `fromEntryIndex`):
     1. Find the witness proof entry matching this log entry's versionId
     2. Verify at least `threshold` valid proofs exist
     3. For each witness proof:
        - Verify it's signed by a DID in the active witnesses list
        - Verify the Data Integrity proof signature
        - Use the versionId as the signed data
     4. Ignore proofs for unpublished (future) entries
     5. Ignore proofs from witnesses not in the active list

### Tests

- **Valid single-entry log**: create a DID, validate the single-entry log -> valid
- **Valid multi-entry log**: create, update 3 times, validate -> all valid
- **Tampered entry hash**: modify state after creation, validate -> fails at tampered entry
- **Tampered proof**: modify proof value, validate -> fails
- **Wrong signing key**: sign with unauthorized key, validate -> fails
- **Version number gap**: skip version 2 -> fails
- **versionTime ordering**: set versionTime earlier than previous -> fails
- **SCID tampering**: modify SCID in first entry -> fails
- **Pre-rotation**: create with nextKeyHashes, update with matching keys -> valid; update with non-matching -> fails
- **Deactivation**: deactivate, then try to add entry -> fails
- **Parameter validation**: missing method in first entry -> fails, scid in second entry -> fails
- **Witness validation**: threshold met -> valid, threshold not met -> fails, invalid witness signature -> fails
- **Graceful degradation**: valid entries followed by invalid -> returns last valid index

### Acceptance Criteria
- Full log chain validation per spec section 3.6.2
- All validation rules from the spec are implemented
- Witness validation works with threshold logic
- Pre-rotation verification works correctly
- Clear error messages for each type of validation failure
- Validation is the most thoroughly tested component

---

## Iteration 8: DID Resolution `[NOT STARTED]`

### Goal
Implement DID resolution: fetch `did.jsonl` (and `did-witness.json`) from HTTPS, parse, validate, and return the resolved DID Document with metadata.

### Tasks

1. **`HttpDidFetcher`** in `resolve` package:
   - `String fetchDidLog(String httpsUrl)` - HTTP GET, return body as string
   - `String fetchWitnessProofs(String witnessUrl)` - HTTP GET witness file
   - Configurable: timeout (default 10s), max response size (default 200KB)
   - Uses OkHttp
   - Throws `ResolutionException` on HTTP errors (404, 500, timeout, etc.)

2. **`FileDidFetcher`** in `resolve` package:
   - `String fetchDidLog(Path filePath)` - read local file
   - `String fetchWitnessProofs(Path witnessPath)` - read local witness file

3. **`LogProcessor`** in `resolve` package:
   - `ResolveResult process(String didLogContent, String witnessContent, String did, ResolveOptions options)`:
     1. Split `didLogContent` by `\n`, parse each line as `LogEntry`
     2. Parse `witnessContent` as `WitnessProofCollection` (if present)
     3. Call `LogChainValidator.validate()` on entries
     4. If witnesses configured, call `WitnessValidator.validate()`
     5. Apply query parameters:
        - `versionId`: find entry with matching versionId, return that version's DIDDoc
        - `versionTime`: find last entry with versionTime <= requested time
        - `versionNumber`: find entry with matching version number
     6. Build `ResolutionMetadata` from accumulated state
     7. Return `ResolveResult` with DIDDoc, metadata, and any errors

4. **`DidResolver`** in `resolve` package:
   - `ResolveResult resolve(String did)` and `ResolveResult resolve(String did, ResolveOptions options)`:
     1. Parse DID using `DidWebVhUrl`
     2. Transform to HTTPS URL using `DidToHttpsTransformer`
     3. Fetch `did.jsonl` using `HttpDidFetcher`
     4. Optionally fetch `did-witness.json`
     5. Delegate to `LogProcessor`
   - `ResolveResult resolveFromFile(Path didLogPath)` - for local files
   - `ResolveResult resolveFromLog(String rawJsonl, String did)` - for in-memory content

5. **`ResolveOptions`** in `resolve` package:
   - `String versionId`, `String versionTime`, `Integer versionNumber`
   - Builder pattern

6. **`DidWebVh.resolve(String did)`** facade method.

### Tests

- **Resolve from file**: create a DID, write to file, resolve from file, verify
- **Resolve with MockWebServer**: set up mock HTTP server returning valid `did.jsonl`, resolve via HTTPS
- **Resolve specific version**: create + update, resolve with `?versionId=1-...`, verify returns first version
- **Resolve by time**: create at T1, update at T2, resolve with versionTime=T1.5 -> first version
- **Resolve by version number**: resolve with versionNumber=1
- **HTTP errors**: 404 -> `ResolutionException` with notFound error, 500 -> `ResolutionException`
- **Invalid log**: tampered log -> `ResolutionException` with invalidDid error
- **Deactivated DID**: resolve deactivated -> metadata shows deactivated=true, no DIDDoc returned
- **Witness required**: resolve DID with witnesses, provide valid witness file -> success
- **Large log**: 50+ entries log, resolve latest and specific versions
- **Timeout**: mock slow server, verify timeout handling

### Acceptance Criteria
- Full resolution flow works end-to-end (create -> write -> resolve -> verify)
- HTTP resolution with configurable timeout and max size
- File-based resolution for testing and offline use
- Query parameter filtering (versionId, versionTime, versionNumber)
- Resolution metadata matches spec section 3.6.2
- Error responses follow spec (notFound, invalidDid with problemDetails)

---

## Iteration 9: DID Update, Migration, and Deactivation `[NOT STARTED]`

### Goal
Implement all update operations from spec section 3.6.3 and deactivation from section 3.6.4.

### Tasks

1. **`UpdateDidConfig`** builder in `update` package:
   - Required: `DidWebVhState existingState`, `Signer signer`
   - Optional: new `DidDocument`, new `Parameters` (partial), new service endpoints, etc.
   - `execute()` method

2. **`UpdateDidOperation`** in `update` package:
   - Standard update flow (spec section 3.6.3):
     1. Take existing state (all previous log entries + accumulated parameters)
     2. Build new parameters (merge provided changes with active parameters)
     3. Build preliminary log entry:
        - `versionId`: previous entry's versionId (will be replaced)
        - `versionTime`: current UTC ISO8601
        - `parameters`: the changed parameters (only diffs, or `{}` if none)
        - `state`: the new DID Document
     4. Generate entry hash with previous versionId as predecessor
     5. Set versionId to `"<n>-<entryHash>"` where n = previous version + 1
     6. Generate Data Integrity proof
     7. Return updated log entry

3. **`MigrateDidOperation`** in `update` package:
   - Migration flow (spec section 3.7.6):
     1. Verify `portable` is `true` in current parameters
     2. Build new DID string with new domain
     3. Rewrite all DID references in the DID Document
     4. Add previous DID to `alsoKnownAs`
     5. Create a standard update entry with the new references

4. **`DeactivateDidOperation`** in `update` package:
   - Deactivation flow (spec section 3.6.4):
     1. If pre-rotation is active: create intermediate entry to turn off pre-rotation (`nextKeyHashes: []`)
     2. Create final entry with `deactivated: true` and `updateKeys: []`
     3. Return one or two entries

5. **`DidWebVhState`** in `core` package:
   - Holds: `String did`, `List<LogEntry> logEntries`, `WitnessProofCollection witnessProofs`, `Parameters activeParameters`, `boolean validated`, `boolean deactivated`
   - `appendEntry(LogEntry entry)` - add new entry to log
   - `toDidLog()` - serialize all entries to JSONL string (for `did.jsonl`)
   - `toJson()` / `fromJson()` - save/load full state for caching
   - `validate()` - re-validate the log chain

6. **Facade methods**: `DidWebVh.update(state)`, `DidWebVh.migrate(state, newDomain)`, `DidWebVh.deactivate(state)`

### Tests

- **Simple update**: create, update document (add service), verify new entry is valid, full log validates
- **Key rotation**: create, update with new updateKeys, verify old key can't sign new entries
- **Parameter update**: change TTL, verify parameters merge correctly
- **Multiple updates**: create, update 5 times, verify full chain validates
- **Migration**: create with portable=true, migrate to new domain, verify alsoKnownAs, verify full chain
- **Migration without portable**: attempt migration with portable=false -> error
- **Deactivation**: create, deactivate, verify deactivated=true, updateKeys=[]
- **Deactivation with pre-rotation**: create with pre-rotation, deactivate -> two entries generated
- **Update after deactivation**: attempt update after deactivation -> error
- **End-to-end**: create -> update 3x -> migrate -> update 2x -> deactivate -> resolve each version

### Acceptance Criteria
- All update operations follow the spec exactly
- Log chain validates after every operation
- Migration preserves SCID and history
- Deactivation correctly handles pre-rotation edge case
- `DidWebVhState` tracks full state correctly

---

## Iteration 10: did:web Parallel Publishing `[NOT STARTED]`

### Goal
Implement the parallel did:web document generation from spec section 3.7.10.

### Tasks

1. **`DidWebPublisher`** in `didweb` package:
   - `static DidDocument toDidWeb(ResolveResult resolvedWebVh)`:
     1. Start with the resolved DIDDoc from did:webvh
     2. Add implicit services (#files, #whois) if not already present, with serviceEndpoint derived from DID-to-HTTPS transformation
     3. Replace all `did:webvh:<SCID>:` with `did:web:` in the document
     4. Set controller to the original did:webvh DID
     5. Add the full did:webvh DID to alsoKnownAs
     6. Remove duplicates from alsoKnownAs
   - `static String toDidWebUrl(String didWebVhUrl)` - convert did:webvh DID to did:web DID

### Tests

- Convert a basic did:webvh DIDDoc to did:web, verify all references updated
- Verify implicit services are added
- Verify controller is set to did:webvh
- Verify alsoKnownAs contains did:webvh
- Verify no duplicate alsoKnownAs entries

### Acceptance Criteria
- Parallel did:web document generation follows spec section 3.7.10
- All DID references correctly converted
- Implicit services injected

---

## Iteration 11: Interactive Wizard CLI `[NOT STARTED]`

### Goal
Build an interactive CLI wizard in the `didwebvh-wizard` module, similar to the Rust implementation.

### Tasks

1. **`WizardMain`** in `wizard` package:
   - Main menu using picocli + JLine:
     ```
     === did:webvh Wizard ===
     1. Create a new DID
     2. Update an existing DID
     3. Resolve a DID
     4. Exit
     ```

2. **`CreateWizard`**:
   - Prompts for:
     1. Web address (domain, optional path)
     2. Generate or import authorization key (Ed25519)
     3. DID Document content:
        - Verification methods and relationships
        - Services (JSON input)
        - Controller
        - AlsoKnownAs
     4. Parameters:
        - Portable (yes/no)
        - Pre-rotation keys (yes/no, generate next keys)
        - Witnesses (add witness DIDs, set threshold)
        - Watchers (add URLs)
        - TTL
   - Executes `DidWebVh.create()` with collected config
   - Saves outputs:
     - `did.jsonl` - the DID log
     - `did-secrets.json` - the signing key (WARNING: keep secure)
     - `did-witness.json` - witness proofs (if witnesses configured)
   - Displays the created DID and file locations

3. **`UpdateWizard`**:
   - Loads existing `did.jsonl` and `did-secrets.json`
   - Sub-menu:
     1. **Modify** - Edit document/parameters
     2. **Migrate** - Move to new domain
     3. **Deactivate** - Permanent deactivation (with confirmation)
   - Executes the chosen operation
   - Updates `did.jsonl` with new entry

4. **`ResolveWizard`**:
   - Prompts for DID string
   - Optionally: version filters (versionId, versionTime, versionNumber)
   - Resolves and displays:
     - DID Document (pretty-printed JSON)
     - Resolution Metadata
     - Validation status

5. **Build the wizard as an executable JAR** with `maven-shade-plugin` or `maven-assembly-plugin` in the wizard module POM.

### Tests

- Test the wizard components with pre-configured inputs (no interactive prompts in tests)
- Verify file output formats (did.jsonl, did-secrets.json)
- Verify round-trip: create with wizard -> resolve with wizard
- Test error handling (invalid inputs, missing files)

### Acceptance Criteria
- Wizard runs interactively from `java -jar didwebvh-wizard.jar`
- Create flow produces valid `did.jsonl` and `did-secrets.json`
- Update flow correctly appends to existing `did.jsonl`
- Resolve flow shows formatted output
- Deactivation requires confirmation
- Non-interactive mode works for CI/testing

---

## Iteration 12: Test Vectors and Spec Compliance `[NOT STARTED]`

### Goal
Add test vectors from the spec examples and from the Rust implementation. Ensure full spec compliance.

### Tasks

1. **Create test vector files** in `src/test/resources/test-vectors/`:
   - `first-log-entry-good.jsonl` - a valid single-entry DID log
   - `first-log-entry-tampered.jsonl` - same but with tampered data
   - `multi-entry-log.jsonl` - multi-version DID log with updates
   - `multi-entry-witness.json` - witness proofs for the multi-entry log
   - `deactivated-did.jsonl` - a deactivated DID log
   - `migrated-did.jsonl` - a DID that was migrated to a new domain
   - `pre-rotation-log.jsonl` - a DID using pre-rotation keys
   - Generate these by running the library's create/update operations with deterministic keys and timestamps

2. **Spec compliance tests** (as integration tests in `src/test/java/.../integration/`):
   - Test every MUST requirement from the spec:
     - SCID generation and verification
     - Entry hash generation and verification
     - Data Integrity proof eddsa-jcs-2022
     - DID-to-HTTPS transformation (all examples from spec)
     - Parameter rules (method required in first entry, scid only in first, portable immutable, etc.)
     - Pre-rotation constraints
     - Witness threshold algorithm
     - Deactivation rules
     - Resolution metadata format

3. **Cross-implementation compatibility** (if test vectors available from Rust/TypeScript implementations):
   - Verify this library can resolve/validate DIDs created by other implementations
   - Verify DIDs created by this library can be validated by other implementations

4. **Property-based tests** (optional but valuable):
   - Any valid create followed by any number of valid updates always produces a valid log chain
   - Tampering with any byte of any entry always causes validation failure

### Acceptance Criteria
- Test vectors cover all major spec features
- All spec MUST requirements are tested
- Test coverage > 80% (ideally > 90% for core module)
- All tests pass on Java 11, 17, 21, and 25

---

## Iteration 13: Quality, CI Finalization, and Documentation `[NOT STARTED]`

### Goal
Finalize CI badges, quality gates, documentation, and prepare for first release.

### Tasks

1. **Fix any Checkstyle violations** across all modules.

2. **Fix any SpotBugs findings** across all modules.

3. **Ensure JaCoCo coverage > 80%** for core module. Add tests if needed.

4. **Configure SonarCloud**:
   - Set up project on sonarcloud.io
   - Configure quality gate (coverage, duplications, bugs, code smells)
   - Ensure badge in README works

5. **Configure Codecov**:
   - Add `codecov.yml` if needed
   - Ensure coverage badge in README works

6. **Update README.md** with:
   - Accurate badges (now pointing to real CI, Codecov, SonarCloud, Maven Central)
   - Final API examples reflecting actual API
   - Complete feature list
   - Build and test instructions
   - License and contributing sections

7. **Add Javadoc** to all public classes and methods in core module.

8. **Create `CHANGELOG.md`** with initial version entry.

9. **Configure Maven Central publishing** in parent POM using a `release` profile:
   - The `release` profile is activated only during release builds (not normal `./mvnw verify`)
   - `maven-gpg-plugin` for JAR signing:
     ```xml
     <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-gpg-plugin</artifactId>
       <version>3.2.4</version>
       <executions>
         <execution>
           <id>sign-artifacts</id>
           <phase>verify</phase>
           <goals><goal>sign</goal></goals>
           <configuration>
             <gpgArguments>
               <arg>--pinentry-mode</arg>
               <arg>loopback</arg>
             </gpgArguments>
           </configuration>
         </execution>
       </executions>
     </plugin>
     ```
   - `central-publishing-maven-plugin` for Sonatype Central publishing (the new portal):
     ```xml
     <plugin>
       <groupId>org.sonatype.central</groupId>
       <artifactId>central-publishing-maven-plugin</artifactId>
       <version>0.7.0</version>
       <extensions>true</extensions>
       <configuration>
         <publishingServerId>central</publishingServerId>
         <autoPublish>true</autoPublish>
       </configuration>
     </plugin>
     ```
   - `maven-source-plugin` (attach source JAR — required by Maven Central)
   - `maven-javadoc-plugin` (attach javadoc JAR — required by Maven Central)
   - Add server credentials in the workflow (NOT in pom.xml):
     ```xml
     <!-- This goes in the CI-generated settings.xml, not committed to repo -->
     <server>
       <id>central</id>
       <username>${env.OSSRH_USERNAME}</username>
       <password>${env.OSSRH_TOKEN}</password>
     </server>
     ```

10. **Create release workflow** (`.github/workflows/release.yml`):
    - Triggered on tag push (`v*`)
    - Uses these **GitHub repo secrets** (configured at `Settings > Secrets and variables > Actions`):
      | Secret Name | Value | Purpose |
      |-------------|-------|---------|
      | `GPG_PRIVATE_KEY` | Full output of `gpg --armor --export-secret-keys <KEY_ID>` | Signs JARs for Maven Central |
      | `GPG_PASSPHRASE` | GPG key passphrase | Unlocks the GPG key in CI |
      | `OSSRH_USERNAME` | Sonatype Central portal username | Authenticates to publish |
      | `OSSRH_TOKEN` | Sonatype Central portal token (generate at central.sonatype.com) | Authenticates to publish |
    - Full workflow structure:
      ```yaml
      name: Release to Maven Central
      on:
        push:
          tags: ['v*']

      jobs:
        release:
          runs-on: ubuntu-latest
          steps:
            - uses: actions/checkout@v4

            - name: Set up Java 11
              uses: actions/setup-java@v4
              with:
                java-version: '11'
                distribution: 'temurin'
                server-id: central
                server-username: OSSRH_USERNAME
                server-password: OSSRH_TOKEN
                gpg-private-key: ${{ secrets.GPG_PRIVATE_KEY }}
                gpg-passphrase: GPG_PASSPHRASE

            - name: Publish to Maven Central
              run: ./mvnw clean deploy -P release -B --no-transfer-progress
              env:
                OSSRH_USERNAME: ${{ secrets.OSSRH_USERNAME }}
                OSSRH_TOKEN: ${{ secrets.OSSRH_TOKEN }}
                GPG_PASSPHRASE: ${{ secrets.GPG_PASSPHRASE }}

            - name: Create GitHub Release
              uses: softprops/action-gh-release@v2
              with:
                generate_release_notes: true
                files: |
                  didwebvh-core/target/*.jar
                  didwebvh-wizard/target/*-shaded.jar
      ```
    - Note: `actions/setup-java@v4` handles importing the GPG key and creating `settings.xml` with the server credentials. The `server-username` and `server-password` fields are the **env variable names** (not the values), which the step maps to the actual secrets via the `env` block.

11. **Create SECURITY.md** with vulnerability reporting instructions.

12. **Verify all badges work** (may need real CI runs first -- placeholder badge URLs are fine initially).

### Acceptance Criteria
- CI runs green on all Java versions (11, 17, 21, 25)
- Checkstyle, SpotBugs, JaCoCo all pass
- SonarCloud quality gate passes
- README accurately reflects the project
- All public API has Javadoc
- Maven Central publishing is configured with the `release` profile
- Release workflow exists and references all 4 required GitHub secrets (`GPG_PRIVATE_KEY`, `GPG_PASSPHRASE`, `OSSRH_USERNAME`, `OSSRH_TOKEN`)
- Tagging `v0.1.0` and pushing triggers the full release pipeline

---

## Iteration 14: Performance and Edge Cases `[NOT STARTED]`

### Goal
Handle edge cases, optimize for real-world usage, add benchmarks.

### Tasks

1. **Large DID logs**: Test and optimize resolution of logs with 100+ entries. Ensure no excessive memory usage.

2. **Concurrent resolution**: Verify `DidResolver` works correctly when used from multiple threads.

3. **Edge cases**:
   - Empty DID Document (just `id`)
   - Maximum-size DID Document (near 200KB response limit)
   - Unicode in DID Document content
   - Internationalized domain names
   - Very long paths in DID URLs
   - Null/empty optional parameters in all combinations

4. **Timeout and retry**: Configurable HTTP timeout. No automatic retry (let callers handle it).

5. **Response size limit**: Configurable max response size for HTTP resolution (default 200KB).

6. **Helpful error messages**: Review all exception messages. Each should tell the user:
   - What went wrong
   - Which entry/field caused the problem
   - What the expected value was vs. the actual value

7. **Add benchmarks** (optional, using JMH):
   - DID creation time
   - Log chain validation time for N entries
   - Resolution time (from file)

### Acceptance Criteria
- No OutOfMemoryError on 100+ entry logs
- Thread safety verified for stateless components
- All edge cases have tests
- Error messages are clear and actionable
- HTTP timeouts work correctly
