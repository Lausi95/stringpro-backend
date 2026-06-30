# Deployment Design — stringpro-backend

Continuous deployment of stringpro-backend to a single VServer, built and shipped
by **GitHub Actions**, deployed by **Ansible**, run as a **Docker Compose** stack
behind an existing **Traefik** reverse proxy.

This document is the agreed design. It records *what* we build and *why*; the
implementation (workflow, playbook, compose, templates) follows from it.

---

## 1. Target environment

| Property | Value |
|---|---|
| Server | VServer reachable at `lausi95.net` |
| Reverse proxy | Traefik, already running, terminates SSL (Let's Encrypt) |
| Deploy user | `tom` (SSH `lausi95.net:22`, default port) |
| App directory | `~/stringpro-backend` (i.e. `/home/tom/stringpro-backend`) |
| Public app URL | `https://stringpro-backend.lausi95.net` |
| Co-tenant | A frontend will run on the same box (shared Traefik, shared Docker) |
| GitHub repo | `github.com/lausi95/stringpro-backend` (**public**) |

Traefik conventions on this server (confirmed against the running instance):

- External Docker network Traefik watches: **`traefik`**
- HTTPS entrypoint: **`websecure`**
- Cert resolver: **`letsencrypt`**
- Provider: **Docker label provider** (services route via container labels)

The server is assumed already provisioned: Docker is installed (Traefik runs in
it) and the `traefik` network exists. The pipeline does **not** provision the box.

---

## 2. Architecture overview

```
 git push main ──► GitHub Actions
                   ├─ verify        ./gradlew ktlintCheck test         (hard gate)
                   ├─ build-and-push  Docker build ─► GHCR              (needs: verify)
                   │                  ghcr.io/lausi95/stringpro-backend:<sha> + :latest
                   └─ deploy         ansible-playbook over SSH ─► tom@lausi95.net
                                     (needs: build-and-push)
                                       ├─ template .env  (IMAGE_TAG=<sha>, secrets)
                                       ├─ copy docker-compose.prod.yml + init-mongo.js
                                       ├─ docker compose pull
                                       ├─ docker compose up -d --remove-orphans
                                       ├─ probe /actuator/health until 200  (deploy gate)
                                       └─ docker image prune -f  (dangling only)

 On the server: Docker Compose stack
   ┌─ app (stringpro-backend)  ── joined to: traefik (external) + internal
   │     labels route stringpro-backend.lausi95.net ─► :8080
   └─ mongodb                  ── joined to: internal ONLY (never exposed)
         named volume mongo-data
```

**Separation of concerns:** GitHub Actions **builds**; Ansible **deploys**. The
build produces an immutable image in GHCR; the deploy renders config and rolls
the stack. The server never compiles source.

---

## 3. Decisions

Each decision below was deliberately chosen; alternatives considered are noted.

### 3.1 Image flow — registry pull from GHCR

GHA builds the image and pushes it to **GHCR**
(`ghcr.io/lausi95/stringpro-backend`); the server pulls the prebuilt image.

- *Chosen because:* the existing multi-stage Dockerfile (distroless, layer-split,
  `type=gha` cache) is purpose-built for this. The server stays free of
  JDK/Gradle build load.
- *Rejected:* building from source on the VServer (Ansible ships source, runs
  `docker compose build`) — wastes server resources and duplicates the toolchain.

> **Step zero — make the GHCR package public.** A public *repo* does **not** make
> its GHCR *package* public. The first push creates a **private** package, and
> anonymous `docker compose pull` on the server will fail until the package is
> flipped to **public** in its GitHub package settings (one-time, manual). The
> push job must also declare `permissions: packages: write`.

### 3.2 Image tagging — SHA-pinned, immutable

Every build pushes **two tags**: `:<git-sha>` (immutable) and `:latest`
(convenience). The compose file references
`ghcr.io/lausi95/stringpro-backend:${IMAGE_TAG}`, and Ansible writes
`IMAGE_TAG=<sha>` into the server's `.env` for each deploy.

- *Chosen because:* deploys are immutable and reproducible; rollback is
  re-deploying an older SHA. Zero extra cost — GHA already knows the SHA.
- *Rejected:* floating `:latest` only — mutable, un-pinnable, rollback by guess.

### 3.3 Trigger — continuous deployment on `main`, manual rollback

- **Push to `main`** → `verify → build-and-push → deploy` runs automatically.
- **`workflow_dispatch`** with an optional `image_tag` input → manual
  redeploy/rollback to any SHA without a code change.
- A GHA **concurrency group** (`cancel-in-progress: false`) serializes deploys so
  two pushes can't deploy at once: a second deploy **queues** behind the in-flight
  one rather than cancelling it (we never want to kill a half-applied deploy).

- *Rejected:* tag/release-gated deploys — unnecessary ceremony for a
  solo-operated service.

### 3.4 Quality gate — `ktlintCheck test` before anything ships

A **`verify`** job runs `./gradlew ktlintCheck test` first; `build-and-push` and
`deploy` declare `needs: verify`. Red tests or lint abort before GHCR or the
server are touched.

- The **Docker build stays test-free** (it already skips tests/ktlint) — the
  gate runs once, in `verify`, with no duplication.
- *Chosen because:* with straight CD on `main`, an ungated pipeline ships broken
  commits to prod. Matches `CLAUDE.md` (ktlint is the gate, tests are strategy).

### 3.5 Ansible execution — GitHub-hosted runner over SSH

The `deploy` job runs on a **GitHub-hosted Ubuntu runner**, installs Ansible, and
runs `ansible-playbook` against `lausi95.net` over SSH as `tom` using an existing
deploy keypair (private key stored as the `SSH_PRIVATE_KEY` Actions secret; public
key already in `tom`'s `authorized_keys`). Host-key checking is pinned via
`SSH_KNOWN_HOSTS` (or `accept-new`).

- *Chosen because:* no agent to maintain on the box; inbound SSH only.
- *Rejected:* self-hosted runner on the VServer — overkill for one service.

### 3.6 Ansible scope — deploy-only, idempotent

The playbook does **not** provision. It:

1. Ensures `~/stringpro-backend` exists.
2. Asserts the external `traefik` network is present (fail fast otherwise).
3. Templates `.env` (mode `0600`, owner `tom`) from extra-vars.
4. Copies the **static** `docker-compose.prod.yml` and `init-mongo.js`.
5. `docker compose pull`.
6. `docker compose up -d --remove-orphans`.
7. Probes health (see 3.9), failing the run if unhealthy.
8. `docker image prune -f` — **dangling only**.

> **`prune -f`, never `prune -a`.** The frontend shares this box. `docker image
> prune -a` removes any image not tied to a *running* container and would delete
> the frontend's images whenever it is momentarily down. Dangling-only is safe.

- *File delivery:* the prod compose carries no secrets (image ref, labels,
  networks, volume) so it is a **static copied file**; only `.env` is templated.

### 3.7 MongoDB — co-located in the prod stack

Mongo runs as a service in the same compose stack, on an **internal-only**
network (never joined to `traefik`, never publishing `27017` to the host), with a
named volume **`mongo-data`** for persistence across container recreation.

- *Chosen because:* single-server, single-tenant — no managed-DB cost/complexity.
- *Rejected:* external/managed Mongo (Atlas) — unnecessary here.

The **prod compose is a separate file** (`docker-compose.prod.yml`) from the
existing root `docker-compose.yml`, which stays as the **local-dev** convenience
(unchanged, with its `root/example` placeholder creds).

### 3.8 Mongo auth — dedicated least-privilege app user

The Mongo container's root user lives in the `admin` database; Spring
authenticates against the database in the URI (`stringpro`). To avoid surprising
`authSource` semantics and to follow least privilege:

- An **`init-mongo.js`** (mounted into the container, runs **once on first volume
  init**) creates a `stringpro` user scoped to the `stringpro` DB with
  `readWrite`.
- The app authenticates as that user directly against `stringpro`.
- The root account is reserved for admin.

- *Rejected:* app uses the root user with `?authSource=admin` — simpler, but the
  app would run as DB admin.

> **Caveat:** `init-mongo.js` runs **only on first volume initialization**. If
> the app password changes later, the init script will not re-run against an
> existing `mongo-data` volume — the user must be updated manually (or the volume
> reset). Noted for future credential rotation.

### 3.9 Health verification — Actuator, probed from outside the container

Spring Boot **Actuator** is added; `/actuator/health` is the readiness signal.

- Exposure is scoped to health only:
  `management.endpoints.web.exposure.include=health`, so the `permitAll` does not
  publish the full actuator surface.
- `SecurityConfig` permits `/actuator/health` unauthenticated.

**The probe runs outside the app container**, because the distroless runtime
(`gcr.io/distroless/java21-debian12`) has **no shell and no curl** — a Docker
`HEALTHCHECK CMD` has nothing to execute inside it. Therefore:

- **Ansible post-deploy probe (the deploy gate):** after `up -d`, curl
  `https://stringpro-backend.lausi95.net/actuator/health` until `200` with a timeout;
  fail the GHA run if it never becomes healthy.
- **Traefik healthcheck label (optional, ongoing):**
  `loadbalancer.healthcheck.path=/actuator/health` — Traefik probes from
  outside, needs nothing in the image.
- **Mongo container healthcheck (startup ordering):** the `mongo` image *has* a
  shell, so it keeps a real `healthcheck`, and the app declares
  `depends_on: mongodb: condition: service_healthy`.

- *Rejected:* in-container Docker `HEALTHCHECK` for the app — impossible on
  distroless. Swagger-URL smoke test — works but Actuator is the standard signal.

### 3.10 Rollout — brief downtime accepted

Single app container; `docker compose up -d` stops the old and starts the new —
a sub-minute window where Traefik returns 502/503 during JVM boot.

- *Chosen because:* solo-operated service, not HA. Keeps compose trivial.
- *Rejected:* blue-green / rolling replicas — real complexity (duplicate
  services, Traefik weighting) for no current benefit. Revisit if it ever matters.

### 3.11 Traefik routing — labels on the app service

The app joins the external `traefik` network, publishes **no host port**, and
routes via labels (router/service named **`stringpro-backend`**, since the
frontend coexists on the box):

```yaml
labels:
  - "traefik.enable=true"
  - "traefik.http.routers.stringpro-backend.rule=Host(`stringpro-backend.lausi95.net`)"
  - "traefik.http.routers.stringpro-backend.entrypoints=websecure"
  - "traefik.http.routers.stringpro-backend.tls.certresolver=letsencrypt"
  - "traefik.http.services.stringpro-backend.loadbalancer.server.port=8080"
  # optional ongoing health (see 3.9):
  - "traefik.http.services.stringpro-backend.loadbalancer.healthcheck.path=/actuator/health"
```

The `traefik` network is declared `external: true` in the prod compose.

---

## 4. Secrets and configuration

### GitHub Actions Secrets (repo settings)

| Secret | Purpose |
|---|---|
| `SSH_PRIVATE_KEY` | Deploy key for `tom` (Ansible SSH) |
| `SSH_KNOWN_HOSTS` | Pinned host key for `lausi95.net` (or use `accept-new`) |
| `MONGO_ROOT_PASSWORD` | Mongo admin (`admin` DB) password |
| `MONGO_APP_PASSWORD` | `stringpro` app DB user password |

Secrets flow as `ansible-playbook` extra-vars → rendered into `.env` (`0600`) on
the server. Non-secret values (Keycloak issuer, Traefik names) live in the static
compose / `application.yml`, not in secrets.

### Server `.env` (rendered by Ansible, `0600`, owner `tom`)

```
IMAGE_TAG=<git-sha>
MONGO_ROOT_USERNAME=root
MONGO_ROOT_PASSWORD=<from secret>
MONGO_APP_USERNAME=stringpro
MONGO_APP_PASSWORD=<from secret>
```

The app's `MONGODB_URI` is **assembled in the compose file** from
`MONGO_APP_USERNAME`/`MONGO_APP_PASSWORD`
(`mongodb://<user>:<pass>@mongodb:27017/stringpro`) — credentials are embedded in
the URI because Spring's `spring.data.mongodb.uri` takes full precedence over the
discrete `username`/`password` properties. The Keycloak issuer stays hardcoded in
`application.yml` as today.

> **Config fix (landed with this work):** `application.yml` previously bound Mongo
> under `spring.mongodb.*` — a non-existent prefix Spring Boot ignores, so the
> `MONGODB_*` env vars never took effect and the app silently fell back to
> `mongodb://localhost`. Corrected to `spring.data.mongodb.uri` with embedded
> credentials. Without this, the prod app could not authenticate to Mongo and the
> health gate (§3.9) would fail every deploy.

---

## 5. Repository layout (new files)

```
.github/workflows/
  deploy.yml                 # verify → build-and-push → deploy; CD on main + workflow_dispatch
ansible/
  inventory.ini              # single host: lausi95.net, user tom, port 22
  deploy.yml                 # deploy-only playbook
  templates/
    env.j2                   # renders .env (IMAGE_TAG, Mongo creds) → 0600
  files/
    docker-compose.prod.yml  # app + mongo; traefik labels; external+internal nets; named volume
    init-mongo.js            # creates least-priv stringpro app user (first init only)
```

Unchanged: root `docker-compose.yml` remains the **local-dev** file.

---

## 6. Application-code changes required

These are not pure ops — they touch the app and must land before/with the pipeline:

1. **Add Spring Boot Actuator** dependency.
2. `application.yml`: `management.endpoints.web.exposure.include=health`.
3. `SecurityConfig`: `permitAll` for `/actuator/health` (alongside existing
   Swagger public paths).

---

## 7. Out of scope (deferred)

- **MongoDB backups.** The `mongo-data` volume survives deploys but is *not* a
  backup. A `mongodump`-to-off-box cron (or scheduled GHA job) is the immediate
  next task after CD works — explicitly deferred from this round.
- **Zero-downtime / blue-green rollouts** (see 3.10).
- **Server provisioning** (Docker install, Traefik setup) — assumed present.

---

## 8. First-deploy checklist (one-time)

0. **Point DNS:** create an `A` record for `stringpro-backend.lausi95.net` → the VServer's
   IP. Until this resolves, Traefik can't obtain a Let's Encrypt cert, and the
   health gate (`https://...` with `validate_certs: true`) fails on cert
   validation — which looks like an app failure but isn't. Do this first.
1. Push the repo to `github.com/lausi95/stringpro-backend` (public).
2. Add the four Actions secrets (§4).
3. Run the pipeline once → first image lands in GHCR as a **private** package.
4. **Flip the GHCR package to public** in its package settings (§3.1).
5. Confirm the `traefik` network exists on the server and `tom`'s
   `authorized_keys` contains the deploy public key.
6. Re-run the deploy → server pulls anonymously, stack comes up, health probe
   passes, `stringpro-backend.lausi95.net` serves over HTTPS.
