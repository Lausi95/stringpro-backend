# StringPro Frontend

A single-page application for managing a tennis stringing business. The app is used by the Stringer to track Jobs from intake through payment.

## Language

### Core workflow

**Job**: The unit of work — a Racket brought in by a Customer to be strung. A Job moves through a fixed lifecycle and carries a price derived from Service Fee and String Fee.
_Avoid_: Order, request, ticket

**Stage**: The current lifecycle state of a Job. Progresses in one direction: Queued → In Progress → Ready → Done → Paid.
_Avoid_: Status, phase, step

**Stringer**: The person who operates the app and performs the stringing work. There is one Stringer per installation.
_Avoid_: User, operator, admin

### People and equipment

**Customer**: A person who brings Rackets in to be strung. A Customer may have multiple Rackets and multiple Jobs over time.
_Avoid_: Client, player

**Racket**: A tennis racket owned by a Customer and the physical subject of a Job. A Racket belongs to exactly one Customer and is always viewed in that Customer's context. A Customer may own several identical Rackets (a matched set).
_Avoid_: Equipment, item

**String Pattern**: The layout of a Racket's strings, expressed as mains × crosses (e.g. 16 × 19) — the count of vertical (main) strings and horizontal (cross) strings.
_Avoid_: String count, grid

**Head Size**: The area of a Racket's hitting surface, measured in square centimetres (cm²).
_Avoid_: Face size, racket size

### Pricing

**Service Fee**: The labor charge applied to a Job, configured in Settings.
_Avoid_: Labor cost, stringing fee

**String Fee**: The customer-facing material charge for the string used in a Job. Sourced from the String Reel selected for that Job.
_Avoid_: Material cost, string cost, Job Price

### Inventory

**String Reel**: A physical reel of string held in inventory — the inventory unit itself (there is no separate string-product catalog). Carries its own cost, length, and depletion lifecycle. Short form: **Reel**.
_Avoid_: String, product, item, cord, spool

**String**: _(retired)_ Previously meant a string-product catalog entry; superseded by **String Reel**, which is now the inventory unit.

**Reel State**: Where a String Reel is in its life: New → In Use → Used Up. Set manually by the Stringer; unlike a Job's Stage, it may move in any direction. A Used Up reel stays in the records (it cost money); deletion is reserved for mistaken entries.
_Avoid_: Stage (that is Job-only), status, phase

**Material**: What a String Reel is physically made of — one of Polyester, Natural Gut, Multifilament, Synthetic Gut. A reel is a single material; a two-string ("hybrid") setup is a Job-level combination of two Reels, never a material.
_Avoid_: Hybrid (as a material), composition
