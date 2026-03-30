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
    const [loadingReport, setLoadingReport] = useState(false);
    const [aiReport, setAiReport] = useState(null);
    const [showAiModal, setShowAiModal] = useState(false);

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

    const fetchAllPrices = useCallback(async () => {
        if (!holdings.length) return;
        try {
            const symbols = holdings.map(h => h.symbol).join(',');
            const res = await api.get('/api/v1/market/prices', { params: { symbols } });
            setPrices(prev => ({ ...prev, ...res.data }));
        } catch {}
    }, [holdings]);

    useEffect(() => {
        fetchHoldings();
        fetchWallet();
        fetchBrokerStatus();
    }, [fetchHoldings, fetchWallet, fetchBrokerStatus]);

    useEffect(() => {
        fetchAllPrices();
        const id = setInterval(fetchAllPrices, 2000);
        return () => clearInterval(id);
    }, [fetchAllPrices]);

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

    async function fetchAiReport() {
        setLoadingReport(true);
        setShowAiModal(true);
        try {
            const res = await api.get('/api/v1/portfolio/ai-summary');
            setAiReport(res.data);
        } catch (err) {
            setAiReport("⚠️ Failed to generate AI report. Please ensure GEMINI_API_KEY is configured.");
        } finally {
            setLoadingReport(false);
        }
    }

    const renderAiContent = (text) => {
        if (!text) return null;
        return text.split('\n').map((line, i) => {
            // Very basic markdown parsing
            let formattedLine = line;
            if (line.startsWith('### ')) return <h3 key={i} style={{ color: 'var(--primary)', marginTop: '1.5rem', marginBottom: '0.5rem' }}>{line.slice(4)}</h3>;
            if (line.startsWith('## ')) return <h2 key={i} style={{ color: 'var(--primary)', marginTop: '1.5rem', marginBottom: '0.5rem' }}>{line.slice(3)}</h2>;
            if (line.startsWith('* ') || line.startsWith('- ')) return <li key={i} style={{ marginLeft: '1.5rem', marginBottom: '0.5rem' }}>{line.slice(2)}</li>;
            
            // Bold
            const boldParts = line.split('**');
            if (boldParts.length > 1) {
                return (
                    <p key={i} style={{ marginBottom: '0.75rem', lineHeight: '1.6' }}>
                        {boldParts.map((part, index) => (index % 2 === 1 ? <strong key={index} style={{ color: 'var(--primary)' }}>{part}</strong> : part))}
                    </p>
                );
            }
            
            return <p key={i} style={{ marginBottom: '0.75rem', lineHeight: '1.6' }}>{line}</p>;
        });
    };

    const fmt = (n) => Number(n ?? 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

    return (
        <Layout title="Dashboard">
            <Toast toasts={toasts} />

            <div className="page-header">
                <div>
                    <h1>
                        {new Date().getHours() < 12 ? 'Good morning' : 
                         new Date().getHours() < 17 ? 'Good afternoon' : 
                         new Date().getHours() < 21 ? 'Good evening' : 'Good night'}, 
                        {user?.username ?? 'Trader'} 👋
                    </h1>
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
                                <div style={{ padding: '1.5rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid var(--border)' }}>
                                    <div style={{ display: 'flex', gap: '1.5rem', alignItems: 'center' }}>
                                        <span style={{ fontWeight: 800, fontSize: '1.1rem', letterSpacing: '-0.02em', color: 'var(--text)' }}>Activity Ledger</span>
                                        <div className="tab-row" style={{ padding: '4px', background: 'rgba(255,255,255,0.03)', borderRadius: '8px' }}>
                                            <button 
                                                className="btn" 
                                                style={{ fontSize: '0.65rem', padding: '6px 16px', background: ledgerTab === 'REAL-TIME' ? 'var(--primary-dim)' : 'transparent', color: ledgerTab === 'REAL-TIME' ? 'var(--primary)' : 'var(--text-muted)', border: ledgerTab === 'REAL-TIME' ? '1px solid var(--primary-dim)' : '1px solid transparent' }}
                                                onClick={() => setLedgerTab('REAL-TIME')}
                                            >
                                                REAL-TIME
                                            </button>
                                            <button 
                                                className="btn" 
                                                style={{ fontSize: '0.65rem', padding: '6px 16px', background: ledgerTab === 'REPORTS' ? 'var(--primary-dim)' : 'transparent', color: ledgerTab === 'REPORTS' ? 'var(--primary)' : 'var(--text-muted)', border: ledgerTab === 'REPORTS' ? '1px solid var(--primary-dim)' : '1px solid transparent' }}
                                                onClick={() => setLedgerTab('REPORTS')}
                                            >
                                                REPORTS
                                            </button>
                                        </div>
                                    </div>
                                    <button 
                                        className="btn btn-primary" 
                                        style={{ fontSize: '0.75rem', padding: '8px 16px', fontWeight: 800, background: 'linear-gradient(90deg, #00FFD1 0%, #00BFFF 100%)', color: '#000', boxShadow: '0 0 20px rgba(0,255,209,0.3)' }}
                                        onClick={fetchAiReport}
                                    >
                                        ✨ AI PORTFOLIO INSIGHTS
                                    </button>
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

            {/* AI Report Modal */}
            {showAiModal && (
                <div className="modal-overlay" style={{ backdropFilter: 'blur(12px)', zIndex: 1000 }}>
                    <style>{`
                        @keyframes neural-scan {
                          0% { transform: translateY(-100%); opacity: 0; }
                          50% { opacity: 1; }
                          100% { transform: translateY(100%); opacity: 0; }
                        }
                        .scanning-line {
                          position: absolute;
                          top: 0;
                          left: 0;
                          right: 0;
                          height: 2px;
                          background: linear-gradient(90deg, transparent, var(--primary), transparent);
                          box-shadow: 0 0 15px var(--primary);
                          animation: neural-scan 2s linear infinite;
                        }
                    `}</style>
                    <div className="card" style={{ maxWidth: '700px', width: '90%', maxHeight: '85vh', overflow: 'hidden', display: 'flex', flexDirection: 'column', position: 'relative', border: '1px solid var(--primary-dim)', boxShadow: '0 0 50px rgba(0,255,209,0.1)' }}>
                        {loadingReport && <div className="scanning-line" />}
                        
                        <div style={{ padding: '1.5rem 2rem', borderBottom: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                                <div style={{ background: 'var(--primary-dim)', color: 'var(--primary)', padding: '8px', borderRadius: '8px', fontSize: '1.2rem' }}>✨</div>
                                <div>
                                    <div style={{ fontWeight: 800, fontSize: '1.1rem', color: 'var(--text)' }}>AI Market Intelligence</div>
                                    <div style={{ fontSize: '0.7rem', color: 'var(--primary)', textTransform: 'uppercase', fontWeight: 700, letterSpacing: '0.1em' }}>{loadingReport ? 'Neural Analysis in Progress...' : 'Portfolio Insight Generated'}</div>
                                </div>
                            </div>
                            <button className="btn btn-ghost" style={{ padding: '8px' }} onClick={() => setShowAiModal(false)}>✕</button>
                        </div>

                        <div style={{ padding: '2rem', overflowY: 'auto', flex: 1, background: 'rgba(255,255,255,0.01)' }}>
                            {loadingReport ? (
                                <div style={{ textAlign: 'center', padding: '4rem' }}>
                                    <div className="spinner" style={{ margin: '0 auto 2rem', width: '50px', height: '50px', borderColor: 'var(--primary)', borderRightColor: 'transparent' }} />
                                    <div style={{ color: 'var(--primary)', fontWeight: 700, fontSize: '0.9rem', letterSpacing: '0.1em' }}>SYNCHRONIZING PORTFOLIO DELTAS...</div>
                                    <div style={{ color: 'var(--text-muted)', fontSize: '0.8rem', marginTop: '1rem' }}>Consulting TradeFlow AI Strategist</div>
                                </div>
                            ) : (
                                <div style={{ color: 'var(--text-2)', fontSize: '0.95rem' }}>
                                    {renderAiContent(aiReport)}
                                    <div style={{ marginTop: '2.5rem', padding: '1.5rem', background: 'var(--surface)', borderRadius: '12px', border: '1px solid var(--border)', fontSize: '0.8rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                                        💡 <strong>Pro Tip:</strong> Re-generate this report after major market moves to see revised strategic positioning.
                                    </div>
                                </div>
                            )}
                        </div>

                        <div style={{ padding: '1.5rem 2rem', borderTop: '1px solid var(--border)', display: 'flex', justifyContent: 'flex-end', background: 'var(--surface)' }}>
                            <button className="btn btn-primary" style={{ padding: '0.75rem 2rem', fontWeight: 800 }} onClick={() => setShowAiModal(false)}>ACKNOWLEDGE INSIGHTS</button>
                        </div>
                    </div>
                </div>
            )}
        </Layout>
    );
}
