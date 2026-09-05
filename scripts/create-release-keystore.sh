#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
local_dir="$repo_root/local"
keystore_file="$local_dir/booth-release.jks"
env_file="$local_dir/release-signing.env"
alias_name="booth"
set_github_secrets=false

usage() {
  cat <<'EOF'
Usage: scripts/create-release-keystore.sh [--set-github-secrets]

Creates a local Android release keystore for Booth in ./local, which is
ignored by git. Keep this keystore backed up securely.

Options:
  --set-github-secrets   Also upload signing values to GitHub repository secrets
                         using the authenticated gh CLI.
EOF
}

for arg in "$@"; do
  case "$arg" in
    --set-github-secrets) set_github_secrets=true ;;
    -h|--help) usage; exit 0 ;;
    *) usage >&2; exit 2 ;;
  esac
done

if [[ -f "$keystore_file" ]]; then
  echo "Refusing to overwrite existing keystore: $keystore_file" >&2
  exit 1
fi

command -v keytool >/dev/null || { echo "keytool is required." >&2; exit 1; }
command -v openssl >/dev/null || { echo "openssl is required." >&2; exit 1; }

if [[ "$set_github_secrets" == true ]]; then
  command -v gh >/dev/null || { echo "gh is required for --set-github-secrets." >&2; exit 1; }
  gh auth status >/dev/null
fi

mkdir -p "$local_dir"
chmod 700 "$local_dir"

keystore_password="$(openssl rand -base64 36)"
key_password="$keystore_password"

keytool -genkeypair -v \
  -keystore "$keystore_file" \
  -storepass "$keystore_password" \
  -keypass "$key_password" \
  -alias "$alias_name" \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -dname "CN=Booth,O=Shapeshed,C=GB"

chmod 600 "$keystore_file"
keystore_base64="$(base64 -w 0 "$keystore_file")"

cat > "$env_file" <<EOF
export BOOTH_KEYSTORE_FILE="$keystore_file"
export BOOTH_KEYSTORE_PASSWORD="$keystore_password"
export BOOTH_KEY_ALIAS="$alias_name"
export BOOTH_KEY_PASSWORD="$key_password"
export BOOTH_KEYSTORE_BASE64="$keystore_base64"
EOF
chmod 600 "$env_file"

if [[ "$set_github_secrets" == true ]]; then
  printf '%s' "$keystore_base64" | gh secret set BOOTH_KEYSTORE_BASE64
  printf '%s' "$keystore_password" | gh secret set BOOTH_KEYSTORE_PASSWORD
  printf '%s' "$alias_name" | gh secret set BOOTH_KEY_ALIAS
  printf '%s' "$key_password" | gh secret set BOOTH_KEY_PASSWORD
fi

cat <<EOF
Created release keystore:
  $keystore_file

Created local signing environment:
  $env_file

To test a local signed release build:
  source "$env_file"
  ./gradlew assembleRelease
EOF
