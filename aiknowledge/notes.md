# Yada Framework Project Notes

## Saved properties

`YadaWeb` provides typed persistent application state through `YadaSavedPropertyKey<T>`, `YadaSavedPropertyCodec<T>`, `YadaSavedPropertyCodecs`, `YadaSavedProperty`, and `YadaSavedPropertyDao`.

Saved-property values are stored as canonical text with a `YadaSavedPropertyTypeEnum` discriminator. Built-in codecs support strings, booleans, longs, decimals, instants, enums, and explicitly typed JSON using a caller-supplied `ObjectMapper`.

Both application scopes and property names are canonical identifiers: they are stripped and lowercased with `Locale.ROOT`. A null or blank application scope becomes `default`; a blank property name is invalid. `YadaSavedPropertyKey` owns the shared normalization helpers, and the entity reuses them in constructors, setters, and persistence lifecycle callbacks.

The entity maps `type` with `@Enumerated(STRING)` plus `length` and `value` with a bare `@Lob`, without any `columnDefinition`: the MySQL dialect already generates `varchar(32)` and `longtext`, matching the `@Lob` mapping used by `YadaClause`.

The `@PrePersist`/`@PreUpdate` callback re-normalizes identifiers as a safety net for state populated without setters. It is not a reliable normalization point for Hibernate updates, because dirty state is computed before the callback runs; every framework write path normalizes through constructors and setters instead.

`YadaSavedPropertyDao` reads by the `(applicationName, name)` key, validates the stored logical type before decoding, and exposes `findValue`, `getValue`, `setValue`, and `delete`. Missing rows are represented by `Optional.empty()` or a key default. Null writes, malformed text, type drift, and duplicate rows raise Yada exceptions with key context.

The entity is discovered by runtime package scanning. Schema-generation persistence units must explicitly list `net.yadaframework.persistence.entity.YadaSavedProperty`. Consuming applications own the additive Flyway migration; `YadaWeb` has no runtime database migration directory.

Logical-type validation does not detect a changed enum class or JSON target class within the same logical type. A wrong enum constant fails loudly, while two JSON classes with overlapping field names can deserialize into the wrong shape silently, so a key's JSON target class is part of its stored contract.

Public documentation is in `YadaDocs/src/docs/asciidoc/en/database/savedProperties.adoc`.

