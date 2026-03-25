import { useState, useEffect, useCallback } from 'react';
import Layout from '../components/Layout';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';
import { useWebSocket } from '../hooks/useWebSocket';
import StockChart from '../components/StockChart';
import MarketNews from '../components/MarketNews';
import EquityChart from '../components/EquityChart';
import HoldingsGrid from '../components/HoldingsGrid';
import TradeDesk from '../components/TradeDesk';
import MarketWatch from '../components/MarketWatch';

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
    const [brokerStatus, setBrokerStatus] = useState({ connected: false, broker: 'None' });
    const [loadingBroker, setLoadingBroker] = useState(true);
    const [toasts, setToasts] = useState([]);


    const [side, setSide] = useState('BUY');
    const [orderForm, setOrderForm] = useState({ symbol: '', quantity: '', exchange: 'NSE', orderType: 'MARKET', pricePerUnit: '' });
    const [placing, setPlacing] = useState(false);
    const [showTradeDesk, setShowTradeDesk] = useState(false);
    const { marketData, orderUpdates } = useWebSocket(user?.userId);
    const [chartData, setChartData] = useState({});
    const [ledgerTab, setLedgerTab] = useState('REAL-TIME');

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
            // ignore
        } finally {
            setLoadingHoldings(false);
        }
    }, []);

    const fetchWallet = useCallback(async () => {
        try {
            const res = await api.get(`/api/v1/wallets/user/${user?.userId}`);
            setWallet(res.data);
        } catch {}
    }, [user]);

    const fetchChartData = useCallback(async (sym) => {
        try {
            const res = await api.get(`/api/v1/market/history/${sym}?range=TODAY`);
            setChartData(prev => ({ ...prev, [sym]: res.data }));
        } catch {}
    }, []);

    const fetchBrokerStatus = useCallback(async () => {
        try {
            const res = await api.get('/auth/upstox/status');
            setBrokerStatus(res.data);
        } catch {
            // ignore
        } finally {
            setLoadingBroker(false);
        }
    }, []);

    useEffect(() => {
        fetchHoldings();
        fetchWallet();
        fetchBrokerStatus();
    }, [fetchHoldings, fetchWallet, fetchBrokerStatus]);

    useEffect(() => {
        if (holdings.length) {
            holdings.forEach(h => {
                if (!chartData[h.symbol]) {
                    fetchChartData(h.symbol);
                }
            });
        }
    }, [holdings, fetchChartData, chartData]);

    useEffect(() => {
        if (orderUpdates) {
            addToast(`Update: ${orderUpdates.side} order for ${orderUpdates.symbol} is COMPLETED!`);
            fetchHoldings();
            fetchWallet();
        }
    }, [orderUpdates, fetchHoldings, fetchWallet]);


    const totalInvested = holdings.reduce((s, h) => s + ((h.avgPrice ?? 0) * (h.totalQuantity ?? 0)), 0);
    const totalCurrent = holdings.reduce((s, h) => s + (((prices[h.symbol] ?? h.avgPrice ?? 0) * (h.totalQuantity ?? 0))), 0);
    const pnl = totalCurrent - totalInvested;
    const pnlPct = totalInvested ? (pnl / totalInvested) * 100 : 0;

    async function connectBroker() {
        try {
            const res = await api.get('/auth/upstox/login-url');
            if (res.data?.url) {
                window.location.href = res.data.url;
            }
        } catch (err) {
            addToast('Failed to get connection URL', 'error');
        }
    }

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
                <div>
                    <h1>Good evening, {user?.username ?? 'Trader'} 👋</h1>
                    <p>Here's your portfolio summary for today.</p>
                </div>
                <button 
                    className={`btn ${showTradeDesk ? 'btn-red' : 'btn-primary'}`} 
                    onClick={() => setShowTradeDesk(!showTradeDesk)}
                >
                    {showTradeDesk ? 'EXIT TRADE DESK' : 'LAUNCH TRADE DESK'}
                </button>
            </div>


            {showTradeDesk ? (
                <div className="trade-desk-container" style={{ animation: 'fadeIn 0.5s ease' }}>
                    <TradeDesk symbol={holdings[0]?.symbol || 'RELIANCE'} />
                </div>
            ) : (
                <>
                    <div className="hero-card" style={{ marginBottom: '2rem' }}>
                        <div className="hero-label">
                            CURRENT PORTFOLIO VALUE <span>ⓘ</span>
                        </div>
                        
                        <div className="hero-value-row">
                            <div className="hero-value">₹{fmt(totalCurrent + (wallet?.balance ?? 0))}</div>
                            <div className={`hero-delta ${totalInvested ? (pnl >= 0 ? 'green' : 'red') : 'muted'}`}>
                                {totalInvested ? (pnl >= 0 ? '+' : '-') : ''}{Math.abs(pnlPct).toFixed(2)}%
                            </div>
                        </div>

                        <div className="hero-stats-grid">
                            <div className="hero-stat-item">
                                <div className="label">EQUITIES</div>
                                <div className="value">₹{fmt(totalCurrent)}</div>
                            </div>
                            <div className="hero-stat-item">
                                <div className="label">CASH BALANCE</div>
                                <div className="value" style={{ color: 'var(--primary)' }}>₹{fmt(wallet?.balance)}</div>
                            </div>
                            <div className="hero-stat-item">
                                <div className="label">1D CHANGE</div>
                                <div className={`value ${pnl >= 0 ? 'green' : 'red'}`}>
                                    {pnl >= 0 ? '+' : ''}₹{fmt(pnl)}
                                </div>
                            </div>
                            <div className="hero-stat-item">
                                <div className="label">OPEN POSITIONS</div>
                                <div className="value">{holdings.length}</div>
                            </div>
                        </div>
                        <EquityChart />
                    </div>

                    <div className="dashboard-grid">
                        <div className="dashboard-left">
                            <HoldingsGrid holdings={holdings} marketData={marketData} prices={prices} />
                            <div className="card" style={{ padding: 0 }}>
                                <div style={{ padding: '1.5rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                    <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
                                        <span style={{ fontWeight: 800, fontSize: '1.1rem', letterSpacing: '-0.02em' }}>Activity Ledger</span>
                                        <div className="tab-row">
                                            <button 
                                                className="btn" 
                                                style={{ fontSize: '0.65rem', padding: '4px 12px', background: ledgerTab === 'REAL-TIME' ? 'var(--primary-dim)' : 'transparent', color: ledgerTab === 'REAL-TIME' ? 'var(--primary)' : 'var(--text-muted)' }}
                                                onClick={() => setLedgerTab('REAL-TIME')}
                                            >
                                                REAL-TIME
                                            </button>
                                            <button 
                                                className="btn" 
                                                style={{ fontSize: '0.65rem', padding: '4px 12px', background: ledgerTab === 'REPORTS' ? 'var(--primary-dim)' : 'transparent', color: ledgerTab === 'REPORTS' ? 'var(--primary)' : 'var(--text-muted)' }}
                                                onClick={() => setLedgerTab('REPORTS')}
                                            >
                                                REPORTS
                                            </button>
                                        </div>
                                    </div>
                                </div>
                                {loadingHoldings ? (
                                    <div className="centered-spinner"><div className="spinner" /></div>
                                ) : holdings.length === 0 ? (
                                    <div style={{ padding: '4rem 2rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                                        <div style={{ fontSize: '2rem', marginBottom: '1rem' }}>📭</div>
                                        No holdings yet. Place an order to start trading.
                                    </div>
                                ) : (
                                    <table className="tf-table">
                                        <thead>
                                            <tr>
                                                <th>ASSET / EXCHANGE</th>
                                                <th>QTY</th>
                                                <th>AVG COST</th>
                                                <th>LTP</th>
                                                <th>PROFIT / LOSS</th>
                                                <th>MARKET VALUE</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {holdings.map((h) => {
                                                const avgPrice = h.avgPrice ?? 0;
                                                const quantity = h.totalQuantity ?? 0;
                                                const ltp = marketData[h.symbol] ?? prices[h.symbol] ?? avgPrice;
                                                const hlPnl = (ltp - avgPrice) * quantity;
                                                const pct = avgPrice ? ((ltp - avgPrice) / avgPrice) * 100 : 0;
                                                return (
                                                    <tr key={h.id ?? h.symbol}>
                                                        <td>
                                                            <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
                                                                <div style={{ width: '32px', height: '32px', borderRadius: '8px', background: 'var(--surface)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 800, color: 'var(--primary)', fontSize: '0.75rem' }}>
                                                                    {h.symbol.slice(0, 1)}
                                                                </div>
                                                                <div>
                                                                    <div style={{ fontWeight: 700, color: 'var(--text)' }}>{h.symbol}</div>
                                                                    <div style={{ fontSize: '0.65rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>{h.exchange ?? 'NSE'}</div>
                                                                </div>
                                                            </div>
                                                        </td>
                                                        <td style={{ fontWeight: 600 }}>{quantity}</td>
                                                        <td>₹{fmt(avgPrice)}</td>
                                                        <td style={{ fontWeight: 700, color: 'var(--text)' }}>₹{fmt(ltp)}</td>
                                                        <td>
                                                            <div className={hlPnl >= 0 ? 'green' : 'red'} style={{ fontWeight: 700 }}>
                                                                {hlPnl >= 0 ? '+' : ''}₹{fmt(hlPnl)}
                                                            </div>
                                                            <div className={pct >= 0 ? 'green' : 'red'} style={{ fontSize: '0.72rem', opacity: 0.8 }}>
                                                                {pct >= 0 ? '▲' : '▼'}{Math.abs(pct).toFixed(2)}%
                                                            </div>
                                                        </td>
                                                        <td style={{ fontWeight: 700 }}>₹{fmt(ltp * quantity)}</td>
                                                    </tr>
                                                );
                                            })}
                                        </tbody>
                                    </table>
                                )}
                                <div style={{ padding: '1.25rem 1.5rem', borderTop: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                                    <span>Showing {holdings.length} active positions</span>
                                    <div style={{ display: 'flex', gap: '8px' }}>
                                        <button className="btn" style={{ padding: '4px 8px', background: 'var(--surface)', border: '1px solid var(--border)' }}>&lt;</button>
                                        <button className="btn" style={{ padding: '4px 12px', background: 'var(--primary-dim)', color: 'var(--primary)', border: '1px solid var(--primary)' }}>1</button>
                                        <button className="btn" style={{ padding: '4px 8px', background: 'var(--surface)', border: '1px solid var(--border)' }}>&gt;</button>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div className="dashboard-right">
                            {!brokerStatus.connected && (
                                <div className="card" style={{ alignSelf: 'start' }}>
                                    <div style={{ fontWeight: 600, marginBottom: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                        <span>Broker Connection</span>
                                        {loadingBroker ? null : (
                                            <span className="badge badge-red" style={{ fontSize: '0.65rem' }}>
                                                DISCONNECTED
                                            </span>
                                        )}
                                    </div>
                                    
                                    <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginBottom: '1.25rem' }}>
                                        Connect your Upstox account to enable live market data and real-time trading.
                                    </div>

                                    <button 
                                        type="button" 
                                        className="btn btn-primary btn-full"
                                        style={{ background: 'var(--primary)', color: '#000' }}
                                        onClick={connectBroker}
                                    >
                                        Connect Upstox Broker
                                    </button>
                                </div>
                            )}

                            <div className="card" style={{ alignSelf: 'start' }}>
                                <div style={{ fontWeight: 600, marginBottom: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                    <span>Market Watch</span>
                                    <span className="badge badge-error">URGENT</span>
                                </div>
                                <MarketWatch />
                            </div>

                            <div className="card" style={{ alignSelf: 'start' }}>
                                <div style={{ fontWeight: 600, marginBottom: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                    <span>Market Intelligence</span>
                                    <span className="status-badge status-successful">LIVE</span>
                                </div>
                                <MarketNews />
                            </div>

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
                    </div>
                </>
            )}
        </Layout>
    );
}
