# EscapeHub — team museum integration

Run `docker compose up --build`, then open http://localhost:8080/login.html.
Compose activates `integrated` mode and PostgreSQL. Register separate accounts,
log in, save a nickname/color, select an avatar in the team's lobby, then start
with exactly four authenticated players. All browsers navigate to the team's
museum renderer, with a short entry intro before enabling local controls.

Login retains the team's registration/BCrypt/JWT response. For this browser flow,
login also establishes an HttpOnly session, used by APIs and the two WebSockets.
Bearer-only JWT verification is not implemented. Mutating HTTP requests use CSRF
tokens obtained by apiClient.js. Tokens are not passed in WebSocket URLs.

`/ws/lobby` preserves the team's JOIN_LOBBY / START_HEIST protocol and UI. The
server derives account identity from the authenticated Principal. Avatar/name
selection is presentation data. The four-member roster survives navigation to
`museum.html`; `/ws/game` admits only those members. Both sockets are same-origin.

`playerMovement.js` retains the team's collision/movement logic. `gameRenderer.js`
retains the flashlight, exhibits, panic effects and alarm modal, and adds remote
players via an adapter. `museumNetwork.js` connects the adapter to `gameSocket.js`.
The server uses the same 800×600 room bounds and exhibit geometry, and triggers a
shared alarm when a member reaches the sword. Late arrivals receive alarm state.
This is destination collision validation, not authoritative speed/path simulation.

Room state is in memory on one backend instance. Duplicate account connections
and unadmitted players are rejected. A disconnected member disappears; rejoining
an active match through the lobby restores entry, with its initial spawn rather
than a saved position. After the last museum socket closes, the room resets.
Use four separate browser profiles/modes; tabs sharing login cookies are not four
independent accounts. Subsequent wings, puzzles and door transitions are not
implemented by this integration; the supplied team renderer contains one room.

## Checks

```
bash mvnw -B verify
node src/main/resources/static/UI_Tests.js
node --test scripts/frontend.test.cjs
```

CI runs these checks, Compose validation and image build. Tests use H2; the manual
Docker check uses PostgreSQL. Checkstyle is a limited initial ruleset scoped to
auth/lobby/movement. Deployment publishing and GitHub main protection require
separate repository configuration. Keep the integration PR in draft until reviewed.

## Manual four-player QA

1. Rebuild Compose. In four separate browser sessions register/log in as distinct
   accounts, save profiles, and open the lobby. Before four players, start must fail.
2. After the fourth joins, click START THE ROBBERY once. All four must reach the
   museum entry intro and display the team's flashlight room after entry.
3. Move each player; its avatar/name must update in every other client. Walls and
   exhibits must prevent ordinary local movement.
4. Approach the sword from below. All clients must receive the alarm effects and
   alarm modal. Click Continue on each client and verify movement resumes.
5. Close one museum window: its avatar must disappear. Rejoin through the lobby
   and confirm the alarm state persists while another player remains connected.
6. Close all museum windows, then return to the lobby to start a fresh game.

Verification in the delivery environment: six existing UI assertions and three
Node test groups passed (movement/collision, renderer adapter/shared alarm,
socket protocol). JS syntax, XML and archive structure checked. Java build was
not run to completion because Maven Central dependencies are unavailable here.
The new Java lobby/movement tests and Docker four-browser QA must pass locally.
Previous successful CI runs refer to the earlier integration, not this revision.
