// ---------------------------------------------------------------------------
// NimbusBank UI logic. Pure vanilla JS, no build step.
// Every network call goes through api() which reads baseUrl() from config.js,
// so switching "API source" in the header is all it takes to hit WireMock.
// ---------------------------------------------------------------------------

let token = null;
let accounts = [];

const $ = (id) => document.getElementById(id);

// --- tiny fetch helper -----------------------------------------------------
async function api(path, { method = "GET", body } = {}) {
  const res = await fetch(baseUrl() + path, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: "Bearer " + token } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) throw Object.assign(new Error(data?.message || res.statusText), { data, status: res.status });
  return data;
}

const money = (n) =>
  new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(n);

// --- environment switch ----------------------------------------------------
function initEnvSwitch() {
  const sel = $("env");
  sel.innerHTML = Object.entries(ENVIRONMENTS)
    .map(([k, v]) => `<option value="${k}">${v.label}</option>`)
    .join("");
  sel.value = currentEnv();
  paintEnv();
  sel.addEventListener("change", () => {
    setEnv(sel.value);
    paintEnv();
    // Force a fresh login so it's obvious which server answered.
    logout();
  });
}
function paintEnv() {
  const key = currentEnv();
  $("env-dot").className = "dot" + (key === "wiremock" ? " wiremock" : "");
  $("footer-env").textContent =
    "Talking to: " + ENVIRONMENTS[key].label + "  •  " + ENVIRONMENTS[key].baseUrl;
}

// --- views -----------------------------------------------------------------
function showLogin() {
  $("login-view").classList.remove("hidden");
  $("dashboard-view").classList.add("hidden");
  $("logout").classList.add("hidden");
}
function showDashboard() {
  $("login-view").classList.add("hidden");
  $("dashboard-view").classList.remove("hidden");
  $("logout").classList.remove("hidden");
}

// --- login -----------------------------------------------------------------
$("login-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  $("login-error").classList.add("hidden");
  try {
    const data = await api("/api/login", {
      method: "POST",
      body: { username: $("username").value, password: $("password").value },
    });
    token = data.token;
    showDashboard();
    await loadDashboard();
  } catch (err) {
    $("login-error").textContent = err.message || "Login failed";
    $("login-error").classList.remove("hidden");
  }
});

function logout() {
  token = null;
  accounts = [];
  showLogin();
}
$("logout").addEventListener("click", logout);

// --- dashboard data --------------------------------------------------------
async function loadDashboard() {
  const [customer, acctResp] = await Promise.all([
    api("/api/customer"),
    api("/api/accounts"),
  ]);

  $("cust-name").textContent = customer.name;
  $("cust-id").textContent = customer.customerId;
  $("cust-email").textContent = customer.email;
  $("cust-phone").textContent = customer.phone;
  $("cust-since").textContent = customer.memberSince;

  accounts = acctResp.accounts;
  renderAccounts();
  populateTransferSelects();
}

function renderAccounts() {
  $("accounts").innerHTML = accounts
    .map(
      (a) => `
      <div class="account" data-id="${a.id}">
        <div>
          <div class="type">${a.type}</div>
          <div class="num">${a.number}</div>
        </div>
        <div class="bal">${money(a.balance)}</div>
      </div>`
    )
    .join("");

  document.querySelectorAll(".account").forEach((el) =>
    el.addEventListener("click", () => loadTransactions(el.dataset.id, el))
  );
}

async function loadTransactions(accountId, el) {
  document.querySelectorAll(".account").forEach((a) => a.classList.remove("active"));
  if (el) el.classList.add("active");
  $("txn-account").textContent = "· " + accountId;
  $("transactions").innerHTML = `<p class="muted">Loading…</p>`;
  try {
    const { transactions } = await api(`/api/accounts/${accountId}/transactions`);
    $("transactions").innerHTML = transactions.length
      ? transactions
          .map(
            (t) => `
        <div class="txn">
          <div>
            <div>${t.description}</div>
            <div class="date">${t.date} · ${t.id}</div>
          </div>
          <div class="amt ${t.amount < 0 ? "debit" : "credit"}">${money(t.amount)}</div>
        </div>`
          )
          .join("")
      : `<p class="muted">No transactions.</p>`;
  } catch (err) {
    $("transactions").innerHTML = `<p class="error">${err.message}</p>`;
  }
}

// --- transfer --------------------------------------------------------------
function populateTransferSelects() {
  const opts = accounts
    .map((a) => `<option value="${a.id}">${a.type} ${a.number} (${a.id})</option>`)
    .join("");
  $("from-account").innerHTML = opts;
  $("to-account").innerHTML = opts;
  if (accounts.length > 1) $("to-account").selectedIndex = 1;
}

$("transfer-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const result = $("transfer-result");
  result.className = "result hidden";
  try {
    const data = await api("/api/transfer", {
      method: "POST",
      body: {
        fromAccountId: $("from-account").value,
        toAccountId: $("to-account").value,
        amount: parseFloat($("amount").value),
      },
    });
    result.textContent = `✔ ${data.status} · ref ${data.reference} · new balance ${money(data.newBalance)}`;
    result.className = "result ok";
    await loadDashboard(); // refresh balances
  } catch (err) {
    result.textContent = `✖ ${err.message}`;
    result.className = "result bad";
  }
});

// --- boot ------------------------------------------------------------------
initEnvSwitch();
showLogin();
