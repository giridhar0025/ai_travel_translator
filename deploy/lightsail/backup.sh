#!/usr/bin/env bash
# Create a compressed PostgreSQL backup and prune old local copies.
# Intended for cron:
#   15 3 * * * cd /opt/travel-translator/deploy/lightsail && ./backup.sh >> backups/backup.log 2>&1
set -euo pipefail

cd "$(dirname "$0")"
umask 077

BACKUP_DIR="${BACKUP_DIR:-backups}"
BACKUP_RETAIN_DAYS="${BACKUP_RETAIN_DAYS:-7}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
TARGET="${BACKUP_DIR}/translator-${STAMP}.dump"

mkdir -p "$BACKUP_DIR"

echo "[$(date -u +%FT%TZ)] starting backup -> ${TARGET}"
docker compose exec -T db sh -c \
  'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom --no-owner --no-privileges' \
  >"$TARGET"

if [[ ! -s "$TARGET" ]]; then
  echo "Backup is empty; refusing to keep it." >&2
  rm -f "$TARGET"
  exit 1
fi

sha256sum "$TARGET" >"${TARGET}.sha256"
find "$BACKUP_DIR" -type f \( -name '*.dump' -o -name '*.dump.sha256' \) \
  -mtime "+${BACKUP_RETAIN_DAYS}" -delete

echo "[$(date -u +%FT%TZ)] backup complete: $(du -h "$TARGET" | cut -f1)"
