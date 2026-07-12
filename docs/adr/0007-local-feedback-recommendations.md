# ADR 0007: Local feedback recommendations

## Status

Accepted

## Context

CouchIndex needs personal recommendations without blending ratings, uploading viewing state or requiring another account. TMDb discover results include stable genre identifiers, and CouchIndex already owns local title feedback state.

## Decision

Store explicit Like and Dislike feedback locally. Recommend only titles available from enabled subscriptions. Rank candidates by overlap with genres from liked titles, apply a stronger penalty for overlap with disliked titles, and exclude titles that already have feedback.

Recommendations are labeled `For You`; they are not represented as TMDb, IMDb or provider recommendations.

## Consequences

- Recommendations work offline after catalogue refresh and require no additional API requests.
- Feedback remains on the TV and can be cleared by selecting the active choice again.
- The algorithm is deterministic and explainable, but intentionally modest until richer explicit feedback exists.
- Genre IDs are retained on the canonical title model.
