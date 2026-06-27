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

**String Fee**: The material cost for the string used in a Job, configured per String in inventory.
_Avoid_: Material cost, string cost

### Inventory

**String**: A specific string product held in inventory. Each String has an availability toggle and a fee.
_Avoid_: Product, item, cord
