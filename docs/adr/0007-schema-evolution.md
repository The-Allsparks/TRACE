# ADR 0007: Schema evolution

## Context

Logs must remain readable across TRACE upgrades or a converter must exist.

## Decision

Integer `schemaVersion` on every record and metadata. TLOG magic+version in the header. Unknown versions fail the reader. Fields do not change meaning; we add or bump.

No Java serialization.

## Alternatives considered

* JSON-only logs — easier evolution, worse size/CPU.
* Protobuf — extra dependency for students.
* Unversioned CSV — silent meaning changes.

## Consequences

Phase 5 converters can target a specific schema version.

## Student impact

Invisible if we do it right; visible as “unsupported log version” if they mix tools.

## Revisit conditions

Need for optional extra payload maps after classroom use.
