# TradeFlow

TradeFlow is a professional, high-performance trading platform built with a modern **Microservices Architecture**. It enables users to track live markets, manage a virtual portfolio, and execute trades in a secure, event-driven environment.

## 🚀 Key Features

- **Microservices-Based Backend**: 9 decoupled services for maximum scalability and fault tolerance.
- **2FA Security (OTP)**: Secure login flow using One-Time Passwords sent via email, powered by an asynchronous **Kafka** event pipeline.
- **Live Market Data**: Real-time intraday price tracking and dynamic charts for top Indian stocks (NSE/BSE).
- **Wallet & Ledger**: Robust virtual wallet with a transaction ledger for tracking buys, sells, and balance history.
- **Smart Watchlist**: Persistent user-specific watchlists with instant "Buy" shortcuts.
- **Intelligent Polling**: Automated market hours detection (IST) to optimize data fetching and reduce API/resource load.
- **Premium UI/UX**: Stunning dark/light mode interface with sleek animations and responsive design.

## 🏗️ Architecture Overiew

### Backend Services
The backend is built using **Spring Boot** and **Spring Cloud**, orchestrated with an **API Gateway** and **Discovery Server**.

- **API Gateway**: Central entry point for all client requests; handles JWT validation.
- **Discovery Server (Eureka)**: Service registry for dynamic service lookup.
- **Auth Service**: Manages users, JWT generation, and OTP state.
- **Market Service**: Aggregates and serves live price data for symbols.
- **Order Service**: Processes buy/sell orders and manages trade lifecycle.
- **Wallet Service**: Handles user balances and maintains a transaction ledger.
- **Portfolio Service**: Tracks user holdings and performance metrics.
- **Notification Service**: Asynchronously handles communications (like OTP emails) via Kafka.
- **User Service**: Core user profile management.

### Tech Stack
- **Frontend**: React.js, Recharts, Axios, CSS Variables (Design Tokens).
- **Backend**: Java 21, Spring Boot 3, Spring Security, Spring Cloud.
- **Data & Messaging**: PostgreSQL, Apache Kafka.
- **Build Tools**: Maven, npm.

## 🛠️ Getting Started

### Prerequisites
- **Java 21** or later.
- **Node.js** (v18+) & **npm**.
- **Apache Kafka** (running locally on `localhost:9092`).
- **PostgreSQL** (configured in service properties).

### Setup & Installation

1. **Clone the Repository**
   ```bash
   git clone https://github.com/Hardikbhanot/TradeFlow.git
   cd TradeFlow
   ```

2. **Configure Environment**
   - Each backend service uses an `.env` or `application.properties` for database and Kafka credentials.
   - Refer to `.env.example` in `tradeflow-backend` for required variables.

3. **Run the Backend**
   - Start the **Discovery Server** first.
   - Run each service using Maven:
     ```bash
     mvn spring-boot:run
     ```
   - (Optional) Use the provided `start_services.sh` script to boot all services sequentially.

4. **Run the Frontend**
   ```bash
   cd tradeflow-frontend
   npm install
   npm run dev
   ```

## 🔐 Security Flow (OTP)
1. User submits credentials to `/auth/login`.
2. Auth Service validates and saves a temporary OTP, then publishes an `OtpRequestedEvent` to Kafka.
3. Notification Service consumes the event and sends an email via Thymeleaf templates.
4. User submits the OTP to `/auth/verify-otp`.
5. Success returns a **JWT**, which is used for subsequent authenticated requests.

## ⏱️ Market Rules
The platform respects **IST (India Standard Time)** market hours (09:15 - 15:30). Data polling and graph updates are intelligently paused during off-hours to conserve bandwidth and system resources.

---
*Developed by Hardik Bhanot*
