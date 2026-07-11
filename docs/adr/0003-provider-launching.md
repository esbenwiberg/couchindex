# ADR 0003: Provider launching

## Status
Accepted

## Context
Commercial streaming services control authentication, DRM and playback. CouchIndex cannot act as a universal player without provider agreements.

## Decision
CouchIndex resolves deep links or Android intents and launches the installed provider application.

## Consequences
Playback remains reliable and compliant. Some providers may only support title-level links, and the app switch is visible to the user.