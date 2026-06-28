# Snapshot Service Fee and String Fee onto the Job

A Job's price is built from a **Service Fee** (sourced from Settings) and a **String Fee** per string side (sourced from the String Reel used). Rather than referencing those sources and computing the price live on every read, each Job **stores its own fee amounts at the moment they are set**, in integer minor units (see ADR-0002):

- `serviceFeeCents` — copied onto the Job (the client sends the value; it defaults from Settings in the UI).
- For each string side, a `stringFeeCents` — the Reel's String Fee for a Reel side (editable, e.g. for a discount), or `0` for an Own String side.

Total price is then `serviceFeeCents + mains.stringFeeCents + (crosses.stringFeeCents when hybrid)` — a pure function of values held on the Job, with no lookups into Settings or Reel.

## Alternatives considered

- **Derive fees live from Settings and the referenced Reel(s).** Store only references; compute the price on read. Rejected: a Job is an invoice-like record. Editing a Reel's String Fee, editing the global Service Fee, or soft-deleting a Reel would silently rewrite the price of every past Job. It also breaks for Own String sides (no Reel to read) and for any Job whose Reel has since been soft-deleted (the lookup fails).
- **Hybrid mid-ground: snapshot the String Fee but derive the Service Fee.** Rejected: inconsistent, and the Service Fee is just as mutable in Settings as the String Fee is on a Reel — the same drift problem applies.

## Consequences

- The Job aggregate carries fee amounts directly; `JobService` does **not** read Settings or Reel fees. It injects `CustomerRepository`, `RacketRepository`, and `ReelRepository` only to validate that referenced aggregates **exist** (and that the Racket belongs to the Customer) — never to read fee values.
- A Reel side still references its Reel by id (for traceability and the list-by-reel filter), but the price does not depend on that Reel remaining unchanged or even present.
- Because fees are decoupled from their sources, a Job can be edited and re-priced freely (the PUT carries the fee values), and a since-deleted Reel does not block editing a Job that referenced it.
- Reversing this (moving to live-derived pricing) would require backfilling references and accepting retroactive price drift, which is why it is recorded here.
