#!/bin/sh

set -euo pipefail

echo "\n📦 Deploying bookshop UI..."

kubectl apply -f resources

echo "⌛ Waiting for bookshop UI to be deployed..."

while [ $(kubectl get pod -l app=bookshop-ui | wc -l) -eq 0 ] ; do
  sleep 5
done

echo "\n⌛ Waiting for bookshop UI to be ready..."

kubectl wait \
  --for=condition=ready pod \
  --selector=app=bookshop-ui \
  --timeout=180s

echo "\n📦 bookshop UI deployment completed.\n"
