# Play Data Safety Answers

This answer sheet reflects the Milestone 8 release artifact. Re-audit it whenever an
SDK, permission, network service or data flow changes. The Play account owner must
enter and attest to these answers in Play Console.

## Data collection and security

| Play Console question | Answer | Evidence |
| --- | --- | --- |
| Does the app collect or share any required user data types? | No | Personal state remains in app-private local storage and CouchIndex has no backend, analytics or advertising SDK. |
| Is all user data encrypted in transit? | Yes | The app's TMDb, IMDb and web handoff endpoints use HTTPS. |
| Does the app provide a way for users to request deletion? | Not applicable | CouchIndex does not create accounts or collect user data on a developer-operated server. Local data is deleted by clearing app storage or uninstalling. |
| Does the app support account creation? | No | CouchIndex has no account or sign-in flow. |
| Is the app independently security reviewed? | No | No independent review is claimed for V1. |

## Data-type review

Select no collected or shared data types. In particular:

- streaming-subscription selections, watchlist membership, launch history, watched
  state and Like or Dislike feedback remain local to the device
- search text is used locally to filter the downloaded catalogue and is not sent to a
  CouchIndex server
- TMDb and IMDb receive ordinary network metadata such as IP address when the app
  fetches public catalogue, artwork and rating resources; CouchIndex does not control
  or retain that service data
- provider apps, TMDb watch pages or Google Play receive a user-selected handoff only
  after the user activates the corresponding action

## Related declarations

- Contains ads: No
- App access restrictions: None
- Target audience: General audience; not designed specifically for children
- Category: Entertainment
- Privacy policy: https://esbenwiberg.github.io/couchindex/privacy/
- Support email: mint-apps.earthlike955@passmail.com
