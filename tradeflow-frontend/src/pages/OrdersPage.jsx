import { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import Layout from '../components/Layout';
import SymbolSearch from '../components/SymbolSearch';
import api from '../api/axios';

const EXCHANGES = ['NSE', 'BSE'];
const ORDER_TYPES = ['MARKET', 'LIMIT', 'STOP_LOSS'];
const SIDES = ['BUY', 'SELL'];

export default function OrdersPage() {
    const location = useLocation();
    const queryParams = new URLSearchParams(location.search);
    const initialSymbol = queryParams.get('symbol') || '';

    const [form, setForm] = useState({
        symbol: initialSymbol, quantity: '', exchange: 'NSE',
        side: 'BUY', orderType: 'MARKET',
        pricePerUnit: '', triggerPrice: '',
    });
    const [livePrice, setLivePrice] = useState(null);
    const [fetchingPrice, setFetchingPrice] = useState(false);
    const [showConfirm, setShowConfirm] = useState(false);
    const [placing, setPlacing] = useState(false);
    const [result, setResult] = useState(null);
    const [history, setHistory] = useState([]);

    async function fetchPriceForSym(symbol) {
        if (!symbol.trim()) return;
        setFetchingPrice(true);
        try {
            const r = await api.get(`/api/v1/market/price/${symbol.toUpperCase()}`);
            setLivePrice(r.data);
            if (form.orderType === 'MARKET') {
                setForm(f => ({ ...f, pricePerUnit: parseFloat(r.data).toFixed(2) }));
            }
        } catch { setLivePrice('N/A'); }
        finally { setFetchingPrice(false); }
    }

    async function fetchHistory() {
        try {
            const r = await api.get('/api/v1/orders');
            setHistory(r.data);
        } catch {}
    }

    useEffect(() => {
        if (initialSymbol) fetchPriceForSym(initialSymbol);
        fetchHistory();
    }, [initialSymbol]);

    async function confirmOrder() {
        setPlacing(true);
        try {
            const derivedTriggerPrice = form.orderType === 'MARKET'
                ? null
                : (form.triggerPrice ? parseFloat(form.triggerPrice) : (form.pricePerUnit ? parseFloat(form.pricePerUnit) : null));

            const body = {
                symbol: form.symbol.toUpperCase(),
                quantity: parseInt(form.quantity),
                exchange: form.exchange,
                side: form.side,
                orderType: form.orderType,
                pricePerUnit: form.pricePerUnit ? parseFloat(form.pricePerUnit) : null,
                triggerPrice: derivedTriggerPrice,
            };
            const res = await api.post('/api/v1/orders', body);
            setResult({ ok: true, msg: `✅ Order placed! ID: ${res.data.id} | Status: ${res.data.status}` });
            setForm({ symbol: '', quantity: '', exchange: 'NSE', side: 'BUY', orderType: 'MARKET', pricePerUnit: '', triggerPrice: '' });
            setLivePrice(null);
            fetchHistory();
        } catch (err) {
            setResult({ ok: false, msg: `❌ ${err.response?.data?.message ?? 'Failed to place order'}` });
        } finally {
            setPlacing(false);
            setShowConfirm(false);
        }
    }

    const totalEstimate = form.pricePerUnit && form.quantity
        ? (parseFloat(form.pricePerUnit) * parseInt(form.quantity)).toLocaleString('en-IN', { minimumFractionDigits: 2 })
        : '—';

    const fmt = n => Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2 });

    return (
        <Layout title="Place Order">
            <div className="page-header">
                <h1>Execution Terminal</h1>
                <p>Deploy capital with high-precision KINETIC execution controls.</p>
            </div>

            <div className="orders-grid" style={{ display: 'grid', gridTemplateColumns: 'minmax(400px, 1fr) 2fr', gap: '2rem', alignItems: 'start' }}>
                <div className="execution-side">
                    {result && (
                        <div style={{
                            background: result.ok ? 'rgba(0,255,150,0.1)' : 'rgba(255,0,80,0.1)',
                            color: result.ok ? 'var(--green)' : 'var(--red)',
                            padding: '1rem', borderRadius: '12px', border: `1px solid ${result.ok ? 'var(--green)' : 'var(--red)'}`,
                            marginBottom: '1.5rem', fontSize: '0.9rem', textAlign: 'center'
                        }}>
                            {result.msg}
                        </div>
                    )}

                    <div className="card" style={{ padding: '2rem' }}>
                        <div className="tab-row" style={{ marginBottom: '2rem' }}>
                            {SIDES.map(s => (
                                <button key={s} type="button"
                                    className={`order-tab ${s.toLowerCase()} ${form.side === s ? 'active' : ''}`}
                                    style={{ flex: 1, padding: '12px' }}
                                    onClick={() => setForm(f => ({ ...f, side: s }))}>
                                    {s}
                                </button>
                            ))}
                        </div>

                        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                            <div className="form-group">
                                <label className="auth-label">Asset Search</label>
                                <SymbolSearch onSelect={(sym) => {
                                    setForm(f => ({ ...f, symbol: sym }));
                                    setLivePrice(null);
                                    fetchPriceForSym(sym);
                                }} />
                                {form.symbol && (
                                    <div style={{ marginTop: '8px', fontSize: '0.9rem', color: 'var(--primary)', fontWeight: 700 }}>
                                        SELECTED: {form.symbol.toUpperCase()}
                                    </div>
                                )}
                            </div>

                            {livePrice && (
                                <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '12px', padding: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                    <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>Real-time LTP</span>
                                    <span style={{ fontWeight: 800, fontSize: '1.2rem', color: 'var(--green)' }}>₹{typeof livePrice === 'number' ? fmt(livePrice) : livePrice}</span>
                                </div>
                            )}

                            <div className="form-row">
                                <div className="form-group">
                                    <label className="auth-label">Quantity</label>
                                    <input className="form-input" type="number" min="1" placeholder="0" value={form.quantity}
                                        onChange={e => setForm(f => ({ ...f, quantity: e.target.value }))} />
                                </div>
                                <div className="form-group">
                                    <label className="auth-label">Exchange</label>
                                    <select className="form-input" value={form.exchange}
                                        onChange={e => setForm(f => ({ ...f, exchange: e.target.value }))}>
                                        {EXCHANGES.map(ex => <option key={ex}>{ex}</option>)}
                                    </select>
                                </div>
                            </div>

                            <div className="form-group">
                                <label className="auth-label">Order Type</label>
                                <div style={{ display: 'flex', gap: '8px' }}>
                                    {ORDER_TYPES.map(t => (
                                        <button 
                                            key={t}
                                            type="button"
                                            className={`btn ${form.orderType === t ? 'btn-primary-dim' : 'btn-ghost'}`}
                                            style={{ flex: 1, fontSize: '0.7rem' }}
                                            onClick={() => setForm(f => ({ ...f, orderType: t }))}
                                        >
                                            {t}
                                        </button>
                                    ))}
                                </div>
                            </div>

                            {form.orderType !== 'MARKET' && (
                                <div className="form-group">
                                    <label className="auth-label">Limit Price (₹)</label>
                                    <input className="form-input" type="number" step="0.01" placeholder="0.00" value={form.pricePerUnit}
                                        onChange={e => setForm(f => ({ ...f, pricePerUnit: e.target.value }))} />
                                </div>
                            )}

                            {form.orderType === 'STOP_LOSS' && (
                                <div className="form-group">
                                    <label className="auth-label">Trigger Price (₹)</label>
                                    <input className="form-input" type="number" step="0.01" placeholder="0.00" value={form.triggerPrice}
                                        onChange={e => setForm(f => ({ ...f, triggerPrice: e.target.value }))} />
                                </div>
                            )}

                            <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '12px', padding: '1.25rem', marginTop: '1rem' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                                    <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>Estimated Capital Required</span>
                                    <span style={{ fontWeight: 800, fontSize: '1.1rem' }}>₹{totalEstimate}</span>
                                </div>
                                <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>*Excluding brokerage and SEBI charges.</div>
                            </div>

                            <button
                                className={`btn btn-full ${form.side === 'BUY' ? 'btn-primary' : 'btn-red'}`}
                                style={{ padding: '1rem', fontWeight: 800, fontSize: '1.1rem', marginTop: '1rem' }}
                                onClick={() => setShowConfirm(true)}
                                disabled={!form.symbol || !form.quantity}
                            >
                                REVIEW {form.side} ORDER →
                            </button>
                        </div>
                    </div>
                </div>

                <div className="history-side">
                    <div className="card" style={{ padding: 0 }}>
                        <div style={{ padding: '1.5rem', borderBottom: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <h2 style={{ fontSize: '1.1rem', fontWeight: 800 }}>Past Orders</h2>
                            <button className="btn btn-ghost" style={{ fontSize: '0.7rem' }} onClick={fetchHistory}>REFRESH</button>
                        </div>
                        <div className="table-container" style={{ maxHeight: '600px', overflowY: 'auto' }}>
                            <table className="ledger-table">
                                <thead>
                                    <tr>
                                        <th>Symbol</th>
                                        <th>Side</th>
                                        <th>Qty</th>
                                        <th>Type</th>
                                        <th>Price</th>
                                        <th>Status</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {history.length === 0 ? (
                                        <tr>
                                            <td colSpan="6" style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-muted)' }}>
                                                No recent orders found.
                                            </td>
                                        </tr>
                                    ) : (
                                        history.map(order => (
                                            <tr key={order.id}>
                                                <td style={{ fontWeight: 700 }}>{order.symbol}</td>
                                                <td>
                                                    <span style={{ color: order.side === 'BUY' ? 'var(--primary)' : 'var(--red)', fontWeight: 800 }}>
                                                        {order.side}
                                                    </span>
                                                </td>
                                                <td>{order.quantity}</td>
                                                <td style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>{order.type}</td>
                                                <td>₹{fmt(order.executedPrice || order.triggerPrice || 0)}</td>
                                                <td>
                                                    <span style={{ 
                                                        fontSize: '0.65rem', 
                                                        fontWeight: 800, 
                                                        padding: '4px 8px', 
                                                        borderRadius: '4px',
                                                        background: order.status === 'COMPLETED' ? 'rgba(0,255,150,0.1)' : 
                                                                    order.status === 'REJECTED' ? 'rgba(255,0,80,0.1)' : 'rgba(255,180,0,0.1)',
                                                        color: order.status === 'COMPLETED' ? 'var(--green)' : 
                                                               order.status === 'REJECTED' ? 'var(--red)' : 'var(--yellow)'
                                                    }}>
                                                        {order.status}
                                                    </span>
                                                </td>
                                            </tr>
                                        ))
                                    )}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>

            {/* KINETIC Confirmation Modal */}
            {showConfirm && (
                <div className="modal-overlay">
                    <div className="card execution-modal">
                        <div style={{ fontWeight: 800, fontSize: '1.25rem', marginBottom: '1.5rem', color: 'var(--primary)', textAlign: 'center' }}>
                            CONFIRM {form.side} EXECUTION
                        </div>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                            {[
                                ['SYMBOL', form.symbol.toUpperCase()],
                                ['QUANTITY', form.quantity],
                                ['ORDER TYPE', form.orderType],
                                ['EST. TOTAL', `₹${totalEstimate}`],
                            ].map(([l, v]) => (
                                <div key={l} style={{ display: 'flex', justifyContent: 'space-between', padding: '0.75rem 0', borderBottom: '1px solid var(--border)' }}>
                                    <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)' }}>{l}</span>
                                    <span style={{ fontWeight: 700 }}>{v}</span>
                                </div>
                            ))}
                        </div>
                        <div style={{ display: 'flex', gap: '1rem', marginTop: '2rem' }}>
                            <button className="btn btn-ghost btn-full" onClick={() => setShowConfirm(false)}>CANCEL</button>
                            <button className={`btn btn-full ${form.side === 'BUY' ? 'btn-primary' : 'btn-red'}`}
                                onClick={confirmOrder} disabled={placing}>
                                {placing ? 'EXECUTING...' : 'CONFIRM ORDER'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </Layout>
    );
}
