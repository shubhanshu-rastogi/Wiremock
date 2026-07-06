# NimbusBank — a hands-on WireMock lab

A small but realistic banking app built specifically to **learn WireMock**.

You get the *same* UI running against two interchangeable backends:

```
                 ┌────────────────────────────┐
 Browser UI ───► │  REAL backend  (Spring Boot)│  http://localhost:8080
 (:5500)    │    └────────────────────────────┘
            │    ┌────────────────────────────┐
            └──► │  WireMock  (JSON stubs)     │  http://localhost:8081
                 └────────────────────────────┘
```

The UI never changes. You flip **one dropdown** in the header ("API source") and the
whole app runs against WireMock instead of the real server. That swap *is* the lesson:
WireMock impersonates a real API so precisely the frontend can't tell the difference.

---

## What's inside

```
WireMockBanking/
├── backend/            Spring Boot REST API  (the "real" bank)  → :8080
│   └── src/main/java/com/learn/bank/
│       ├── BankApplication.java        app entry point
│       ├── CorsConfig.java             lets the browser call the API
│       ├── controller/BankController.java   the 5 endpoints
│       ├── service/BankService.java    in-memory data + transfer logic
│       └── model/Dtos.java             request/response shapes
├── frontend/           Static UI (vanilla HTML/CSS/JS, no build step) → :5500
│   ├── index.html
│   ├── app.js          all logic; every call goes through baseUrl()
│   └── config.js       ← the ONE place that picks real vs WireMock
├── wiremock/           WireMock standalone + your stubs               → :8081
│   ├── wiremock-standalone.jar
│   ├── mappings/       one JSON file per stubbed request
│   └── __files/        response bodies referenced by the mappings
└── start-*.sh          convenience scripts
```

## The API (identical on both servers)

| Method | Path                              | Purpose                    |
|--------|-----------------------------------|----------------------------|
| POST   | `/api/login`                      | authenticate → token       |
| GET    | `/api/customer`                   | profile                    |
| GET    | `/api/accounts`                   | account list + balances    |
| GET    | `/api/accounts/{id}/transactions` | statement for one account  |
| POST   | `/api/transfer`                   | move money                 |

Demo login: **adesh / password123**

---

## Run it (3 terminals)

```bash
# 1) the real backend  (http://localhost:8080)
./start-backend.sh

# 2) WireMock          (http://localhost:8081)
./start-wiremock.sh

# 3) the UI            (http://localhost:5500)
./start-ui.sh
```

Then open <http://localhost:5500>, log in, and use the **"API source"** dropdown in
the header to switch between the real backend and WireMock. Watch the footer — it
tells you which server is answering.

> Prerequisites (already installed on this machine): Java 17, Maven, Python 3.
> The backend jar builds automatically on first `./start-backend.sh`.

### See the difference with your own eyes
1. Log in against **Real backend**, do a transfer → balances change and persist.
2. Switch to **WireMock**, log in again → balances are back to the pristine stub
   values, and every transfer returns the *same* canned success. That's because
   WireMock serves static data — it has no database, no logic. Understanding that
   boundary is most of what "learning WireMock" means.

---

## How the stubs map to WireMock concepts

Open each file next to this table — every mapping demonstrates one idea.

| File (`wiremock/mappings/`)   | WireMock concept it teaches |
|-------------------------------|-----------------------------|
| `01-login-success.json`       | **Request body matching** with `matchesJsonPath` — only `adesh`/`password123` matches. Plus `priority` (lower number wins). |
| `02-login-failure.json`       | A lower-priority **catch-all** for the same URL → returns 401 for any other body. Shows how WireMock picks the most specific match. |
| `03-customer.json`            | **Header matching** — requires `Authorization: Bearer …` via a regex (`matches`). |
| `04-accounts.json`            | **`bodyFileName`** — response body lives in `__files/accounts.json` instead of inline. |
| `05/06-transactions-*.json`   | **URL path matching** for different accounts, each returning its own file. |
| `07-transfer.json`            | **Response templating** — `reference` uses `{{randomValue}}`, so every call returns a different transaction id (dynamic responses). |

Anatomy of a stub — request matcher on top, canned response below:

```jsonc
{
  "priority": 1,                       // lower = preferred when several match
  "request": {
    "method": "POST",
    "urlPath": "/api/login",
    "bodyPatterns": [                   // ALL patterns must match
      { "matchesJsonPath": "$[?(@.username == 'adesh')]" },
      { "matchesJsonPath": "$[?(@.password == 'password123')]" }
    ]
  },
  "response": {
    "status": 200,
    "headers": { "Content-Type": "application/json" },
    "jsonBody": { "token": "…", "customerId": "CUST-1001", "name": "Adesh Sharma" }
  }
}
```

---

## Exercises — change a stub, re-hit the UI, see it change

WireMock re-reads `mappings/` on the fly for file edits, but to be safe you can
force a reload any time with:

```bash
curl -X POST http://localhost:8081/__admin/mappings/reset
```

1. **Break the balance.** Edit `__files/accounts.json`, change a balance, reload the
   accounts page in the UI (on WireMock). Your fake number shows up. This is why
   mocks are great for UI edge cases (huge numbers, negative balances, empty lists).

2. **Simulate a slow network.** Add a delay to `04-accounts.json`'s response:
   ```json
   "response": { "status": 200, "fixedDelayMilliseconds": 3000, "bodyFileName": "accounts.json" }
   ```
   Reload — the accounts card now takes 3s. Great for testing spinners/timeouts.

3. **Simulate a server crash.** Make `/api/customer` fail:
   ```json
   "response": { "status": 500, "jsonBody": { "error": "BOOM", "message": "backend on fire" } }
   ```
   Watch the UI handle the error. (Try `"fault": "CONNECTION_RESET_BY_PEER"` too.)

4. **Stateful mocking (Scenarios).** Make the transfer *change* the balance on the
   second call using WireMock scenarios (`scenarioName` + `requiredScenarioState` +
   `newScenarioState`). This is how you mock a stateful flow without real logic.

5. **Verify calls were made.** WireMock records every request. After clicking around:
   ```bash
   curl "http://localhost:8081/__admin/requests" | less        # full journal
   # count how many times /api/accounts was called:
   curl -X POST http://localhost:8081/__admin/requests/count \
        -H 'Content-Type: application/json' \
        -d '{ "method": "GET", "urlPath": "/api/accounts" }'
   ```

6. **Create a stub over the admin API** (no file, no restart):
   ```bash
   curl -X POST http://localhost:8081/__admin/mappings \
     -H 'Content-Type: application/json' \
     -d '{ "request": { "method": "GET", "urlPath": "/api/ping" },
           "response": { "status": 200, "jsonBody": { "pong": true } } }'
   curl http://localhost:8081/api/ping
   ```

7. **Record & playback.** Point WireMock at the real backend and let it generate
   stubs from real traffic:
   ```bash
   # start a recording proxy, then browse the UI against it
   java -jar wiremock/wiremock-standalone.jar --port 8082 --enable-stub-cors \
        --proxy-all http://localhost:8080 --record-mappings
   ```
   Every request it proxies gets saved as a mapping — instant stubs from a real API.

---

## Useful WireMock admin endpoints

Everything under `http://localhost:8081/__admin`:

| Endpoint                          | What it does                          |
|-----------------------------------|---------------------------------------|
| `GET  /__admin/mappings`          | list all loaded stubs                 |
| `POST /__admin/mappings/reset`    | reload stubs from disk                 |
| `GET  /__admin/requests`          | the request journal (what was called) |
| `POST /__admin/requests/count`    | count matching requests               |
| `DELETE /__admin/requests`        | clear the journal                     |

Run with `--verbose` (the script already does) to see match/no-match decisions in
the WireMock terminal — invaluable when a stub isn't matching.

---

## Troubleshooting

- **UI shows a network error / CORS**: make sure you opened the UI via
  `http://localhost:5500` (not by double-clicking the file), and that the target
  server is running. WireMock CORS comes from the `--enable-stub-cors` flag.
- **WireMock returns 404**: no stub matched. Check the `--verbose` log; usually a
  header or body pattern didn't match. Remember stubs require the `Authorization`
  header — the UI sends it automatically after login.
- **Port already in use**: `lsof -ti tcp:8081 | xargs kill` (swap the port number).
- **Rebuild the backend**: `cd backend && mvn -DskipTests package`.

Happy mocking.
