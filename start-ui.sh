#!/usr/bin/env bash
# Serve the UI on http://localhost:5500 (needs http:// so browser CORS works)
cd "$(dirname "$0")/frontend"
echo "UI            ->  http://localhost:5500"
python3 -m http.server 5500
