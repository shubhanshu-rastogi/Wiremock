#!/usr/bin/env bash
# Start WireMock on http://localhost:8081, serving the stubs in ./wiremock
set -e
cd "$(dirname "$0")/wiremock"

# The standalone jar is ~18MB and not committed to git — fetch it on first run.
WIREMOCK_VERSION="3.9.2"
if [ ! -f wiremock-standalone.jar ]; then
  echo "Downloading WireMock ${WIREMOCK_VERSION} (first run)…"
  curl -sSL -o wiremock-standalone.jar \
    "https://repo1.maven.org/maven2/org/wiremock/wiremock-standalone/${WIREMOCK_VERSION}/wiremock-standalone-${WIREMOCK_VERSION}.jar"
fi

echo "WireMock      ->  http://localhost:8081   (admin: http://localhost:8081/__admin)"
java -jar wiremock-standalone.jar \
  --port 8081 \
  --root-dir . \
  --enable-stub-cors \
  --verbose
