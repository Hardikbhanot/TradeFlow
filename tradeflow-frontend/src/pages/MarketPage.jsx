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
            } catch {}
        }
        fetchWatchlist();
    }, []);

    const allSymbols = useMemo(
        () => Array.from(new Set([...watchlist, ...POPULAR.map((p) => p.sym)])),
        [watchlist]
    );

    const fmt = (n) => n == null ? '---' : Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2 });

    useEffect(() => {
        if (allSymbols.length === 0) return;
        async function fetchAllPrices() {
            try {
                const res = await api.get('/api/v1/market/prices', {
                    params: { symbols: allSymbols.join(',') },
                });
                const next = {};
                allSymbols.forEach((sym) => {
                    next[sym] = res.data?.[sym] == null ? null : Number(res.data[sym]);
                });
                setPrices(next);
            } catch {}
        }
        fetchAllPrices();
        const id = setInterval(fetchAllPrices, 2000);
        return () => clearInterval(id);
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
                setChartData(Array.isArray(res.data) ? res.data : []);
            } catch {
                if (!cancelled) setChartData([]);
            } finally {
                if (!cancelled) setChartLoading(false);
            }
        }
        fetchHistory();
        return () => { cancelled = true; };
    }, [selected, chartRange]);

    async function handleSearchSelect(symbol) {
        const normalized = String(symbol).toUpperCase();
        if (!watchlist.includes(normalized)) {
            try {
                await api.post(`/api/v1/watchlist/${normalized}`);
                setWatchlist((w) => [...w, normalized]);
            } catch {}
        }
        setSelected(normalized);
    }

    async function removeWatchlist(e, symbol) {
        e.stopPropagation();
        try {
            await api.delete(`/api/v1/watchlist/${symbol}`);
            setWatchlist((w) => w.filter((s) => s !== symbol));
            if (selected === symbol) setSelected(watchlist[0] || 'RELIANCE');
        } catch {}
    }

    return (
        <Layout title="Market">
            <div className="page-header">
                <div>
                    <h1>Market Intelligence</h1>
                    <p>Track your favorite assets with KINETIC real-time feeds.</p>
                </div>
                <div className="header-search-container">
                    <SymbolSearch onSelect={handleSearchSelect} />
                </div>
            </div>

            <div className="dashboard-grid">
                {/* Main Chart Card */}
                <div className="card">
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
                        <div>
                            <div style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--primary)' }}>{selected}</div>
                            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>NSE • {chartRange}</div>
                        </div>
                        <div style={{ textAlign: 'right' }}>
                            <div style={{ fontSize: '1.5rem', fontWeight: 800 }}>₹{fmt(prices[selected])}</div>
                            <div className="badge badge-green">LIVE UPDATES</div>
                        </div>
                    </div>

                    <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '2rem' }}>
                        {RANGE_OPTIONS.map(r => (
                            <button
                                key={r.key}
                                className={`btn ${chartRange === r.key ? 'btn-primary' : 'btn-ghost'}`}
                                style={{ padding: '6px 16px', fontSize: '0.75rem' }}
                                onClick={() => setChartRange(r.key)}
                            >
                                {r.label}
                            </button>
                        ))}
                    </div>

                    {chartLoading ? (
                        <div className="centered-spinner"><div className="spinner" /></div>
                    ) : (
                        <ResponsiveContainer width="100%" height={300}>
                            <LineChart data={chartData}>
                                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" vertical={false} />
                                <XAxis dataKey="label" hide />
                                <YAxis domain={['auto', 'auto']} hide />
                                <Tooltip
                                    contentStyle={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 12 }}
                                    formatter={v => [`₹${fmt(v)}`, 'Price']}
                                />
                                <Line 
                                    type="monotone" 
                                    dataKey="price" 
                                    stroke={(chartData.length > 1 && chartData[chartData.length-1].price < chartData[0].price) ? 'var(--red)' : 'var(--primary)'} 
                                    strokeWidth={3} 
                                    dot={false} 
                                    isAnimationActive={false} 
                                />
                            </LineChart>
                        </ResponsiveContainer>
                    )}
                </div>

                {/* Watchlist Sidebar */}
                <div className="card" style={{ padding: 0 }}>
                    <div style={{ padding: '1.25rem', borderBottom: '1px solid var(--border)', fontWeight: 800 }}>MY WATCHLIST</div>
                    <div style={{ overflowY: 'auto', maxHeight: '500px' }}>
                        {watchlist.map(sym => (
                            <div 
                                key={sym}
                                onClick={() => setSelected(sym)}
                                style={{
                                    padding: '1.25rem', borderBottom: '1px solid var(--border)',
                                    background: selected === sym ? 'rgba(0, 255, 209, 0.05)' : 'transparent',
                                    cursor: 'pointer', display: 'flex', justifyContent: 'space-between', alignItems: 'center'
                                }}
                            >
                                <div>
                                    <div style={{ fontWeight: 700 }}>{sym}</div>
                                    <div style={{ fontSize: '0.8rem', color: prices[sym] ? 'var(--green)' : 'var(--text-muted)' }}>
                                        ₹{fmt(prices[sym])}
                                    </div>
                                </div>
                                <div style={{ display: 'flex', gap: '4px' }}>
                                    <button className="btn btn-green" style={{ padding: '4px 8px', fontSize: '0.65rem' }} onClick={() => navigate(`/orders?symbol=${sym}`)}>BUY</button>
                                    <button className="btn btn-ghost" style={{ padding: '4px 8px', fontSize: '0.65rem' }} onClick={(e) => removeWatchlist(e, sym)}>✕</button>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>

            <div style={{ marginTop: '2.5rem' }}>
                <h2 style={{ fontSize: '1.2rem', marginBottom: '1.5rem' }}>Popular Assets</h2>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))', gap: '1rem' }}>
                    {POPULAR.map(({ sym, name }) => (
                        <div key={sym} className="card" onClick={() => setSelected(sym)} style={{ cursor: 'pointer', border: selected === sym ? '1px solid var(--primary)' : '1px solid var(--border)' }}>
                            <div style={{ fontWeight: 800, color: 'var(--primary)' }}>{sym}</div>
                            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '1rem' }}>{name}</div>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <span style={{ fontWeight: 700 }}>₹{fmt(prices[sym])}</span>
                                <button className="btn btn-green" style={{ height: '30px', fontSize: '0.7rem' }} onClick={() => navigate(`/orders?symbol=${sym}`)}>TRADE</button>
                            </div>
                        </div>
                    ))}
                </div>
            </div>
        </Layout>
    );
}
