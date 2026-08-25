#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Spring Boot does not load a root .env file by itself. Export it only for this
# local process so OAuth, mail, JWT and other demo settings reach the backend.
if [[ -f "$ROOT_DIR/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT_DIR/.env"
  set +a
fi

cd "$ROOT_DIR/backend"
exec ./mvnw spring-boot:run -Dspring-boot.run.profiles=local "$@"
