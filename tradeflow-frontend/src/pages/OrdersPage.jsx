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

    async function fetchPrice() {
        fetchPriceForSym(form.symbol);
    }

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

    const fmt = n => Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

    return (
        <Layout title="Place Order">
            <div className="page-header">
                <h1>Place Order</h1>
                <p>Execute trades at market or limit prices.</p>
            </div>

            <div style={{ maxWidth: 560 }}>
                {result && (
                    <div style={{
                        background: result.ok ? 'var(--green-dim)' : 'var(--red-dim)',
                        color: result.ok ? 'var(--green)' : 'var(--red)',
                        border: `1px solid ${result.ok ? 'var(--green)' : 'var(--red)'}`,
                        borderRadius: 'var(--radius)', padding: '0.75rem 1rem',
                        marginBottom: '1rem', fontSize: '0.88rem'
                    }}>
                        {result.msg}
                    </div>
                )}

                <div className="card">

                    <div className="tab-row" style={{ marginBottom: '1.25rem' }}>
                        {SIDES.map(s => (
                            <button key={s} type="button"
                                className={`order-tab ${s.toLowerCase()} ${form.side === s ? 'active' : ''}`}
                                onClick={() => setForm(f => ({ ...f, side: s }))}>
                                {s}
                            </button>
                        ))}
                    </div>

                    <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>

                        <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'flex-end', marginBottom: '1rem' }}>
                            <div className="form-group" style={{ flex: 1 }}>
                                <label>Symbol Search</label>
                                <SymbolSearch onSelect={(sym) => {
                                    setForm(f => ({ ...f, symbol: sym }));
                                    setLivePrice(null);
                                    fetchPriceForSym(sym);
                                }} />
                                {form.symbol && (
                                    <div style={{ marginTop: '6px', fontSize: '0.8rem', color: 'var(--blue)', fontWeight: 600 }}>
                                        Selected: {form.symbol}
                                    </div>
                                )}
                            </div>
                        </div>

                        {livePrice && (
                            <div style={{ background: 'var(--surface2)', borderRadius: 'var(--radius)', padding: '0.6rem 1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <span style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>Live Price</span>
                                <span style={{ fontWeight: 700, color: 'var(--green)' }}>₹{typeof livePrice === 'number' ? fmt(livePrice) : livePrice}</span>
                            </div>
                        )}

                        <div className="form-row">
                            <div className="form-group">
                                <label>Quantity</label>
                                <input className="form-input" type="number" min="1" placeholder="1" value={form.quantity}
                                    onChange={e => setForm(f => ({ ...f, quantity: e.target.value }))} />
                            </div>
                            <div className="form-group">
                                <label>Exchange</label>
                                <select className="form-input" value={form.exchange}
                                    onChange={e => setForm(f => ({ ...f, exchange: e.target.value }))}>
                                    {EXCHANGES.map(ex => <option key={ex}>{ex}</option>)}
                                </select>
                            </div>
                        </div>

                        <div className="form-group">
                            <label>Order Type</label>
                            <select className="form-input" value={form.orderType}
                                onChange={e => setForm(f => ({ ...f, orderType: e.target.value }))}>
                                {ORDER_TYPES.map(t => <option key={t}>{t}</option>)}
                            </select>
                        </div>

                        {form.orderType !== 'MARKET' && (
                            <div className="form-group">
                                <label>Price per Unit (₹)</label>
                                <input className="form-input" type="number" step="0.01" placeholder="0.00" value={form.pricePerUnit}
                                    onChange={e => setForm(f => ({ ...f, pricePerUnit: e.target.value }))} />
                            </div>
                        )}

                        {form.orderType === 'STOP_LOSS' && (
                            <div className="form-group">
                                <label>Trigger Price (₹)</label>
                                <input className="form-input" type="number" step="0.01" placeholder="0.00" value={form.triggerPrice}
                                    onChange={e => setForm(f => ({ ...f, triggerPrice: e.target.value }))} />
                            </div>
                        )}


                        <div style={{ background: 'var(--surface2)', borderRadius: 'var(--radius)', padding: '0.6rem 1rem', display: 'flex', justifyContent: 'space-between' }}>
                            <span style={{ color: 'var(--text-muted)', fontSize: '0.82rem' }}>Estimated Total</span>
                            <span style={{ fontWeight: 700 }}>₹{totalEstimate}</span>
                        </div>

                        <button
                            className={`btn btn-full ${form.side === 'BUY' ? 'btn-primary' : 'btn-red'}`}
                            onClick={() => setShowConfirm(true)}
                            disabled={!form.symbol || !form.quantity}
                        >
                            Review {form.side} Order →
                        </button>
                    </div>
                </div>
            </div>


            {showConfirm && (
                <div style={{
                    position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000
                }}>
                    <div className="card" style={{ width: 360, border: '1px solid var(--border2)' }}>
                        <div style={{ fontWeight: 700, fontSize: '1.05rem', marginBottom: '1rem' }}>
                            Confirm {form.side} Order
                        </div>
                        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                            {[
                                ['Symbol', form.symbol.toUpperCase()],
                                ['Side', form.side],
                                ['Type', form.orderType],
                                ['Quantity', form.quantity],
                                ['Exchange', form.exchange],
                                ['Est. Total', `₹${totalEstimate}`],
                            ].map(([l, v]) => (
                                <tr key={l} style={{ borderBottom: '1px solid var(--border)' }}>
                                    <td style={{ padding: '0.5rem 0', color: 'var(--text-muted)', fontSize: '0.82rem' }}>{l}</td>
                                    <td style={{ padding: '0.5rem 0', fontWeight: 600, textAlign: 'right' }}>{v}</td>
                                </tr>
                            ))}
                        </table>
                        <div style={{ display: 'flex', gap: '0.75rem', marginTop: '1.25rem' }}>
                            <button className="btn btn-ghost btn-full" onClick={() => setShowConfirm(false)}>Cancel</button>
                            <button className={`btn btn-full ${form.side === 'BUY' ? 'btn-primary' : 'btn-red'}`}
                                onClick={confirmOrder} disabled={placing}>
                                {placing ? 'Placing…' : 'Confirm'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </Layout>
    );
}
