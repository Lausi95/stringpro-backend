# Paginate global list endpoints; allow plain lists for bounded sub-collections

List endpoints over **global, unbounded** collections — those that grow without limit over the life of the installation (Customers, String Reels) — are paginated using the shared `PageResult<T>` wrapper: `page`/`size` query params (defaults `0`/`20`, no enforced maximum), and a deterministic default sort so page boundaries are stable. List endpoints over **naturally-bounded, per-parent** sub-collections — those always scoped to a single owner and small by construction (a Customer's Rackets, fetched via `?customerId`) — may return a plain `List<T>`. We split on bounded-vs-unbounded rather than paginating everything because pagination over a handful of a Customer's Rackets is pure ceremony, while an unbounded inventory or customer base will eventually return payloads large enough to matter.

The decision rule for any future list endpoint: **global/unbounded → paginate; per-parent bounded → plain list is acceptable.**

## Consequences

- A paginated endpoint must apply a stable, deterministic sort (e.g. Reels sort by `createdAt` descending). Relying on MongoDB natural order makes page boundaries undefined — rows can be skipped or duplicated across pages.
- Paginated responses are a `{ content, totalElements, totalPages, page, size }` object, not a bare JSON array. Converting an existing plain-list endpoint to paginated is therefore a breaking contract change for its clients.
- No maximum `size` is enforced, matching the Customer endpoint; a client can still request an unbounded page if it insists. Revisit if that becomes a problem.
