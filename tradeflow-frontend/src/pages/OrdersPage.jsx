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
    const [showOtpModal, setShowOtpModal] = useState(false);
    const [otpCode, setOtpCode] = useState('');
    const [otpError, setOtpError] = useState('');
    const [otpPlacing, setOtpPlacing] = useState(false);
    const [pendingOrder, setPendingOrder] = useState(null);
    const [holdings, setHoldings] = useState([]);
    const [showInsufficientModal, setShowInsufficientModal] = useState(false);

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

    async function fetchHoldings() {
        try {
            const r = await api.get('/api/v1/portfolio');
            setHoldings(r.data);
        } catch {}
    }

    useEffect(() => {
        if (initialSymbol) fetchPriceForSym(initialSymbol);
        fetchHistory();
        fetchHoldings();
    }, [initialSymbol]);

    async function confirmOrder() {
        setPlacing(true);
        const derivedTriggerPrice = form.orderType === 'MARKET'
            ? null
            : (form.triggerPrice ? parseFloat(form.triggerPrice) : (form.pricePerUnit ? parseFloat(form.pricePerUnit) : null));
        try {

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
            fetchHoldings();
        } catch (err) {
            const data = err.response?.data;
            if (data?.status === 'OTP_REQUIRED') {
                setResult({ ok: true, msg: `🔐 ${data.message}` });
                setPendingOrder({
                    symbol: form.symbol.toUpperCase(),
                    quantity: parseInt(form.quantity),
                    exchange: form.exchange,
                    side: form.side,
                    orderType: form.orderType,
                    pricePerUnit: form.pricePerUnit ? parseFloat(form.pricePerUnit) : null,
                    triggerPrice: derivedTriggerPrice,
                });
                setOtpCode('');
                setOtpError('');
            } else {
                setResult({ ok: false, msg: `❌ ${data?.message ?? 'Failed to place order'}` });
            }
        } finally {
            setPlacing(false);
            setShowConfirm(false);
        }
    }

    async function handleOtpSubmit(e) {
        if (e) e.preventDefault();
        if (!otpCode || otpCode.trim().length !== 6) {
            setOtpError('Please enter a 6-digit OTP code.');
            return;
        }

        setOtpPlacing(true);
        setOtpError('');
        try {
            const res = await api.post('/api/v1/orders', {
                ...pendingOrder,
                otp: otpCode,
            });
            setResult({ ok: true, msg: `✅ Sell order placed! ID: ${res.data.id} | Status: ${res.data.status}` });
            setForm({ symbol: '', quantity: '', exchange: 'NSE', side: 'BUY', orderType: 'MARKET', pricePerUnit: '', triggerPrice: '' });
            setLivePrice(null);
            setPendingOrder(null);
            fetchHistory();
            fetchHoldings();
        } catch (err) {
            const data = err.response?.data;
            if (data?.status === 'INVALID_OTP') {
                setOtpError(data.message || 'Incorrect or expired OTP');
            } else {
                setOtpError(data?.message ?? 'OTP verification failed.');
            }
        } finally {
            setOtpPlacing(false);
        }
    }

    const selectedHolding = holdings.find(h => h.symbol.toUpperCase() === form.symbol.toUpperCase());
    const ownedQty = selectedHolding ? selectedHolding.totalQuantity : 0;
    const hasInsufficientHoldings = form.side === 'SELL' && form.symbol && form.quantity && (parseInt(form.quantity) > ownedQty);

    const totalEstimate = form.pricePerUnit && form.quantity
        ? (parseFloat(form.pricePerUnit) * parseInt(form.quantity)).toLocaleString('en-IN', { minimumFractionDigits: 2 })
        : '—';

    const fmt = n => Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2 });

    return (
        <Layout title="Place Order">
            <div className="page-header">
                <h1>Execution Terminal</h1>
                <p>Manage your funds and transaction history in premium style.</p>
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
                                    <div style={{ marginTop: '8px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                        <div style={{ fontSize: '0.9rem', color: 'var(--primary)', fontWeight: 700 }}>
                                            SELECTED: {form.symbol.toUpperCase()}
                                        </div>
                                        {form.side === 'SELL' && (
                                            <div style={{ fontSize: '0.85rem', color: ownedQty > 0 ? 'var(--green)' : 'var(--text-muted)', fontWeight: 700 }}>
                                                Available Qty: {ownedQty} shares
                                            </div>
                                        )}
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
                                    {hasInsufficientHoldings && (
                                        <div style={{ color: 'var(--red)', fontSize: '0.78rem', fontWeight: 600, marginTop: '6px' }}>
                                            ⚠️ Insufficient holdings. You only own {ownedQty} shares.
                                        </div>
                                    )}
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

                            {pendingOrder ? (
                                <div style={{
                                    background: 'rgba(255,75,75,0.03)',
                                    border: '1px dashed rgba(255,75,75,0.4)',
                                    padding: '1.25rem',
                                    borderRadius: '12px',
                                    marginTop: '1rem',
                                    animation: 'fadeIn 0.3s ease'
                                }}>
                                    <label className="auth-label" style={{ color: 'var(--red)', fontWeight: 800, display: 'block', marginBottom: '8px', textAlign: 'center' }}>
                                        ENTER 6-DIGIT SELL OTP
                                    </label>
                                    <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textAlign: 'center', marginBottom: '12px', lineHeight: 1.4 }}>
                                        An OTP has been sent to your email. Enter it below to authorize this sell order.
                                    </p>
                                    <input
                                        className="form-input"
                                        type="text"
                                        maxLength="6"
                                        placeholder="000000"
                                        value={otpCode}
                                        onChange={e => setOtpCode(e.target.value.replace(/\D/g, ''))}
                                        style={{
                                            textAlign: 'center',
                                            fontSize: '1.4rem',
                                            fontWeight: 800,
                                            letterSpacing: '0.3em',
                                            height: '48px',
                                            marginBottom: '8px',
                                            color: 'var(--red)',
                                            borderColor: 'rgba(255,75,75,0.3)',
                                            background: 'var(--background)'
                                        }}
                                    />
                                    {otpError && (
                                        <div style={{ color: 'var(--red)', fontSize: '0.8rem', fontWeight: 600, marginBottom: '12px', textAlign: 'center' }}>
                                            ⚠️ {otpError}
                                        </div>
                                    )}
                                    <div style={{ display: 'flex', gap: '8px' }}>
                                        <button
                                            type="button"
                                            className="btn btn-ghost"
                                            style={{ flex: 1 }}
                                            onClick={() => {
                                                setPendingOrder(null);
                                                setOtpCode('');
                                                setOtpError('');
                                                setResult(null);
                                            }}
                                        >
                                            CANCEL
                                        </button>
                                        <button
                                            type="button"
                                            className="btn btn-red"
                                            style={{ flex: 2, fontWeight: 800 }}
                                            onClick={handleOtpSubmit}
                                            disabled={otpPlacing}
                                        >
                                            {otpPlacing ? 'VERIFYING...' : 'VERIFY & SELL'}
                                        </button>
                                    </div>
                                </div>
                            ) : (
                                <button
                                    className={`btn btn-full ${form.side === 'BUY' ? 'btn-primary' : 'btn-red'}`}
                                    style={{ padding: '1rem', fontWeight: 800, fontSize: '1.1rem', marginTop: '1rem' }}
                                    onClick={() => {
                                        if (hasInsufficientHoldings) {
                                            setShowInsufficientModal(true);
                                        } else {
                                            setShowConfirm(true);
                                        }
                                    }}
                                    disabled={!form.symbol || !form.quantity || parseInt(form.quantity) <= 0}
                                >
                                    REVIEW {form.side} ORDER →
                                </button>
                            )}
                        </div>
                    </div>
                </div>

                <div className="history-side">
                    <div className="card" style={{ padding: 0 }}>
                        <div style={{ padding: '1.5rem', borderBottom: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <h2 style={{ fontSize: '1rem', fontWeight: 800, letterSpacing: '-0.02em' }}>Past Orders</h2>
                            <button className="btn btn-ghost" style={{ fontSize: '0.65rem', padding: '6px 12px' }} onClick={fetchHistory}>REFRESH</button>
                        </div>
                        <div className="table-container" style={{ maxHeight: '600px', overflowY: 'auto' }}>
                            <table className="tf-table">
                                <thead>
                                    <tr>
                                        <th>Symbol</th>
                                        <th className="text-center">Side</th>
                                        <th className="text-right">Qty</th>
                                        <th className="text-center">Type</th>
                                        <th className="text-right">Price</th>
                                        <th className="text-center">Status</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {history.length === 0 ? (
                                        <tr>
                                            <td colSpan="6" style={{ textAlign: 'center', padding: '4rem', color: 'var(--text-muted)' }}>
                                                <div style={{ fontSize: '2rem', marginBottom: '1rem', opacity: 0.2 }}>📋</div>
                                                Start your trading journey with advanced tools.
                                            </td>
                                        </tr>
                                    ) : (
                                        history.map(order => (
                                            <tr key={order.id}>
                                                <td className="symbol-col">{order.symbol}</td>
                                                <td className="text-center">
                                                    <span style={{ 
                                                        color: order.side === 'BUY' ? 'var(--primary)' : 'var(--red)', 
                                                        fontWeight: 900,
                                                        fontSize: '0.8rem',
                                                        letterSpacing: '0.05em'
                                                    }}>
                                                        {order.side}
                                                    </span>
                                                </td>
                                                <td className="text-right" style={{ fontWeight: 600 }}>{order.quantity}</td>
                                                <td className="text-center">
                                                    <span style={{ 
                                                        fontSize: '0.65rem', 
                                                        fontWeight: 700, 
                                                        color: 'var(--text-muted)',
                                                        background: 'rgba(255,255,255,0.03)',
                                                        padding: '2px 6px',
                                                        borderRadius: '4px',
                                                        border: '1px solid var(--border)'
                                                    }}>
                                                        {order.type}
                                                    </span>
                                                </td>
                                                <td className="text-right price-col">₹{fmt(order.executedPrice || order.triggerPrice || 0)}</td>
                                                <td className="text-center">
                                                    <span className={`status-badge status-${order.status.toLowerCase()}`}>
                                                        {order.status === 'COMPLETED' && <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: 'var(--primary)', boxShadow: '0 0 6px var(--primary)' }}></span>}
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

            {/* Premium Confirmation Modal */}
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



            {/* Insufficient Holdings Warning Modal */}
            {showInsufficientModal && (
                <div className="modal-overlay" style={{ zIndex: 1200 }}>
                    <div className="card execution-modal" style={{ maxWidth: '440px', border: '1px solid rgba(255,75,75,0.2)', boxShadow: '0 0 50px rgba(255,75,75,0.15)', animation: 'fadeIn 0.3s ease' }}>
                        <div style={{ fontWeight: 800, fontSize: '1.25rem', marginBottom: '1.5rem', color: 'var(--red)', textAlign: 'center', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px' }}>
                            <span>⚠️ Execution Blocked</span>
                        </div>
                        <div style={{ padding: '0 1rem 1rem', textAlign: 'center' }}>
                            <p style={{ marginBottom: '1.5rem', color: 'var(--text-muted)', lineHeight: 1.6, fontSize: '0.9rem' }}>
                                You cannot execute a sell order for <strong>{form.quantity} shares</strong> of <strong>{form.symbol.toUpperCase()}</strong> because you only own <strong>{ownedQty} shares</strong> in your portfolio.
                            </p>
                            <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center' }}>
                                <button type="button" className="btn btn-red btn-full" onClick={() => setShowInsufficientModal(false)} style={{ fontWeight: 800 }}>
                                    ACKNOWLEDGE & FIX
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </Layout>
    );
}
