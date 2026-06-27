# Store Racket Head Size in square centimetres (cm²)

A Racket's Head Size is stored and validated as an integer number of square centimetres (cm²), with bounds `400–900`, even though the tennis industry universally measures head size in square inches (in²) — e.g. a "100 in²" frame. We chose cm² deliberately because the Stringer works in metric; storing the native unit avoids lossy conversion on every read and write and keeps stored values matching what the Stringer reads off the frame.

## Consequences

- Any UI or integration expecting in² must convert (1 in² ≈ 6.4516 cm²; e.g. 100 in² ≈ 645 cm²).
- Reversing this decision later requires a data migration of existing Racket records plus a constraint change, which is why it is recorded here.
