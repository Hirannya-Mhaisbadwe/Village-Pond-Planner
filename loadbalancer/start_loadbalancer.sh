#!/usr/bin/env bash
# ==============================================================================
# Script to Start Custom Go Load Balancer on stu6_sys1
# ==============================================================================

set -e

echo "=== Checking Go Installation ==="
if ! command -v go &> /dev/null; then
    echo "Installing Golang..."
    sudo apt-get update && sudo apt-get install -y golang-go
fi

echo "=== Compiling Go Load Balancer ==="
cd "$(dirname "$0")"
go build -o pond-lb *.go

echo "=== Starting Load Balancer ==="
# Port 3000 inside stu6_sys1 maps to external port 3221
./pond-lb -port=3000 -config=config.json
