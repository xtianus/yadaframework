# Yada Saved Property Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a reusable, type-safe persistent key/value facility to `YadaWeb`, backed by the `YadaSavedProperty` JPA entity and accessed through typed keys, codecs, and a dedicated DAO.

**Architecture:** Persist every value as canonical text together with a logical type discriminator. Applications declare immutable `YadaSavedPropertyKey<T>` constants carrying scope, name, codec, Java type, and an optional default; `YadaSavedPropertyDao` validates the stored discriminator before decoding. Application scopes and property names are trimmed and lowercased with `Locale.ROOT`; a null or blank application scope becomes the non-null value `"default"`, so `(applicationName, name)` has one canonical representation and can be enforced as a reliable unique key on MySQL.

**Tech Stack:** Java 21, Jakarta Persistence 3.2, Hibernate 7.1, Spring ORM transactions, Jackson 2.20 for explicitly typed JSON codecs, JUnit 5, Mockito, Gradle.

## Global Constraints

- Every new YadaWeb class and interface must start with `Yada`.
- Use tabs for Java indentation and do not reformat unrelated code.
- Add English Javadoc to every new class and public method.
- Keep persistence logic in `YadaSavedPropertyDao`; do not expose a generic raw entity save API that bypasses type checks.
- Normalize both `applicationName` and `name` by trimming and lowercasing with `Locale.ROOT`; accept null or blank `applicationName` at API boundaries, but persist `"default"` in a `NOT NULL` column.
- Apply identifier normalization in typed keys and entity constructors, setters, and lifecycle callbacks so all framework persistence paths store lowercase identifiers.
- Treat a missing row as an absent value; reject null property values and require explicit deletion.
- Do not persist Java class names and do not use Java native serialization.
- Read values by key only; do not add value-based filtering or sorting APIs.
- Runtime entity discovery already scans `net.yadaframework.persistence.entity`; `persistence.test.xml` still needs an explicit entry for schema generation.
- `YadaWeb` has no application Flyway migration directory. Update its tracked generated test schema; consuming applications must create their own additive Flyway migration when adopting this optional entity.
- Do not modify an existing versioned Flyway migration.
- Do not create or amend Git commits during implementation; leave all changes in the working tree for the user to review.

## File Map

### Create

- `YadaWeb/src/main/java/net/yadaframework/persistence/YadaSavedPropertyTypeEnum.java` — stable logical storage types.
- `YadaWeb/src/main/java/net/yadaframework/persistence/YadaSavedPropertyCodec.java` — conversion contract between `T` and canonical text.
- `YadaWeb/src/main/java/net/yadaframework/persistence/YadaSavedPropertyCodecs.java` — built-in codec factories.
- `YadaWeb/src/main/java/net/yadaframework/persistence/YadaSavedPropertyKey.java` — immutable typed property declaration and canonical lowercase identifier normalization.
- `YadaWeb/src/main/java/net/yadaframework/persistence/entity/YadaSavedProperty.java` — JPA row and uniqueness/versioning invariants.
- `YadaWeb/src/main/java/net/yadaframework/persistence/repository/YadaSavedPropertyDao.java` — typed transactional persistence API.
- `YadaWeb/src/test/java/net/yadaframework/persistence/YadaSavedPropertyCodecsTest.java` — codec round-trip and malformed-value coverage.
- `YadaWeb/src/test/java/net/yadaframework/persistence/YadaSavedPropertyKeyTest.java` — key validation, normalization, and default coverage.
- `YadaWeb/src/test/java/net/yadaframework/persistence/entity/YadaSavedPropertyTest.java` — entity invariant coverage.
- `YadaWeb/src/test/java/net/yadaframework/persistence/repository/YadaSavedPropertyDaoTest.java` — DAO behavior with mocked JPA collaborators.
- `YadaDocs/src/docs/asciidoc/en/database/savedProperties.adoc` — public setup and usage documentation.

### Modify

- `YadaWeb/src/test/resources/META-INF/persistence.test.xml:8-14` — register `YadaSavedProperty` for schema generation.
- `YadaWeb/schema/V1__yadatest.sql` — regenerate with `testDbSchema`; do not hand-format the generated statement.
- `YadaDocs/src/docs/asciidoc/en/database/overview.adoc:6-11` — link the new saved-properties subsection.

## Task 1: Typed keys and canonical codecs

**Files:**
- Create: `YadaWeb/src/main/java/net/yadaframework/persistence/YadaSavedPropertyTypeEnum.java`
- Create: `YadaWeb/src/main/java/net/yadaframework/persistence/YadaSavedPropertyCodec.java`
- Create: `YadaWeb/src/main/java/net/yadaframework/persistence/YadaSavedPropertyCodecs.java`
- Create: `YadaWeb/src/main/java/net/yadaframework/persistence/YadaSavedPropertyKey.java`
- Test: `YadaWeb/src/test/java/net/yadaframework/persistence/YadaSavedPropertyCodecsTest.java`
- Test: `YadaWeb/src/test/java/net/yadaframework/persistence/YadaSavedPropertyKeyTest.java`

**Interfaces:**
- Produces `YadaSavedPropertyTypeEnum` values `STRING`, `BOOLEAN`, `LONG`, `DECIMAL`, `INSTANT`, `ENUM`, and `JSON`.
- Produces this codec contract:

```java
Class<T> getJavaType();
YadaSavedPropertyTypeEnum getType();
String serialize(T value);
T deserialize(String value);
```

- Produces `YadaSavedPropertyCodecs.stringCodec()`, `booleanCodec()`, `longCodec()`, `decimalCodec()`, `instantCodec()`, `enumCodec(Class<E>)`, and `jsonCodec(Class<T>, ObjectMapper)`.
- Produces `YadaSavedPropertyKey.of(applicationName, name, codec)` and `YadaSavedPropertyKey.withDefault(applicationName, name, codec, defaultValue)`.
- Produces `YadaSavedPropertyKey.DEFAULT_APPLICATION_NAME` with value `"default"` and shared `normalizeApplicationName(String)` and `normalizeName(String)` methods that lowercase with `Locale.ROOT`.

- [ ] **Step 1: Write failing codec tests**

Cover canonical round trips for `String`, `Boolean`, `Long`, `BigDecimal`, `Instant`, and a nested `YadaSavedPropertyTestEnum`. Assert that `Instant` uses `DateTimeFormatter.ISO_INSTANT`, decimals use `BigDecimal.toPlainString()`, booleans serialize only as lowercase `true`/`false`, and enums serialize by constant name rather than ordinal.

- [ ] **Step 2: Add malformed-value and JSON safety tests**

Assert that invalid boolean, number, instant, and enum text raises `YadaInvalidValueException`. For JSON, pass a test-local `ObjectMapper` and an explicit nested `YadaSavedPropertyJsonFixture` class; verify round-trip behavior and failure on malformed JSON. Confirm that no codec stores a Java class name in its encoded value.

- [ ] **Step 3: Write failing key tests**

Test that null, empty, and whitespace-only application names become `"default"`; explicit scopes and property names are trimmed and lowercased with `Locale.ROOT`; mixed-case inputs produce the same canonical identifiers; blank or overlength normalized names are rejected; null codecs/defaults are rejected; and keys with or without defaults report their state correctly.

- [ ] **Step 4: Run focused tests and confirm the expected failure**

Run from `YadaWeb`:

```powershell
.\gradlew test --tests "net.yadaframework.persistence.YadaSavedPropertyCodecsTest" --tests "net.yadaframework.persistence.YadaSavedPropertyKeyTest"
```

Expected result: compilation fails because the four production types do not exist yet.

- [ ] **Step 5: Implement the logical type enum and codec contract**

Keep the enum independent from JPA localization: it is an internal storage discriminator, not a `YadaLocalEnum` and not a `YadaPersistentEnum`. Document that new logical formats require a new enum constant but no database column change.

- [ ] **Step 6: Implement built-in codecs**

Use strict parsers and wrap conversion failures in `YadaInvalidValueException` with the logical type and bad text in the message. The JSON factory must require both the target `Class<T>` and caller-supplied `ObjectMapper`, thereby restricting deserialization to the type declared by the key.

- [ ] **Step 7: Implement the immutable typed key**

Store the canonical lowercase application name and property name, codec, and `Optional<T>` default. Normalize with `strip().toLowerCase(Locale.ROOT)` before applying the entity-aligned maximum lengths of 64 characters for `applicationName` and 191 for `name`. Expose getters only.

- [ ] **Step 8: Re-run focused tests**

Run the command from Step 4. Expected result: all key and codec tests pass.

- [ ] **Step 9: Review checkpoint**

Verify that every new type and public method has English Javadoc, every new type is prefixed `Yada`, identifiers are canonical lowercase, and there is no native Java serialization or unrestricted JSON polymorphism. Do not create or amend a Git commit.

## Task 2: Entity mapping and generated schema

**Files:**
- Create: `YadaWeb/src/main/java/net/yadaframework/persistence/entity/YadaSavedProperty.java`
- Test: `YadaWeb/src/test/java/net/yadaframework/persistence/entity/YadaSavedPropertyTest.java`
- Modify: `YadaWeb/src/test/resources/META-INF/persistence.test.xml:8-14`
- Modify: `YadaWeb/schema/V1__yadatest.sql`

**Interfaces:**
- Consumes `YadaSavedPropertyTypeEnum`, `YadaSavedPropertyKey.normalizeApplicationName(String)`, and `YadaSavedPropertyKey.normalizeName(String)`.
- Produces a `YadaSavedProperty` entity with technical `id`, semantic columns `applicationName`, `name`, `type`, `value`, and optimistic-lock `version`.
- Produces a named unique constraint on `(applicationName, name)`.

- [ ] **Step 1: Write the failing entity tests**

Verify constructor/default state and setters: null/blank application names normalize to `"default"`; explicit scopes and property names are trimmed and lowercased with `Locale.ROOT`; `type` and `value` are retained; lifecycle callbacks reapply canonical normalization; and getters expose `id` and `version` for JPA/diagnostics. Use reflection only where needed to inspect generated-id/version fields or lifecycle normalization.

- [ ] **Step 2: Run the focused entity test and confirm failure**

```powershell
.\gradlew test --tests "net.yadaframework.persistence.entity.YadaSavedPropertyTest"
```

Expected result: compilation fails because `YadaSavedProperty` does not exist.

- [ ] **Step 3: Implement `YadaSavedProperty`**

Map the entity as follows:

- `id`: `Long`, `@Id`, identity generation.
- `applicationName`: length 64, `nullable=false`, initialized and normalized to `"default"`, with Hibernate `@ColumnDefault("'default'")` so generated DDL also records the default.
- `name`: length 191, `nullable=false`; no special case-sensitive collation is needed because Java lifecycle normalization stores canonical lowercase names.
- `type`: `YadaSavedPropertyTypeEnum`, `@Enumerated(EnumType.STRING)`, length 32, `nullable=false`.
- `value`: `String`, `@Lob`, `nullable=false`.
- `version`: primitive `long`, `@Version`.
- table constraint: explicitly named unique constraint over `applicationName` and `name`.

Add `@PrePersist` and `@PreUpdate` normalization/validation so direct entity use cannot persist a null scope, mixed-case identifiers, or an incomplete row. Constructors and setters must normalize immediately, and lifecycle callbacks must reapply the shared key helpers. Reuse `YadaInvalidValueException` for invalid state. Do not add inheritance or `Serializable` without a concrete framework requirement.

- [ ] **Step 4: Run the entity test**

Run the command from Step 2. Expected result: the entity tests pass.

- [ ] **Step 5: Register the entity for YadaWeb schema generation**

Add `net.yadaframework.persistence.entity.YadaSavedProperty` to `persistence.test.xml` alongside the other `net.yadaframework.persistence.entity` entries. No runtime configuration change is needed because `YadaJpaConfig` already scans that package.

- [ ] **Step 6: Regenerate and inspect the test schema**

```powershell
.\gradlew testDbSchema
```

Expected result: `schema/V1__yadatest.sql` contains `YadaSavedProperty` with `id`, non-null scope/name/type/value, `version`, primary key, and the composite unique constraint. Confirm that `applicationName` has the SQL default `default`, `name` has no special case-sensitive collation, `value` is generated as `longtext`, and `type` as a character column; preserve generated one-statement-per-line formatting.

- [ ] **Step 7: Review checkpoint**

Confirm that MySQL cannot store multiple null scopes, while Java callers can still pass null and obtain the `"default"` scope, and that every framework persistence path stores lowercase scope/name identifiers. Do not create or amend a Git commit.

## Task 3: Type-safe transactional DAO

**Files:**
- Create: `YadaWeb/src/main/java/net/yadaframework/persistence/repository/YadaSavedPropertyDao.java`
- Test: `YadaWeb/src/test/java/net/yadaframework/persistence/repository/YadaSavedPropertyDaoTest.java`

**Interfaces:**
- Consumes `YadaSavedPropertyKey<T>`, its codec, and `YadaSavedProperty`.
- Produces this public API:

```java
<T> Optional<T> findValue(YadaSavedPropertyKey<T> key);
<T> T getValue(YadaSavedPropertyKey<T> key);
<T> void setValue(YadaSavedPropertyKey<T> key, T value);
boolean delete(YadaSavedPropertyKey<?> key);
```

- `getValue` returns the stored value, otherwise the key default, otherwise throws `YadaInvalidUsageException`.
- `setValue` rejects null, inserts when absent, updates the managed entity when present, and never silently changes an existing row’s logical type.

- [ ] **Step 1: Write failing read-path DAO tests**

Using JUnit 5, Mockito, `EntityManager`, and `TypedQuery<YadaSavedProperty>`, cover: missing row returns `Optional.empty()`; `getValue` uses a declared default; missing row without default throws; matching type decodes; mismatched type throws `YadaInvalidValueException`; malformed stored text is wrapped with scope/name context; multiple rows raise `YadaInternalException` instead of hiding corruption.

- [ ] **Step 2: Write failing write/delete DAO tests**

Cover: mixed-case key input queries with lowercase scope/name parameters; creating a row calls `persist` with lowercase scope/name plus the expected type/canonical value; updating a managed row changes only its value; writing through a key with a different type is rejected; null values are rejected; delete removes an existing managed row and returns true; delete returns false when absent.

- [ ] **Step 3: Run the focused DAO test and confirm failure**

```powershell
.\gradlew test --tests "net.yadaframework.persistence.repository.YadaSavedPropertyDaoTest"
```

Expected result: compilation fails because the DAO does not exist.

- [ ] **Step 4: Implement DAO lookup and decoding**

Annotate the class with `@Repository` and `@Transactional(readOnly=true)`, inject `EntityManager` with `@PersistenceContext`, and use a typed JPQL query on both key columns. Keep entity lookup private, return all matches long enough to detect invariant violations, validate `entity.type == key.codec.type`, then deserialize and verify `key.codec.javaType.isInstance(result)`.

- [ ] **Step 5: Implement transactional writes and deletion**

Annotate write methods with `@Transactional(readOnly=false)`. Load and mutate an existing managed entity without merging; construct and persist a new entity when absent. Preserve the row type after creation and rely on `@Version` for stale-update detection.

- [ ] **Step 6: Run DAO and full YadaWeb tests**

```powershell
.\gradlew test --tests "net.yadaframework.persistence.repository.YadaSavedPropertyDaoTest"
.\gradlew test
```

Expected result: focused and full test suites pass.

- [ ] **Step 7: Review checkpoint**

Confirm the DAO does not expose raw `save(YadaSavedProperty)`, does not return detached entities, always queries canonical lowercase identifiers, and logs neither saved values nor JSON payloads. Do not create or amend a Git commit.

## Task 4: Yada documentation and consumer migration contract

**Files:**
- Create: `YadaDocs/src/docs/asciidoc/en/database/savedProperties.adoc`
- Modify: `YadaDocs/src/docs/asciidoc/en/database/overview.adoc:6-11`

**Interfaces:**
- Documents declaration of application-owned typed keys and injection/use of `YadaSavedPropertyDao`.
- Documents that consumers must add `YadaSavedProperty` to their schema-generation `persistence.xml` and create an additive Flyway migration containing the generated table and unique constraint.

- [ ] **Step 1: Write the focused AsciiDoc page**

In English, cover intended uses such as last-run timestamps and feature state; explain when a normal domain table/column is preferable; list built-in codecs and canonical forms; explain that application scopes and property names are trimmed and lowercased with `Locale.ROOT`, with blank scopes becoming `"default"`; show one key declaration and read/write/delete examples; describe defaults, missing keys, type mismatch, malformed values, and optimistic locking.

Keep every code sample at five lines or fewer. Include a warning that saved properties are not a secret store and that arbitrary polymorphic JSON/native Java serialization are unsupported.

- [ ] **Step 2: Document schema adoption**

State that runtime package scanning discovers the entity, while schema generation requires this entry:

```xml
<class>net.yadaframework.persistence.entity.YadaSavedProperty</class>
```

Require each released application to regenerate its schema, diff against the previous revision, and add a new Flyway migration rather than editing old migrations. Do not add a Flyway migration to `YadaWeb`, because it is a library and owns no runtime database.

- [ ] **Step 3: Link the page from the database overview**

Add `savedProperties.adoc` to the existing Subsections list with a concise description such as “Persisting typed application state by key.”

- [ ] **Step 4: Build documentation when repository tooling is available**

Run from `YadaDocs`:

```powershell
.\gradlew asciidoctor
```

Expected result: AsciiDoc conversion succeeds and the overview link resolves. The current checkout is missing `YadaDocs/gradle/wrapper/gradle-wrapper.jar`; if that pre-existing condition remains, report it as a tooling blocker instead of changing Gradle configuration or downloading an unreviewed wrapper.

- [ ] **Step 5: Run final YadaWeb verification**

Run from `YadaWeb`:

```powershell
.\gradlew clean test testDbSchema javadoc
```

Expected result: compilation, tests, schema generation, and Javadoc complete successfully. Inspect `git diff --check`, verify only planned files changed, and confirm the regenerated schema still has one SQL statement per line.

- [ ] **Step 6: Final review**

Check every requirement against the implementation: all new types start with `Yada`; the entity has all requested semantic columns plus technical `id`; application scopes and property names persist in canonical lowercase form, with null application scopes becoming `"default"`; uniqueness is enforced; all supported codecs round-trip; defaults and corruption paths are tested; and consumer migration instructions are explicit. Do not create or amend a Git commit.

## Acceptance Criteria

- `YadaSavedProperty` persists `applicationName`, `name`, `type`, `value`, and `version`, plus an identity primary key.
- `(applicationName, name)` is unique and both columns are non-null.
- Null/blank application scope is consistently represented as `"default"` in keys, entities, queries, and generated DDL.
- Non-blank application scopes and all property names are trimmed and lowercased with `Locale.ROOT` in keys, entities, queries, and persisted rows.
- Built-in typed keys support string, boolean, long, decimal, instant, enum, and explicitly typed JSON values.
- The DAO rejects type drift, malformed persisted values, null writes, and duplicate rows with Yada exceptions carrying scope/name context.
- Missing values can be handled as `Optional` or by a key-level default.
- Unit tests cover codecs, keys, entity invariants, DAO reads/writes/deletion, and failure paths.
- `persistence.test.xml` and the tracked YadaWeb test schema include the entity.
- Yada documentation explains API use and the Flyway responsibility of consuming applications.
- No existing migration, unrelated source, build security policy, or dependency version is modified.
