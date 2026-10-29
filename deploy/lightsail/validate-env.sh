#!/usr/bin/env bash
# Validate the server-side .env before starting the stack.
set -euo pipefail

cd "$(dirname "$0")"

if [[ ! -f .env ]]; then
  echo "Missing .env. Copy env.example to .env and fill it in." >&2
  exit 1
fi

read_value() {
  sed -n "s/^$1=//p" .env | tail -n 1
}

required=(APP_JWT_SECRET POSTGRES_DB POSTGRES_USER POSTGRES_PASSWORD API_DOMAIN FIREBASE_PROJECT_ID FIREBASE_SERVICE_ACCOUNT_JSON)
for key in "${required[@]}"; do
  if [[ -z "$(read_value "$key")" ]]; then
    echo "Missing required value: $key" >&2
    exit 1
  fi
done

jwt_secret="$(read_value APP_JWT_SECRET)"
db_password="$(read_value POSTGRES_PASSWORD)"
api_domain="$(read_value API_DOMAIN)"
db_user="$(read_value POSTGRES_USER)"

if [[ ${#jwt_secret} -lt 32 || "$jwt_secret" == *"replace"* || "$jwt_secret" == *"change"* ]]; then
  echo "APP_JWT_SECRET must be a fresh random value of at least 32 characters." >&2
  exit 1
fi

if [[ ${#db_password} -lt 24 || "$db_password" == *"replace"* ]]; then
  echo "POSTGRES_PASSWORD must be a fresh random value of at least 24 characters." >&2
  exit 1
fi

if [[ ! "$db_password" =~ ^[A-Za-z0-9._~-]+$ ]]; then
  echo "POSTGRES_PASSWORD must be URL-safe (letters, digits, . _ ~ -)." >&2
  exit 1
fi

if [[ ! "$db_user" =~ ^[A-Za-z0-9_]+$ ]]; then
  echo "POSTGRES_USER must contain only letters, digits, or underscores." >&2
  exit 1
fi

if [[ "$api_domain" == "api.example.com" || "$api_domain" == *"://"* || "$api_domain" == *"/"* ]]; then
  echo "API_DOMAIN must be a bare hostname, for example api.203.0.113.10.sslip.io" >&2
  exit 1
fi

if [[ -z "$(read_value HF_API_TOKEN)" && -z "$(read_value GROQ_API_KEY)" ]]; then
  echo "Set at least one of HF_API_TOKEN or GROQ_API_KEY." >&2
  exit 1
fi

if [[ "$(stat -c '%a' .env)" != "600" ]]; then
  echo "Warning: .env permissions are $(stat -c '%a' .env); run: chmod 600 .env" >&2
fi

docker compose config -q
echo "Environment looks valid. API_DOMAIN=${api_domain}"
