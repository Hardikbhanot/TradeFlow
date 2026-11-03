# TradeFlow — Backend Microservices Architecture

The backend of **TradeFlow** is composed of 9 decoupled Spring Boot 3 microservices running on **Java 21**, orchestrated by **Spring Cloud Gateway** and **Netflix Eureka Discovery Server**, with **Apache Kafka (KRaft)** handling asynchronous event-driven transactions and **Redis** providing sub-millisecond caching.

---

## 🏛️ Microservice Inventory

```
tradeflow-backend/
├── discovery-server/      # Port 8761: Netflix Eureka service registry
├── api-gateway/           # Port 8080: Spring Cloud Gateway with JWT filters
├── auth-service/          # Port 9000: User credentials, BCrypt, JWT, 2FA OTP
├── user_service/          # Port 8081: User accounts & wallet provisioning
├── wallet_service/        # Port 8082: Double-entry ledger, balance holds, locking
├── portfolio_service/     # Port 8083: Holdings, watchlist, Gemini AI summaries
├── order_service/         # Port 8084: Matching engine, OrderExecutionWorker
├── market_service/        # Port 8085: Upstox quotes, L2 Market Depth, WebSockets
├── notification-service/  # Port 8086: SMTP2GO / SendGrid async email dispatch
└── docker-compose.deploy.yml # Production multi-container orchestrator
```

---

## 📡 Kafka Event Topics & Choreographed Sagas

TradeFlow implements the **Choreographed Saga Pattern** to ensure strict data consistency across independent microservices without distributed transactions:

| Topic Name | Producer | Consumer(s) | Description |
| :--- | :--- | :--- | :--- |
| `order-created-topic` | `order-service` | `wallet-service` | Published when an order is placed. Triggers wallet balance reservation. |
| `funds-reserved-topic` | `wallet-service` | `order-service` | Emitted when funds are safely locked. Submits order to matching worker. |
| `funds-rejected-topic` | `wallet-service` | `order-service` | Emitted on insufficient funds. Marks order as `FAILED`. |
| `order-completed-topic`| `order-service` | `portfolio-service` | Emitted when a trade fills. Updates user stock holdings. |
| `portfolio-update-failed-topic` | `portfolio-service` | `order-service` | Triggered on holding save failures. Initiates saga balance refund. |
| `wallet-balance-update-topic` | `order-service` | `wallet-service` | Used for sell proceeds credits or failed order saga rollbacks. |
| `notification-topic` | `order-service` | `notification-service` | Publishes trade fill notifications to deliver email receipts. |
| `otp-topic` | `auth-service` | `notification-service` | Dispatches 2FA login & sell confirmation codes. |
| `ledger-transaction-topic` | `wallet-service` | `notification-service` | Dispatches deposit & withdrawal receipts. |

---

## ⚡ Redis Caching Schema

The backend uses a shared Redis 8 instance (`redis://redis:6379`) on `app-net`:

| Key Pattern | TTL | Managed By | Purpose |
| :--- | :--- | :--- | :--- |
| `price:{SYMBOL}` | 5 sec | `market-service` | High-frequency live stock quote caching. |
| `price:mock:{SYMBOL}` | 5 sec | `market-service` | Ticker simulation cache when off-market hours. |
| `market:instruments` | 24 hrs | `market-service` | Upstox symbol-to-instrument key mappings. |
| `market:equity_symbols` | 24 hrs | `market-service` | Fast autocomplete search symbol lookup list. |
| `upstox:access_token` | Dynamic | `auth-service` / `market` | Active OAuth token for Upstox v2 API calls. |
| `jwt_blacklist:{TOKEN}`| Token TTL | `api-gateway` / `auth` | Revoked JWT tokens upon user logout. |

---

## ⚙️ Environment Configuration

Refer to [`tradeflow-backend/.env.example`](.env.example) for all configurable environment variables:

```env
# Database Credentials
AUTH_DB_URL=jdbc:postgresql://postgres:5432/tradeflow
AUTH_DB_USERNAME=postgres
AUTH_DB_PASSWORD=your_password

# JWT Security
JWT_SECRET=your_32_character_minimum_high_entropy_secret_key

# Notification (SMTP2GO & SendGrid)
SMTP_HOST=smtp.smtp2go.com
SMTP_PORT=2525
SMTP_USERNAME=tradeflow-smtp
SMTP_PASSWORD=your_smtp_password
SMTP_FROM=no-reply@hbhanot.tech
SENDGRID_API_KEY=your_optional_fallback_key

# Upstox & AI
UPSTOX_API_KEY=your_key
UPSTOX_API_SECRET=your_secret
GEMINI_API_KEY=your_google_ai_key

# Zerodha Kite Connect (optional until Kite is configured)
KITE_API_KEY=your_kite_api_key
KITE_API_SECRET=your_kite_api_secret
KITE_REDIRECT_URI=https://api.tradeflow.hbhanot.tech/auth/kite/callback
KITE_FRONTEND_REDIRECT_URL=https://tradeflow.hbhanot.tech/dashboard
```

### Kite Connect live quotes

The backend can authenticate a Kite Connect session and stream live NSE equity quotes
for dashboard symbols and the Market page's watchlist/popular symbols. The Market page
registers its symbols with `POST /api/v1/market/subscriptions`; both pages receive
incremental ticks over the `/topic/market-data` WebSocket topic without periodic quote
polling. Set `KITE_API_KEY` and `KITE_API_SECRET` in
the production `.env`, then register this exact redirect URL in the Kite developer
portal: `https://api.tradeflow.hbhanot.tech/auth/kite/callback`. The dashboard's
**Connect Zerodha Kite** action starts the login flow. Kite access tokens are
session-based and expire at 6:00 AM IST the next day, so reconnect after expiry.
Zerodha lists the Personal tier as free; the Connect tier required for live
WebSocket streaming and historical candles is ₹500/month.

This adds live quote streaming only; order placement is not routed to Kite by this
integration.

The NSE instrument master is loaded on market-service startup and refreshed daily
at 08:35 IST. Search scans all symbol-prefix matches before sorting and limiting
results, so newly listed symbols are not hidden by Redis scan ordering.

---

## 🛠️ Build and Testing

```bash
# Compile and package all 9 services
mvn clean package -DskipTests

# Run tests on specific service (e.g. order matching tests)
mvn test -pl order_service
```

---

## 🐳 Docker Deployment

The multi-container deployment connects all services to the external Docker bridge network `app-net`:

```bash
# Pull latest images from GitHub Container Registry
docker compose -f docker-compose.deploy.yml pull

# Start full backend stack in background
docker compose -f docker-compose.deploy.yml up -d --remove-orphans
```
