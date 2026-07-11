# ADR 0006: Ratings remain separate

## Status
Accepted

## Context
IMDb, TMDb and Rotten Tomatoes use different populations, scales and meanings. A blended number would imply false precision.

## Decision
Store and display each rating independently with source, scale, scope and vote count where available.

## Consequences
The UI must accommodate missing and differently scaled ratings. Browse ranking may use an internal algorithm, but it must not be presented as an external consensus score.