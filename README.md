# EscapeHub — movement and integration demo

## Run locally

Install Docker Engine/Desktop with Compose v2, then run from this directory:

```sh
docker compose up --build
```

Open http://localhost:8080/login.html. The compose file activates **demo** mode.
Four local-only accounts: `player1`, `player2`, `player3`, `player4`; password for
all accounts: `demo-pass`. Do not expose this demo deployment publicly.
PostgreSQL is persisted in `postgres_data`. Stop with `docker compose down`.

## Four-player manual QA (not executed in the delivery environment)

1. Use four independent browser profiles/containers (ordinary tabs and incognito
   windows within the same browser mode can share login cookies).
2. Log each profile in with a different account. Confirm its name on Profile.
3. Continue to Lobby and click Ready on each client. Before all four are ready,
   the intro must remain hidden. All four should then see Intro.
4. Click Enter museum on each client. Four labelled circles should appear.
5. Move each player using arrow keys. Its movement must appear on the other three
   clients; movement remains inside the 800×500 canvas.
6. Close one client: its circle must disappear. Reload/rejoin it and confirm the
   current positions appear. A started room permits rejoining; the room resets
   when everyone leaves.
7. Try connecting the same user twice or a fifth user: connection is rejected.
8. Confirm a logged-out client cannot connect to `/ws/game`.

## Protocol and renderer integration

`gameSocket.js` exports `connectGame(callbacks)`, returning `ready()`, `move(x,y)`
and `close()`. Callback contracts:

- `onWelcome(id)` supplies the authenticated player ID.
- `onSnapshot(players)` replaces room state; each player has `id,x,y,ready`.
- `onMove({type,id,x,y})` updates a remote player.
- `onStart()` enables the intro. It can fire again; consumers must be idempotent.
- `onStatus(text)` reports errors/disconnection.

Client frames are `{"type":"ready"}` and `{"type":"move","x":120,"y":140}`.
Identity is taken from Spring Security's Principal, never a client-supplied ID.
The `/ws/game` endpoint is same-origin and requires an authenticated session.
Coordinates must be finite numbers inside x=12..788, y=12..488. The sample
renderer sends at most 20 movement frames per second. Server validation bounds
coordinates but does not implement authoritative physics, collision or speed checks.

This delivery provides one museum room, up to four distinct users, on one backend
instance. Room state is in memory. Multi-room matchmaking, shared state between
instances, reconnect backoff, and game objectives are outside this implementation.

## Integration boundary

The uploaded source had empty authentication, WebSocket, HTML, JS and deployment
files. This implementation therefore includes a **demo-only** Spring Security
configuration and a minimal renderer. The profile page displays authenticated
identity; it is not a completed persistent profile editor. The existing profile
REST controller still uses temporary ID 1 and must be connected to the team's User
entity before production. No claim is made that absent team code was integrated.

Replace demo authentication with the team's security configuration, preserving
an authenticated Principal on the WebSocket handshake. Connect the final renderer
through the callbacks above. The current Lobby → Intro → Museum stays within one
page to preserve the same connection. `museum.html` routes through this lobby.
The existing `apiClient.js` and `lobbySocket.js` remain placeholders and are unused.

## Build and CI

```sh
bash mvnw -B verify
```

CI runs build, JUnit tests, Checkstyle, Compose validation and image build on pushes
and pull requests. Tests use H2; PostgreSQL behavior needs the Docker QA above.
Checkstyle currently checks the auth/lobby/movement packages for wildcard imports,
empty statements and tabs. This is a limited initial ruleset, not a full audit.
The pipeline does not publish or deploy images; deployment credentials/target were
not supplied.

## Git collaboration and main protection

Create a feature branch, commit the changes, push it and open a PR into `main`.
The delivery environment could not access `riakourta/EscapeHub` (GitHub 404), so
nothing was pushed and repository settings were not changed.

A repository administrator must open Settings → Branches → Add branch protection
rule (or Rulesets), target `main`, require a pull request with at least one approval,
dismiss stale approvals, require conversation resolution, and require the
`build-test-analysis` check after its first successful run. Disable force pushes
and branch deletion. Confirm the rule applies to the intended contributors.

## Verification record

- JavaScript module syntax, frontend socket protocol assertions (callbacks, messages, closed socket), XML parsing and ZIP integrity: checked locally.
- `bash mvnw -B verify`: attempted, blocked before compilation by DNS/network access
  to `repo.maven.apache.org` (Spring Boot parent POM could not be downloaded).
- Docker build/Compose execution: not run; Docker unavailable here.
- Four-browser QA: not run; requires the running backend.
- GitHub Actions and branch protection: not executed/activated remotely.

Do not treat this package as a verified production release until the above build
and four-player checklist pass in your team's environment.
