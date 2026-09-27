#!/usr/bin/env bash
# ==============================================================================
# Script to Deploy Spring Boot Backend Instance on stu6_sys2, stu6_sys3, or stu6_sys4
# ==============================================================================

set -e

# Target port for backend (internal port 4000 -> external port 4222 / 4223 / 4224)
export PORT=${PORT:-4000}

# Shared Database Connection pointing to stu6_sys1 on port 7000 (external 7221)
export DATABASE_URL=${DATABASE_URL:-"jdbc:postgresql://10.1.75.51:7221/pond_db?sslmode=disable"}
export DB_DRIVER="org.postgresql.Driver"
export DB_USERNAME="postgres"
export DB_PASSWORD="postgres123"

echo "======================================================================"
echo "🚀 Starting Pond Planning Application Backend Instance"
echo "   Listening Port : $PORT"
echo "   Database URL   : $DATABASE_URL"
echo "======================================================================"

# Check if Java 21 is available
if ! command -v java &> /dev/null; then
    echo "Installing OpenJDK 21..."
    sudo apt-get update && sudo apt-get install -y openjdk-21-jre-headless
fi

# Run application JAR
JAR_FILE=$(find target/ -name "Pond-Planning-Application-*.jar" 2>/dev/null | head -n 1)
if [ -z "$JAR_FILE" ] && [ -f "$HOME/app.jar" ]; then
    JAR_FILE="$HOME/app.jar"
fi

java -Xmx512m -jar "$JAR_FILE" \
    --server.port=$PORT \
    --spring.datasource.url="$DATABASE_URL" \
    --spring.datasource.driver-class-name="$DB_DRIVER" \
    --spring.datasource.username="$DB_USERNAME" \
    --spring.datasource.password="$DB_PASSWORD" \
    --spring.jpa.properties.hibernate.dialect="org.hibernate.dialect.PostgreSQLDialect" \
    --spring.jpa.hibernate.ddl-auto=update
