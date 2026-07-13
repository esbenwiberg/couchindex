# Play Store Listing

This copy is ready for the English-language Android TV internal-testing listing.
Review it again against the final artifact before broader release.

## Listing fields

**App name**

CouchIndex

**Short description**

Browse your streaming subscriptions in one calm, personal TV index.

**Support email**

mint-apps.earthlike955@passmail.com

**Full description**

CouchIndex brings the movies and series available across your streaming subscriptions
into one focused Google TV catalogue.

Choose the services you use, browse their combined Danish availability, search across
providers, keep a local watchlist, and return to recently launched titles. TMDb and
IMDb ratings remain clearly separated, while private Like and Dislike feedback shapes
recommendations directly on your TV.

CouchIndex does not stream or proxy video. When you choose a title, playback is handed
to the relevant provider app or its watch page. Personal state stays on the device;
there is no CouchIndex account, advertising or analytics SDK.

Availability data is supplied by JustWatch through TMDb. This product uses the TMDb API
but is not endorsed or certified by TMDb.

## Classification draft

- Category: Entertainment
- Contains ads: No
- App access: No CouchIndex account or restricted sign-in
- Target audience: General audience; not designed specifically for children
- Pricing: Free for the initial internal test

The final content-rating questionnaire and target-audience declaration must be answered
by the Play account owner from the shipped behavior.

## TV media

Generate the screenshot set with:

```sh
./scripts/capture-tv-store-assets.sh
```

Suggested order and alt text:

1. `01-home.png`: CouchIndex Home showing Continue Watching, My Watchlist and title details.
2. `02-browse.png`: Combined streaming catalogue with provider labels and separate ratings.
3. `03-search.png`: Search results for Interstellar across configured subscriptions.
4. `04-settings.png`: Subscription selection and catalogue integration status.

Prepared graphics under `design/play-store/`:

- `couchindex_icon_512.png`: 512x512 Play listing icon with alpha
- `couchindex_feature_1024x500.png`: required feature graphic
- `couchindex_tv_banner_1280x720.png`: required Play Android TV banner

Regenerate all listing graphics with `scripts/render-play-assets.sh`.

The in-app launcher separately uses the required xhdpi 320x180 banner at
`app/src/main/res/drawable-xhdpi/couchindex_banner.png`.

Suggested feature-graphic alt text: CouchIndex branding between two abstract title
cards with the phrase Your services. One index.

## Owner-supplied fields

- Privacy-policy URL: https://esbenwiberg.github.io/couchindex/privacy/
- Developer website, if used
- Release countries and languages

Use the field-by-field declaration in [play-data-safety.md](play-data-safety.md) when
completing the Play Data safety form.
