#!/bin/bash

# Base directory (dynamically correctly resolves to the path where this script lives, regardless of where user executes it)
BASE_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

# Array of services in suggested startup order
SERVICES=(
    "discovery-server"
    "api-gateway"
    "auth-service"
    "user_service"
    "wallet_service"
    "market_service"
    "order_service"
    "portfolio_service"
    "notification-service"
)

echo "🚀 Launching all microservices in separate Terminal windows from: $BASE_DIR"

for SERVICE in "${SERVICES[@]}"; do
    if [ -d "$BASE_DIR/$SERVICE" ]; then
        echo "Starting $SERVICE..."
        # Uses osascript to tell the macOS Terminal application to open a new window and run the command
        osascript -e "tell application \"Terminal\" to do script \"cd \\\"$BASE_DIR/$SERVICE\\\" && mvn clean compile spring-boot:run\""
        
        # Adding a 2 second delay between launches so CPU/Memory isn't hammered completely at once
        sleep 2
    else
        echo "⚠️  Directory $SERVICE not found in $BASE_DIR! Skipping..."
    fi
done

echo "✅ All available services have been launched!"
echo "If you need to restart one, just go to its specific Terminal window, hit Ctrl+C, and run 'mvn spring-boot:run'."
