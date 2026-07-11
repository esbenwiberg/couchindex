# ADR 0005: TMDb as canonical catalogue identity

## Status
Accepted

## Context
CouchIndex must correlate metadata, provider availability and ratings from multiple sources.

## Decision
Use TMDb movie and TV identifiers as the canonical catalogue identity in V1. Store external identifiers such as IMDb IDs alongside them.

## Consequences
TMDb integration becomes foundational. Mappings must remain explicit so the canonical source can be reconsidered later without losing external identities.