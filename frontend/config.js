// The ONE place that decides which server the UI talks to.
//
// This is the heart of the WireMock lesson: the app code never changes.
// You only flip the environment, and the same UI runs against either the
// real Spring Boot backend (:8080) or WireMock (:8081).
const ENVIRONMENTS = {
  real: {
    label: "Real backend (Spring Boot :8080)",
    baseUrl: "http://localhost:8080",
  },
  wiremock: {
    label: "WireMock (:8081)",
    baseUrl: "http://localhost:8081",
  },
};

// Remember the last choice across page reloads.
function currentEnv() {
  return localStorage.getItem("bank.env") || "real";
}
function setEnv(key) {
  localStorage.setItem("bank.env", key);
}
function baseUrl() {
  return ENVIRONMENTS[currentEnv()].baseUrl;
}
