#!/usr/bin/env bash
# Genera un par de claves RSA para firmar los JWT en desarrollo local.
# Las claves quedan en config/claves/ (ignorado por git). En producción se montan como secretos.
set -euo pipefail

DIRECTORIO="$(cd "$(dirname "$0")/.." && pwd)/config/claves"
mkdir -p "$DIRECTORIO"

if [[ -f "$DIRECTORIO/privada.pem" ]]; then
  echo "Ya existen claves en $DIRECTORIO; no se sobrescriben."
  exit 0
fi

openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$DIRECTORIO/privada.pem"
openssl pkey -in "$DIRECTORIO/privada.pem" -pubout -out "$DIRECTORIO/publica.pem"
chmod 600 "$DIRECTORIO/privada.pem"
echo "Claves generadas en $DIRECTORIO"
