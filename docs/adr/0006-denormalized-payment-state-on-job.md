# Denormalized payment state cached on the Job

Payments are their own aggregate (`payments` collection) referencing a Job by `jobId`, but each Job also carries a cached `amountPaidCents` (running total of its Payments) and a derived `fullyPaid` boolean (`amountPaidCents >= totalCents`). We cache this on the Job so the common reads — "is this Job paid?" and the "Jobs not fully paid" worklist filter — are single indexed queries that never touch the Payment collection.

## Considered Options

- **Compute on read** (sum Payments every time a Job is shown): always correct, no cache, but every Job list/detail view fans out into the Payment collection. Rejected — the whole point was to save those queries, and the unpaid filter would need a per-Job aggregation.
- **Embed Payments inside the Job document**: perfect consistency in one write, but breaks "filter Payments by Customer across all Jobs" and the one-aggregate-per-package convention. Rejected.
- **Cache on the Job** (chosen): one indexed read for paid-state, at the cost of keeping the cache honest.

## Consequences

- **Recompute-from-source, not incremental.** On every Payment create/delete the Payment service re-sums *all* non-deleted Payments for the `jobId` and writes `amountPaidCents` + `fullyPaid` onto the Job via `Job.withAmountPaid(...)`. Re-summing (rather than `+=`/`-=`) means any prior drift self-heals on the next Payment.
- **The invariant lives in the Job model**, not the services: `fullyPaid` is only ever derived (helper `withAmountPaid` plus an `init` `require`), so the two writers (Payment create/delete, Job update) cannot diverge.
- **Two cross-aggregate couplings, both one-directional:** Payment service → Job (read `totalCents`, write the cache) on Payment changes; Job service → Payment (cascade soft-delete) on Job deletion. A Job edit that changes `totalCents` re-derives `fullyPaid` from the Job's own stored `amountPaidCents` — no Payment read needed.
- **No multi-document transaction.** The Mongo deployment is a single node (no replica set), so Payment-write and Job-recompute are two separate writes. We order them Payment-first: a crash in between leaves a recorded Payment with a momentarily-stale Job total (corrected by the next recompute), never a paid-looking Job with no Payment behind it. For a single-Stringer app, concurrent Payments against one Job are effectively impossible, so the race window is accepted rather than locked.
