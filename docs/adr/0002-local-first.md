# ADR 0002: Local-first

## Status
Accepted

## Context
TV browsing must feel immediate and external catalogue services may be slow or unavailable.

## Decision
Persist subscriptions, personal state and cached catalogue data locally. Render from local data and refresh in the background.

## Consequences
The app remains responsive and resilient, but cache freshness and invalidation become explicit concerns.