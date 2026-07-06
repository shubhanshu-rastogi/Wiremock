#!/usr/bin/env bash
# Start the REAL Spring Boot backend on http://localhost:8080
set -e
cd "$(dirname "$0")/backend"
if [ ! -f target/bank.jar ]; then
  echo "Building backend (first run)…"
  mvn -q -DskipTests package
fi
echo "Real backend  ->  http://localhost:8080"
java -jar target/bank.jar
