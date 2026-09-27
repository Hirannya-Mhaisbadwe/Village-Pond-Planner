#!/usr/bin/env bash
# ==============================================================================
# Script to Setup Shared PostgreSQL on stu6_sys1 (Port 4000 / 5432 -> External 4221)
# Run this inside stu6_sys1: ssh -p 2221 student@10.1.75.51
# ==============================================================================

set -e

echo "=== [1/4] Installing PostgreSQL ==="
sudo apt-get update
sudo apt-get install -y postgresql postgresql-contrib

echo "=== [2/4] Configuring PostgreSQL Database and User ==="
sudo -u postgres psql -c "ALTER USER postgres WITH PASSWORD 'postgres123';"
sudo -u postgres psql -c "CREATE DATABASE pond_db OWNER postgres;" || echo "Database pond_db already exists or created."

echo "=== [3/4] Enabling Remote Access and Port Binding ==="
# Configure PostgreSQL to listen on port 7000 (maps to external port 7221)
sudo sed -i "s/#listen_addresses = 'localhost'/listen_addresses = '*'/g" "$PG_CONF"
sudo sed -i "s/listen_addresses = 'localhost'/listen_addresses = '*'/g" "$PG_CONF"
sudo sed -i "s/port = 5432/port = 7000/g" "$PG_CONF"
sudo sed -i "s/#port = 5432/port = 7000/g" "$PG_CONF"

# Allow all network connections with md5/scram password
echo "host    all             all             0.0.0.0/0               md5" | sudo tee -a "$PG_HBA"
echo "host    all             all             ::/0                    md5" | sudo tee -a "$PG_HBA"

echo "=== [4/4] Restarting PostgreSQL ==="
sudo system-service postgresql restart || sudo service postgresql restart || sudo /etc/init.d/postgresql restart

echo "======================================================================"
echo "✅ Shared PostgreSQL is READY on stu6_sys1!"
echo "   Database Name : pond_db"
echo "   Username      : postgres"
echo "   Password      : postgres123"
echo "   Port (Internal/External): 7000 / 7221"
echo "   Connection URL: jdbc:postgresql://10.1.75.51:7221/pond_db"
echo "======================================================================"
