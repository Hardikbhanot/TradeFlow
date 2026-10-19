# TradeFlow — Frontend Client

A high-performance, responsive React 18 trading interface designed with the **Luxe Titanium** design system, featuring real-time market data, Level 2 depth ladders, interactive charts, and double-entry ledger tracking.

---

## 🎨 Features & Interface

- **Luxe Titanium Theme**: Deep slate glassmorphic background with emerald green and rose red trading indicators. Supports seamless toggle between dark and light themes.
- **Level 2 Market Depth Ladder**: 5-level Bid/Ask order book displaying live volumes, order counts, and dynamic buyer vs. seller imbalance bars with click-to-autofill price capability.
- **Interactive Trading Charts**: High-resolution historical and intraday performance charts for top Indian stocks (NSE/BSE).
- **Execution Terminal**: Streamlined order placement ticket supporting Market, Limit, Stop-Loss, Stop-Limit, and Bracket orders with instant balance validation.
- **Virtual Portfolio & Watchlist**: Live unrealized P&L calculations, holding allocations, and custom watchlists.
- **Wallet & Ledger Center**: Balance top-up with idempotency keys and an immutable financial transaction history.
- **Offline / Sandbox Demo Mode**: Explore platform features with realistic simulated data even without backend connectivity.

---

## 🛠️ Tech Stack

- **Framework**: React 18 with Vite
- **Routing**: React Router v6
- **HTTP Client**: Axios with JWT session expiration interceptors
- **Styling**: Modern CSS Design Tokens & Glassmorphic variables (`index.css`)
- **Icons**: Lucide Icons & Custom SVG glyphs

---

## ⚙️ Environment Configuration

Create a `.env` file in `tradeflow-frontend`:

```env
# Point to your local API Gateway or AWS production deployment
VITE_API_BASE_URL=https://api.tradeflow.hbhanot.tech
```

*If omitted, the frontend defaults to relative paths `/` (proxy) or `http://localhost:8080`.*

---

## 🚀 Getting Started

### 1. Install Dependencies
```bash
npm install
```

### 2. Start Local Development Server
```bash
npm run dev
```
Open [http://localhost:5173](http://localhost:5173) in your browser.

### 3. Build for Production
```bash
npm run build
```
Generates optimized, minified production assets in the `dist/` folder ready for deployment on Vercel, Netlify, or Nginx.
