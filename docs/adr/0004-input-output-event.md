# ADR 0004: Input / output / event separation

## Context

AdvantageKit’s core lesson is that logging outputs is not enough for replay. Students also need a story layer (events).

## Decision

Three categories: `INPUT`, `OUTPUT`, `EVENT`, plus accounting `DROP`. Simple `Trace.record` stores **OUTPUT** so beginners can graph without IO refactor. `recordInput` is explicit.

## Alternatives considered

* Outputs only — cannot replay.
* Inputs only — cannot teach “what we commanded.”
* Untyped maps — rejected; typed `TypedValue` is canonical.
* Hidden inference of category from names — too magical.

## Consequences

Early logs may mis-tag battery voltage as OUTPUT until students learn `recordInput`. Docs call this out.

## Student impact

Phase 0 checkpoint is exactly this vocabulary.

## Revisit conditions

If a beginner API with required category proves easier in classroom testing.
