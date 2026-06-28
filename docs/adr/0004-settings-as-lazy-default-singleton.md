# Settings as a lazy-default singleton

Settings is the single, global configuration for the installation (Service Fee plus the Stringer's invoicing identity). Unlike every other aggregate — Customer, Racket, Reel — it has no `id` in its path, no list, and no create or delete. It is exposed as exactly two operations:

- `GET /settings` — always returns `200`. When no document has ever been saved, it returns **in-memory defaults** (Service Fee of zero, blank identity, `updatedAt = null`) without writing anything.
- `PUT /settings` — upserts a single document under a **fixed ID** (`"settings"`), guaranteeing structurally that only one row can exist, and returns the updated Settings.

`GET` is therefore a pure, side-effect-free read: the document exists in the database **if and only if** the Stringer has saved Settings at least once. The nullable `updatedAt` doubles as the "has this been configured yet?" signal.

## Alternatives considered

- **404 until first write.** `GET` returns `404` until a `POST`/`PUT` creates the row. Rejected: it forces the frontend to handle a "settings don't exist" state and a separate create flow for something that is conceptually always present.
- **Seed on startup.** A migration writes a default row at boot. Rejected: adds a startup/migration concern and still leaves `GET` and `PUT` as the only real operations — the lazy default achieves the same first-run experience with no seeding machinery.
- **Write the default on first `GET`.** Rejected: a read endpoint that silently writes is surprising, complicates testing, and means a bare `GET` (health check, frontend load) mutates state.

We chose lazy in-memory defaults so the frontend never sees a missing-settings state and the Stringer can configure incrementally (e.g. set the Service Fee on day one, add banking details later).

## Consequences

- There is no `CreateSettingsUseCase` and no `DeleteSettingsUseCase`. The service exposes only `GetSettingsUseCase` and `UpdateSettingsUseCase`.
- The default Settings is constructed in one place (the service) and returned whenever the repository finds no document. Callers cannot distinguish "defaults" from "saved" except via `updatedAt == null`.
- Reversing this (moving to id-based, create-required Settings) means introducing a create flow and migrating the fixed-ID document, which is why it is recorded here.
- The fixed-ID upsert is the single mechanism enforcing the singleton; no unique-index or count check is needed.
