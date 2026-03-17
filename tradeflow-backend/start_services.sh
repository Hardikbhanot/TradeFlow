#!/bin/bash

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
ENV_FILE="$BASE_DIR/.env"

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

service_port() {
    case "$1" in
        discovery-server) echo 8761 ;;
        api-gateway) echo 8080 ;;
        auth-service) echo 9000 ;;
        user_service) echo 8081 ;;
        wallet_service) echo 8082 ;;
        market_service) echo 8085 ;;
        order_service) echo 8083 ;;
        portfolio_service) echo 8084 ;;
        notification-service) echo 8086 ;;
        *) echo "" ;;
    esac
}

resolve_runner() {
    local service_dir="$1"
    if [ -x "$service_dir/mvnw" ]; then
        echo "./mvnw -q spring-boot:run"
    else
        echo "mvn -q spring-boot:run"
    fi
}

escape_osascript_string() {
    local s="$1"
    s="${s//\\/\\\\}"
    s="${s//\"/\\\"}"
    printf '%s' "$s"
}

echo "Launching all microservices in separate Terminal windows from: $BASE_DIR"

for SERVICE in "${SERVICES[@]}"; do
    SERVICE_DIR="$BASE_DIR/$SERVICE"
    PORT="$(service_port "$SERVICE")"

    if [ ! -d "$SERVICE_DIR" ]; then
        echo "Directory $SERVICE not found in $BASE_DIR. Skipping."
        continue
    fi

    if [ -n "$PORT" ]; then
        EXISTING_PID="$(lsof -t -iTCP:"$PORT" -sTCP:LISTEN 2>/dev/null | head -n 1)"
        if [ -n "$EXISTING_PID" ]; then
            echo "Skipping $SERVICE: port $PORT is already in use by PID $EXISTING_PID."
            continue
        fi
    fi

    RUNNER_CMD="$(resolve_runner "$SERVICE_DIR")"
    LAUNCH_CMD="cd \"$SERVICE_DIR\" || exit 1; set -o allexport; [ -f \"$ENV_FILE\" ] && source \"$ENV_FILE\"; set +o allexport; $RUNNER_CMD"
    ESCAPED_LAUNCH_CMD="$(escape_osascript_string "$LAUNCH_CMD")"

    echo "Starting $SERVICE on port ${PORT:-unknown}..."
    osascript -e "tell application \"Terminal\" to do script \"$ESCAPED_LAUNCH_CMD\""
    if [ "$SERVICE" = "discovery-server" ]; then
        sleep 8
    else
        sleep 2
    fi
done

echo "Startup dispatch complete."
echo "If a service was skipped, it was already running on its configured port."
