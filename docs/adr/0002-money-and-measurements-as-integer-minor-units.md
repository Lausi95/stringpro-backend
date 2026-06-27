# Store money and measurements as integer minor units

All monetary and measured quantities on the Reel aggregate are stored as exact integers in their smallest sensible unit, never as `Double` or `BigDecimal`: money in **euro cents** (`Long`), Gauge in **hundredths of a millimetre** (`Int`, e.g. `125` = 1.25 mm), and Reel Length / Approximate Meters per Job in **whole metres** (`Int`). DTOs convert to/from human-readable decimals at the API edge (`1299` ⇄ `12.99`, `125` ⇄ `1.25`). We chose integers because they are exact (no binary floating-point rounding, which matters once we sum reel costs for reporting), sort and compare cleanly in MongoDB, and keep the domain model free of `Double` foot-guns and `BigDecimal`/`Decimal128` scale-and-equality quirks.

This is the intended convention for every future quantity in the system (Service Fee, and any later prices or measurements), not just the Reel.

## Consequences

- Each money/measurement field needs an explicit unit-aware conversion in its Request/Response DTO mapping; the domain never sees the decimal form.
- Reversing this (e.g. switching to `BigDecimal`) later requires a data migration of stored Reel records, which is why it is recorded here.
