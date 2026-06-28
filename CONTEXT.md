# StringPro Frontend

A single-page application for managing a tennis stringing business. The app is used by the Stringer to track Jobs from intake through handback to the Customer. (Payment is tracked separately and is not part of the Job lifecycle — see **Stage**.)

## Language

### Core workflow

**Job**: The unit of work — a Racket brought in by a Customer to be strung. A Job moves through a fixed lifecycle and carries a price derived from Service Fee and String Fee.
_Avoid_: Order, request, ticket

**Stage**: The current lifecycle state of a Job. Progresses in one direction (forward only; it may skip ahead but never move back): Announced → Picked Up → In Progress → Done → Returned.
- **Announced**: the Job is recorded but the Racket is not yet in the Stringer's hands.
- **Picked Up**: the Stringer has received the Racket.
- **In Progress**: the Racket is being strung.
- **Done**: stringing is finished; the Racket is awaiting handback.
- **Returned**: the Racket has been handed back to the Customer. This is the final Stage.

This lifecycle is intake-to-handback and deliberately does **not** include payment — billing/payment is tracked separately and added later, which is why there is no "Paid" Stage. (Supersedes the earlier Queued → In Progress → Ready → Done → Paid wording.)
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

**String Fee**: The customer-facing material charge for the string used in a Job. Sourced from the String Reel selected for that Job. It is zero when the Customer brings their own string (no Reel is consumed). For a hybrid Job it is the sum across both strings (one charge per Reel used).
_Avoid_: Material cost, string cost, Job Price

**Own String**: String the Customer brings themselves rather than buying from the Stringer's inventory. It is identified only by a free-text name (it is not a Reel) and carries no String Fee. The alternative to taking string from a Reel.
_Avoid_: BYO, customer string

### Inventory

**String Reel**: A physical reel of string held in inventory — the inventory unit itself (there is no separate string-product catalog). Carries its own cost, length, and depletion lifecycle. Short form: **Reel**.
_Avoid_: String, product, item, cord, spool

**String**: _(retired)_ Previously meant a string-product catalog entry; superseded by **String Reel**, which is now the inventory unit.

**Reel State**: Where a String Reel is in its life: New → In Use → Used Up. Set manually by the Stringer; unlike a Job's Stage, it may move in any direction. A Used Up reel stays in the records (it cost money); deletion is reserved for mistaken entries.
_Avoid_: Stage (that is Job-only), status, phase

**Material**: What a String Reel is physically made of — one of Polyester, Natural Gut, Multifilament, Synthetic Gut. A reel is a single material; "hybrid" is never a material — it is a Job-level property (see **Hybrid**).
_Avoid_: Hybrid (as a material), composition

**Hybrid**: A Job whose mains and crosses use different strings, supplied separately. Each side (mains, crosses) is independently either a String Reel or the Customer's Own String — so a hybrid is any combination (Reel + Reel, Reel + Own, Own + Own). A non-hybrid Job uses one string for both sides. (Hybrid refers to the string source per side; tension is always recorded separately for mains and crosses regardless of whether the Job is hybrid.)
_Avoid_: Hybrid material, mixed

### Configuration

**Settings**: The single, global configuration for the installation. Holds the Service Fee applied to every Job, plus the Stringer's invoicing identity — full name, email, IBAN, and address. Exactly one Settings exists per installation; it is never created or deleted, only read and updated. Before the Stringer has saved anything, Settings reads back as defaults (Service Fee of zero, blank identity).
_Avoid_: Config, preferences, profile
