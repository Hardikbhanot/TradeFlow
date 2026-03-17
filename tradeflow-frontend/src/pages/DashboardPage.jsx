import { useState, useEffect, useCallback } from 'react';
import Layout from '../components/Layout';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';

function Toast({ toasts }) {
    return (
        <div className="toast-container">
            {toasts.map(t => (
                <div key={t.id} className={`toast toast-${t.type}`}>
                    {t.type === 'success' ? '✅' : '❌'} {t.msg}
                </div>
            ))}
        </div>
    );
}

export default function DashboardPage() {
    const { user } = useAuth();
    const [holdings, setHoldings] = useState([]);
    const [wallet, setWallet] = useState(null);
    const [prices, setPrices] = useState({});
    const [loadingHoldings, setLoadingHoldings] = useState(true);
    const [toasts, setToasts] = useState([]);

    // Quick order state
    const [side, setSide] = useState('BUY');
    const [orderForm, setOrderForm] = useState({ symbol: '', quantity: '', exchange: 'NSE', orderType: 'MARKET', pricePerUnit: '' });
    const [placing, setPlacing] = useState(false);

    function addToast(msg, type = 'success') {
        const id = Date.now();
        setToasts(t => [...t, { id, msg, type }]);
        setTimeout(() => setToasts(t => t.filter(x => x.id !== id)), 4000);
    }

    const fetchHoldings = useCallback(async () => {
        try {
            const res = await api.get('/api/v1/portfolio');
            setHoldings(res.data);
        } catch {
            // silent
        } finally {
            setLoadingHoldings(false);
        }
    }, []);

    const fetchWallet = useCallback(async () => {
        try {
            const res = await api.get(`/api/v1/wallets/user/${user?.userId}`);
            setWallet(res.data);
        } catch {/* silent */ }
    }, [user]);

    const fetchPrices = useCallback(async (syms) => {
        if (!syms.length) return;
        const results = {};
        for (const sym of syms) {
            try {
                const r = await api.get(`/api/v1/market/price/${sym}`);
                results[sym] = r.data;
            } catch { results[sym] = null; }
            // Stagger requests to avoid 429 Rate Limits
            await new Promise(resolve => setTimeout(resolve, 300));
        }
        setPrices(p => ({ ...p, ...results }));
    }, []);

    useEffect(() => {
        fetchHoldings();
        fetchWallet();
    }, [fetchHoldings, fetchWallet]);

    useEffect(() => {
        if (holdings.length) {
            const syms = [...new Set(holdings.map(h => h.symbol))];
            fetchPrices(syms);
            const id = setInterval(() => fetchPrices(syms), 15000);
            return () => clearInterval(id);
        }
    }, [holdings, fetchPrices]);

    // Compute portfolio stats
    const totalInvested = holdings.reduce((s, h) => s + ((h.avgPrice ?? 0) * (h.totalQuantity ?? 0)), 0);
    const totalCurrent = holdings.reduce((s, h) => s + (((prices[h.symbol] ?? h.avgPrice ?? 0) * (h.totalQuantity ?? 0))), 0);
    const pnl = totalCurrent - totalInvested;
    const pnlPct = totalInvested ? (pnl / totalInvested) * 100 : 0;

    async function placeQuickOrder(e) {
        e.preventDefault();
        setPlacing(true);
        try {
            const triggerPrice = orderForm.orderType === 'MARKET'
                ? null
                : (orderForm.pricePerUnit ? parseFloat(orderForm.pricePerUnit) : null);

            await api.post('/api/v1/orders', {
                symbol: orderForm.symbol.toUpperCase(),
                quantity: parseInt(orderForm.quantity),
                exchange: orderForm.exchange,
                side,
                orderType: orderForm.orderType,
                pricePerUnit: orderForm.pricePerUnit ? parseFloat(orderForm.pricePerUnit) : null,
                triggerPrice,
            });
            addToast(`${side} order placed for ${orderForm.symbol.toUpperCase()}!`);
            setOrderForm(f => ({ ...f, symbol: '', quantity: '', pricePerUnit: '' }));
            setTimeout(fetchHoldings, 2000);
        } catch (err) {
            addToast(err.response?.data?.message ?? 'Order failed', 'error');
        } finally {
            setPlacing(false);
        }
    }

    const fmt = (n) => Number(n ?? 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

    return (
        <Layout title="Dashboard">
            <Toast toasts={toasts} />

            <div className="page-header">
                <h1>Good evening, {user?.username ?? 'Trader'} 👋</h1>
                <p>Here's your portfolio summary for today.</p>
            </div>

            {/* Stat Cards */}
            <div className="stat-cards" style={{ marginBottom: '1.25rem' }}>
                <div className="stat-card">
                    <div className="label">Wallet Balance</div>
                    <div className="value green">₹{fmt(wallet?.balance)}</div>
                    <div className="sub">Available funds</div>
                </div>
                <div className="stat-card">
                    <div className="label">Total Invested</div>
                    <div className="value">₹{fmt(totalInvested)}</div>
                    <div className="sub">{holdings.length} positions</div>
                </div>
                <div className="stat-card">
                    <div className="label">Total P&amp;L</div>
                    <div className={`value ${pnl >= 0 ? 'green' : 'red'}`}>
                        {pnl >= 0 ? '+' : ''}₹{fmt(pnl)}
                    </div>
                    <div className={`sub ${pnl >= 0 ? 'green' : 'red'}`}>
                        {pnl >= 0 ? '▲' : '▼'} {Math.abs(pnlPct).toFixed(2)}%
                    </div>
                </div>
            </div>

            <div className="dashboard-grid">
                {/* Holdings Table */}
                <div className="dashboard-left">
                    <div className="card" style={{ padding: 0 }}>
                        <div style={{ padding: '1.25rem 1.5rem', borderBottom: '1px solid var(--border)' }}>
                            <span style={{ fontWeight: 600 }}>Holdings</span>
                        </div>
                        {loadingHoldings ? (
                            <div className="centered-spinner"><div className="spinner" /></div>
                        ) : holdings.length === 0 ? (
                            <div style={{ padding: '2.5rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                                No holdings yet. Place an order to get started!
                            </div>
                        ) : (
                            <table className="tf-table">
                                <thead>
                                    <tr>
                                        <th>Symbol</th>
                                        <th>Qty</th>
                                        <th>Avg Price</th>
                                        <th>LTP</th>
                                        <th>P&amp;L</th>
                                        <th>Value</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {holdings.map((h) => {
                                        const avgPrice = h.avgPrice ?? 0;
                                        const quantity = h.totalQuantity ?? 0;
                                        const ltp = prices[h.symbol] ?? avgPrice;
                                        const hlPnl = (ltp - avgPrice) * quantity;
                                        const pct = avgPrice ? ((ltp - avgPrice) / avgPrice) * 100 : 0;
                                        return (
                                            <tr key={h.id ?? h.symbol}>
                                                <td>
                                                    <strong>{h.symbol}</strong>
                                                    <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>{h.exchange ?? 'NSE'}</div>
                                                </td>
                                                <td>{quantity}</td>
                                                <td>₹{fmt(avgPrice)}</td>
                                                <td style={{ fontWeight: 600 }}>₹{fmt(ltp)}</td>
                                                <td className={hlPnl >= 0 ? 'green' : 'red'}>
                                                    {hlPnl >= 0 ? '+' : ''}₹{fmt(hlPnl)}
                                                    <div style={{ fontSize: '0.72rem' }}>{pct >= 0 ? '▲' : '▼'}{Math.abs(pct).toFixed(2)}%</div>
                                                </td>
                                                <td>₹{fmt(ltp * quantity)}</td>
                                            </tr>
                                        );
                                    })}
                                </tbody>
                            </table>
                        )}
                    </div>
                </div>

                {/* Quick Order Panel */}
                <div className="card" style={{ alignSelf: 'start' }}>
                    <div style={{ fontWeight: 600, marginBottom: '1rem' }}>Quick Order</div>
                    <form className="order-panel" onSubmit={placeQuickOrder}>
                        <div className="tab-row">
                            <button type="button" className={`order-tab buy ${side === 'BUY' ? 'active' : ''}`} onClick={() => setSide('BUY')}>BUY</button>
                            <button type="button" className={`order-tab sell ${side === 'SELL' ? 'active' : ''}`} onClick={() => setSide('SELL')}>SELL</button>
                        </div>
                        <div className="form-group">
                            <label>Symbol</label>
                            <input className="form-input" placeholder="e.g. RELIANCE" value={orderForm.symbol}
                                onChange={e => setOrderForm(f => ({ ...f, symbol: e.target.value }))} required />
                        </div>
                        <div className="form-row">
                            <div className="form-group">
                                <label>Qty</label>
                                <input className="form-input" type="number" min="1" placeholder="1" value={orderForm.quantity}
                                    onChange={e => setOrderForm(f => ({ ...f, quantity: e.target.value }))} required />
                            </div>
                            <div className="form-group">
                                <label>Exchange</label>
                                <select className="form-input" value={orderForm.exchange}
                                    onChange={e => setOrderForm(f => ({ ...f, exchange: e.target.value }))}>
                                    <option>NSE</option>
                                    <option>BSE</option>
                                </select>
                            </div>
                        </div>
                        <div className="form-group">
                            <label>Order Type</label>
                            <select className="form-input" value={orderForm.orderType}
                                onChange={e => setOrderForm(f => ({ ...f, orderType: e.target.value }))}>
                                <option value="MARKET">Market</option>
                                <option value="LIMIT">Limit</option>
                                <option value="STOP_LOSS">Stop Loss</option>
                            </select>
                        </div>
                        {orderForm.orderType !== 'MARKET' && (
                            <div className="form-group">
                                <label>Price per Unit (₹)</label>
                                <input className="form-input" type="number" step="0.01" placeholder="0.00" value={orderForm.pricePerUnit}
                                    onChange={e => setOrderForm(f => ({ ...f, pricePerUnit: e.target.value }))} />
                            </div>
                        )}
                        <button
                            type="submit"
                            className={`btn btn-full ${side === 'BUY' ? 'btn-primary' : 'btn-red'}`}
                            disabled={placing}
                        >
                            {placing ? 'Placing…' : `Place ${side} Order`}
                        </button>
                    </form>
                </div>
            </div>
        </Layout>
    );
}
