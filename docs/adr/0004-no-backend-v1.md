# ADR 0004: No backend in V1

## Status
Accepted

## Context
The first product serves one user on one Google TV. A backend would add deployment, authentication and synchronization complexity without solving a current requirement.

## Decision
Keep V1 self-contained on the device.

## Consequences
Development and operation remain simple. Cross-device synchronization and remote management are postponed until a concrete need exists.