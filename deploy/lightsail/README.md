# Travel Translator API on AWS Lightsail

Server-only migration: the FastAPI API and its PostgreSQL database run on one
Lightsail instance. The Android/iOS apps, Firebase Auth, and AI providers stay
unchanged.

## Architecture

```text
Mobile app
    |
    v
Caddy :443 (automatic TLS)
    |
    v
FastAPI container :8000
    |
    v
PostgreSQL 16 container :5432 (private Docker network, no host port)
```

Only ports **22, 80, and 443** are open. PostgreSQL and port 8000 are never
published to the internet.

## Instance

- Ubuntu 24.04
- Lightsail **Small-2GB** (`$12/month`) to start
- Resize to **Medium-4GB** (`$24/month`) if Docker builds run out of memory
- Static IPv4 attached to the instance

The 2 GB plan is enough to run the stack; a 2 GB swap file is created by
`bootstrap.sh` so the image can be built on the instance.

## 1. Create the instance

In the Lightsail console:

1. Create an **Ubuntu 24.04** instance in your chosen region.
2. Choose the **2 GB / 2 vCPU** plan.
3. Create or select an SSH key pair and download the private key.
4. Attach a **static IP**.
5. Under **Networking**, allow SSH (22), HTTP (80), and HTTPS (443).

Keep the static IP. It is needed for DNS and TLS.

## 2. Point DNS at the instance

Use a real domain you control:

```text
api.yourdomain.com  A  <static-ip>
```

For a temporary staging hostname without buying a domain, use sslip.io:

```text
api.<static-ip>.sslip.io  A  <static-ip>
```

Caddy will obtain a Let's Encrypt certificate automatically after DNS resolves
and ports 80/443 are open.

## 3. Bootstrap the server

```bash
ssh -i /path/to/lightsail-key.pem ubuntu@<static-ip>
```

Copy `bootstrap.sh` to the server, then run it:

```bash
bash bootstrap.sh
```

Log out and back in so Docker group membership takes effect. Verify:

```bash
docker version
docker compose version
```

## 4. Copy the project

From this Mac, copy the repository to the server. The API image is built from
`backend/`, and the Compose stack is in `deploy/lightsail/`.

```bash
rsync -av --exclude '.git' --exclude 'backend/.env' --exclude 'backend/.env.bak*' \
  "/Users/pulicharagiridhar/Travel Translator AI UI Design/" \
  ubuntu@<static-ip>:/opt/travel-translator/
```

Do not copy the local `backend/.env`; production secrets go in
`deploy/lightsail/.env` on the server.

## 5. Create the server environment

On the server:

```bash
cd /opt/travel-translator/deploy/lightsail
cp env.example .env
chmod 600 .env
```

Generate values:

```bash
openssl rand -hex 32   # APP_JWT_SECRET
openssl rand -hex 24   # POSTGRES_PASSWORD
```

Fill in `.env` with:

- `APP_JWT_SECRET` — new value, not the old Railway or development value
- `POSTGRES_PASSWORD` — new URL-safe value
- `FIREBASE_PROJECT_ID`
- `FIREBASE_SERVICE_ACCOUNT_JSON` — base64-encoded service-account JSON
- `HF_API_TOKEN` and/or `GROQ_API_KEY`
- `API_DOMAIN` — the hostname from step 2

Validate without printing secrets:

```bash
./validate-env.sh
```

## 6. Start the stack

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f api
```

The API container runs:

```text
alembic upgrade head
python -m scripts.seed_phrases
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

Check health:

```bash
curl https://<API_DOMAIN>/health
# {"status":"ok"}
```

## 7. Migrate existing data from Railway

Keep Railway running until the new API passes smoke tests.

Create a custom-format dump from the Railway database:

```bash
docker run --rm -e DATABASE_URL="<railway-public-database-url>" postgres:16-alpine \
  sh -c 'pg_dump "$DATABASE_URL" --format=custom --no-owner --no-privileges' \
  > railway.dump
```

Copy it to the server:

```bash
scp -i /path/to/lightsail-key.pem railway.dump \
  ubuntu@<static-ip>:/opt/travel-translator/deploy/lightsail/
```

On the server, restore before exposing the new API:

```bash
cd /opt/travel-translator/deploy/lightsail
docker compose stop api caddy
docker compose up -d db
docker compose exec -T db pg_restore \
  -U translator -d translator --no-owner --no-privileges < railway.dump
docker compose up -d
```

Verify row counts on both sides for at least `users`, `languages`,
`phrases`, `conversations`, `messages`, `favorites`, and `recents`.

## 8. Smoke test

```bash
API="https://<API_DOMAIN>"

curl -s "$API/health"
JWT=$(curl -s -X POST "$API/api/v1/auth/guest" | jq -r .token)
curl -s -X POST "$API/api/v1/translate" \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"source":"en","target":"hi","text":"Where is the hospital?","context":"travel"}'
```

Also verify streaming:

```bash
curl -N -X POST "$API/api/v1/translate/stream" \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"source":"en","target":"hi","text":"Where is the hospital?","context":"travel"}'
```

And audio upload/TTS using the existing API tests or the mobile app.

## 9. Point the mobile app at AWS

Android reads `BACKEND_BASE_URL` from `android/local.properties` and embeds it
in `BuildConfig`. Update it to:

```properties
BACKEND_BASE_URL=https://<API_DOMAIN>
```

Then rebuild and release the app.

Existing installed apps still call the old Railway URL. Keep Railway alive
until adoption is sufficient, or put a stable custom domain in front of the
API so future cutovers are DNS changes instead of app releases.

## Backups

Create a nightly local dump:

```bash
chmod +x backup.sh
mkdir -p backups
./backup.sh
```

Install a cron entry:

```cron
15 3 * * * cd /opt/travel-translator/deploy/lightsail && ./backup.sh >> backups/backup.log 2>&1
```

Also enable **automatic snapshots** on the Lightsail instance. Local dumps do
not protect against losing the instance itself.

Restore a dump:

```bash
docker compose exec -T db pg_restore \
  -U translator -d translator --clean --if-exists --no-owner --no-privileges \
  < backups/translator-YYYYMMDDTHHMMSSZ.dump
```

## Updating the API

```bash
cd /opt/travel-translator
git pull
cd deploy/lightsail
docker compose up -d --build
docker compose ps
```

If a deploy fails, the previous container image is still cached locally. Roll
back the Git commit and rebuild, or restore the previous image tag.

## Cost

| Item | Approximate monthly cost |
|---|---:|
| Lightsail 2 GB instance | $12 |
| Static IP while attached | included |
| Local backups | included in disk |
| Lightsail snapshots | ~$0.05/GB-month |
| AI providers (Groq/HF/Replicate) | separate, not AWS credit |

At $12/month, $100 gives roughly eight months of compute before snapshots and
data transfer. App Runner at 1 vCPU / 2 GB would be closer to $50/month, so it
is not the right choice for this budget.

## Security notes

- `.env` must stay at mode `600` and must never be committed.
- The old JWT secret in `backend/DEPLOY.md` was committed to Git. Rotate
  `APP_JWT_SECRET` now and treat the old value as compromised.
- Rotate any API keys that were ever committed or shared outside the local
  `.env`.
- PostgreSQL and uvicorn are only reachable on the private Docker network.
- Restrict SSH to your IP in the Lightsail firewall if possible.
