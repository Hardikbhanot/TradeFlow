import { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import SymbolSearch from '../components/SymbolSearch';
import api from '../api/axios';
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts';
import { useAuth } from '../context/AuthContext';

const POPULAR = [
    { sym: 'RELIANCE', name: 'Reliance Industries' },
    { sym: 'TCS', name: 'Tata Consultancy' },
    { sym: 'INFY', name: 'Infosys' },
    { sym: 'HDFCBANK', name: 'HDFC Bank' },
    { sym: 'ICICIBANK', name: 'ICICI Bank' },
    { sym: 'WIPRO', name: 'Wipro' },
    { sym: 'SBIN', name: 'State Bank of India' },
    { sym: 'BAJFINANCE', name: 'Bajaj Finance' },
];

const DEFAULT_WATCHLIST = ['RELIANCE', 'TCS'];
const RANGE_OPTIONS = [
    { key: 'TODAY', label: 'Today' },
    { key: '30D', label: '30D' },
    { key: '60D', label: '60D' },
];

export default function MarketPage() {
    const { user } = useAuth();
    const navigate = useNavigate();
    const [prices, setPrices] = useState({});
    const [selected, setSelected] = useState('RELIANCE');
    const [chartData, setChartData] = useState([]);
    const [chartRange, setChartRange] = useState('TODAY');
    const [chartLoading, setChartLoading] = useState(false);
    const [watchlist, setWatchlist] = useState([]);

    useEffect(() => {
        async function fetchWatchlist() {
            try {
                const res = await api.get('/api/v1/watchlist');
                if (Array.isArray(res.data)) {
                    setWatchlist(res.data);
                }
            } catch (err) {
                console.error("Failed to fetch watchlist", err);
            }
        }
        fetchWatchlist();
    }, []);

    const allSymbols = useMemo(
        () => Array.from(new Set([...watchlist, ...POPULAR.map((p) => p.sym)])),
        [watchlist]
    );

    const fmt = (n) => n == null ? '---' : Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });


    useEffect(() => {
        if (allSymbols.length === 0) return undefined;
        let cancelled = false;

        async function fetchAllPrices() {
            try {
                const res = await api.get('/api/v1/market/prices', {
                    params: { symbols: allSymbols.join(',') },
                });

                if (cancelled) return;

                const next = {};
                allSymbols.forEach((sym) => {
                    const raw = res.data?.[sym];
                    next[sym] = raw == null ? null : Number(raw);
                });
                setPrices(next);
            } catch {

            }
        }

        fetchAllPrices();
        const id = setInterval(fetchAllPrices, 10000);
        return () => {
            cancelled = true;
            clearInterval(id);
        };
    }, [allSymbols]);


    useEffect(() => {
        let cancelled = false;

        async function fetchHistory() {
            setChartLoading(true);
            try {
                const res = await api.get(`/api/v1/market/history/${selected}`, {
                    params: { range: chartRange },
                });
                if (cancelled) return;
                const points = Array.isArray(res.data)
                    ? res.data
                        .map((point) => ({
                            label: point.label,
                            price: Number(point.price),
                        }))
                        .filter((point) => point.label && Number.isFinite(point.price))
                    : [];
                setChartData(points);
            } catch {
                if (!cancelled) {
                    setChartData([]);
                }
            } finally {
                if (!cancelled) {
                    setChartLoading(false);
                }
            }
        }

        fetchHistory();
        const id = chartRange === 'TODAY' ? setInterval(fetchHistory, 30000) : null;

        return () => {
            cancelled = true;
            if (id) clearInterval(id);
        };
    }, [selected, chartRange]);

    async function handleSearchSelect(symbol) {
        const normalized = String(symbol).toUpperCase();
        if (!watchlist.includes(normalized)) {
            try {
                await api.post(`/api/v1/watchlist/${normalized}`);
                setWatchlist((w) => [...w, normalized]);
            } catch (err) {
                console.error("Failed to add to watchlist", err);
            }
        }
        setSelected(normalized);
    }

    async function removeWatchlist(e, symbol) {
        e.stopPropagation();
        try {
            await api.delete(`/api/v1/watchlist/${symbol}`);
            setWatchlist((w) => {
                const next = w.filter((s) => s !== symbol);
                if (selected === symbol) {
                    setSelected(next[0] ?? 'RELIANCE');
                }
                return next;
            });
        } catch (err) {
            console.error("Failed to remove from watchlist", err);
        }
    }

    return (
        <Layout title="Market">
            <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                    <h1>Market & Watchlist</h1>
                    <p>Watchlist prices auto-refresh every 10s for your account.</p>
                </div>
                <div style={{ width: '350px' }}>
                    <SymbolSearch onSelect={handleSearchSelect} />
                </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1fr) 350px', gap: '1.5rem', marginBottom: '1.5rem' }}>

                <div className="card">
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                        <div>
                            <span style={{ fontWeight: 600, fontSize: '1.2rem' }}>{selected}</span>
                            <span style={{ color: 'var(--text-muted)', fontSize: '0.8rem', marginLeft: 8 }}>
                                {RANGE_OPTIONS.find((option) => option.key === chartRange)?.label} trend
                            </span>
                        </div>
                        <span style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--green)' }}>
                            ₹{fmt(prices[selected])}
                        </span>
                    </div>
                    <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
                        {RANGE_OPTIONS.map((option) => (
                            <button
                                key={option.key}
                                type="button"
                                className={chartRange === option.key ? 'btn btn-blue' : 'btn btn-ghost'}
                                style={{ padding: '0.35rem 0.75rem', fontSize: '0.78rem' }}
                                onClick={() => setChartRange(option.key)}
                            >
                                {option.label}
                            </button>
                        ))}
                    </div>
                    {chartLoading ? (
                        <div className="centered-spinner"><div className="spinner" /></div>
                    ) : chartData.length < 2 ? (
                        <div style={{ color: 'var(--text-muted)', textAlign: 'center', padding: '2rem 0' }}>
                            Not enough chart data available for this range.
                        </div>
                    ) : (
                        <ResponsiveContainer width="100%" height={260}>
                            <LineChart data={chartData}>
                                <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
                                <XAxis dataKey="label" tick={{ fill: 'var(--text-muted)', fontSize: 11 }} />
                                <YAxis domain={['auto', 'auto']} tick={{ fill: 'var(--text-muted)', fontSize: 11 }} width={65}
                                    tickFormatter={v => `₹${v.toLocaleString('en-IN')}`} />
                                <Tooltip
                                    contentStyle={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 8 }}
                                    labelStyle={{ color: 'var(--text-muted)' }}
                                    formatter={v => [`₹${fmt(v)}`, 'Price']}
                                />
                                <Line type="monotone" dataKey="price" stroke="var(--blue)" strokeWidth={3} dot={false} isAnimationActive={false} />
                            </LineChart>
                        </ResponsiveContainer>
                    )}
                </div>


                <div className="card" style={{ padding: '0', display: 'flex', flexDirection: 'column', height: '100%' }}>
                    <div style={{ padding: '1rem', borderBottom: '1px solid var(--border)', fontWeight: 600 }}>My Watchlist</div>
                    <div style={{ flex: 1, overflowY: 'auto' }}>
                        {watchlist.length === 0 ? (
                            <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                                Use the search bar to add stocks to your watchlist.
                            </div>
                        ) : (
                            watchlist.map(sym => (
                                <div key={sym}
                                    style={{
                                        padding: '1rem', borderBottom: '1px solid var(--border)',
                                        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                                        cursor: 'pointer', background: selected === sym ? 'var(--surface2)' : 'transparent'
                                    }}
                                    onClick={() => setSelected(sym)}
                                >
                                    <div>
                                        <div style={{ fontWeight: 600 }}>{sym}</div>
                                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                                            {prices[sym] != null ? `₹${fmt(prices[sym])}` : '---'}
                                        </div>
                                    </div>
                                    <div style={{ display: 'flex', gap: '8px' }}>
                                        <button className="btn btn-green" style={{ padding: '4px 10px', fontSize: '0.7rem' }}
                                            onClick={(e) => { e.stopPropagation(); navigate(`/orders?symbol=${sym}`); }}>
                                            BUY
                                        </button>
                                        <button className="btn btn-ghost" style={{ padding: '4px 8px', fontSize: '0.7rem' }}
                                            onClick={(e) => removeWatchlist(e, sym)} title="Remove">✕</button>
                                    </div>
                                </div>
                            ))
                        )}
                    </div>
                </div>
            </div>


            <h3 style={{ fontSize: '1rem', marginBottom: '1rem', color: 'var(--text-muted)' }}>Popular Stocks</h3>
            <div className="market-grid">
                {POPULAR.map(({ sym, name }) => (
                    <div key={sym} className="market-card" onClick={() => setSelected(sym)}
                        style={{ borderColor: selected === sym ? 'var(--blue)' : undefined }}>
                        <div className="market-symbol">{sym}</div>
                        <div className="market-name">{name}</div>
                        <div className="market-price" style={{ color: 'var(--green)', display: 'flex', alignItems: 'center', gap: '8px' }}>
                            {prices[sym] != null ? `₹${fmt(prices[sym])}` : <span className="spinner" style={{ width: 16, height: 16, borderWidth: 2, display: 'inline-block' }} />}
                            <button className="btn btn-green" style={{ padding: '2px 8px', fontSize: '0.65rem' }}
                                onClick={(e) => { e.stopPropagation(); navigate(`/orders?symbol=${sym}`); }}>
                                BUY
                            </button>
                        </div>
                    </div>
                ))}
            </div>
        </Layout>
    );
}
