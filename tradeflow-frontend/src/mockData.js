// Comprehensive mock data for offline demo mode and graceful API fallbacks

export const MOCK_PRICES = {
    'RELIANCE': 2948.50,
    'TCS': 3845.20,
    'HDFCBANK': 1640.75,
    'INFY': 1520.40,
    'ICICIBANK': 1242.10,
    'WIPRO': 542.80,
    'SBIN': 814.20,
    'BAJFINANCE': 7185.00,
    'ZOMATO': 252.00,
    'SUZLON': 74.80,
    'TATASTEEL': 158.90,
    'TATACHEM': 1042.15,
    'NIFTY 50': 24850.30,
    'SENSEX': 81420.10,
};

export const MOCK_HOLDINGS = [
    { id: 1, symbol: 'RELIANCE', exchange: 'NSE', totalQuantity: 50, avgPrice: 2890.00 },
    { id: 2, symbol: 'TCS', exchange: 'NSE', totalQuantity: 30, avgPrice: 3810.50 },
    { id: 3, symbol: 'HDFCBANK', exchange: 'NSE', totalQuantity: 100, avgPrice: 1615.20 },
    { id: 4, symbol: 'INFY', exchange: 'NSE', totalQuantity: 75, avgPrice: 1490.00 },
    { id: 5, symbol: 'TATASTEEL', exchange: 'NSE', totalQuantity: 200, avgPrice: 152.40 },
];

export const MOCK_WALLET = {
    balance: 412500.00,
    currency: 'INR'
};

export const MOCK_LEDGER = [
    { id: 1, timestamp: new Date(Date.now() - 3600000 * 2).toISOString(), type: 'DEPOSIT', amount: 500000, status: 'SUCCESS' },
    { id: 2, timestamp: new Date(Date.now() - 3600000 * 5).toISOString(), type: 'TRADE_BUY', amount: 144500, status: 'SUCCESS' },
    { id: 3, timestamp: new Date(Date.now() - 3600000 * 12).toISOString(), type: 'TRADE_BUY', amount: 114315, status: 'SUCCESS' },
    { id: 4, timestamp: new Date(Date.now() - 3600000 * 26).toISOString(), type: 'TRADE_BUY', amount: 161520, status: 'SUCCESS' },
    { id: 5, timestamp: new Date(Date.now() - 3600000 * 48).toISOString(), type: 'TRADE_SELL', amount: 232835, status: 'SUCCESS' },
];

export const MOCK_ORDERS = [
    { id: 'ORD-101', symbol: 'RELIANCE', side: 'BUY', quantity: 20, type: 'LIMIT', executedPrice: 2890.00, status: 'COMPLETED', createdAt: new Date(Date.now() - 3600000 * 4).toISOString() },
    { id: 'ORD-102', symbol: 'TCS', side: 'BUY', quantity: 15, type: 'MARKET', executedPrice: 3810.50, status: 'COMPLETED', createdAt: new Date(Date.now() - 3600000 * 8).toISOString() },
    { id: 'ORD-103', symbol: 'HDFCBANK', side: 'BUY', quantity: 50, type: 'LIMIT', executedPrice: 1615.20, status: 'COMPLETED', createdAt: new Date(Date.now() - 3600000 * 18).toISOString() },
    { id: 'ORD-104', symbol: 'INFY', side: 'BUY', quantity: 25, type: 'MARKET', executedPrice: 1490.00, status: 'COMPLETED', createdAt: new Date(Date.now() - 3600000 * 30).toISOString() },
    { id: 'ORD-105', symbol: 'ZOMATO', side: 'BUY', quantity: 100, type: 'LIMIT', executedPrice: 245.50, status: 'PENDING', createdAt: new Date(Date.now() - 3600000 * 45).toISOString() },
];

export const MOCK_HISTORY = MOCK_ORDERS;

export const MOCK_ANALYTICS = {
    realizedProfit: 34250.00,
    dailyReturnPercent: 2.34,
    benchmarkDifference: 1.12,
    bestPerformers: [
        { symbol: 'RELIANCE', unrealizedProfit: 2925.00 },
        { symbol: 'INFY', unrealizedProfit: 2280.00 },
        { symbol: 'HDFCBANK', unrealizedProfit: 2555.00 },
    ],
    worstPerformers: [
        { symbol: 'TCS', unrealizedProfit: 1041.00 },
    ]
};

export function generateMockHistory(symbol = 'RELIANCE', range = 'TODAY') {
    const basePrice = MOCK_PRICES[symbol] || 1500;
    const count = range === 'TODAY' ? 30 : range === '30D' ? 30 : 60;
    const data = [];
    let current = basePrice * 0.96;

    for (let i = 0; i < count; i++) {
        const change = (Math.random() - 0.47) * (basePrice * 0.015);
        current = Math.max(basePrice * 0.85, current + change);
        
        let label = '';
        if (range === 'TODAY') {
            const h = 9 + Math.floor(i / 5);
            const m = (i % 5) * 12;
            label = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
        } else {
            const d = new Date(Date.now() - (count - i) * 86400000);
            label = d.toLocaleDateString('en-IN', { month: 'short', day: 'numeric' });
        }

        data.push({
            time: label,
            label: label,
            price: Number(current.toFixed(2))
        });
    }
    // ensure last price is close to current ltp
    data[data.length - 1].price = basePrice;
    return data;
}
