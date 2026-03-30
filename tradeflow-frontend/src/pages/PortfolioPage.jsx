import { useState, useEffect, useCallback, useMemo } from 'react';
import Layout from '../components/Layout';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';
import { useWebSocket } from '../hooks/useWebSocket';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';

export default function PortfolioPage() {
    const { user } = useAuth();
    const [holdings, setHoldings] = useState([]);
    const [prices, setPrices] = useState({});
    const [loading, setLoading] = useState(true);
    const { marketData } = useWebSocket(user?.userId);

    const fetchHoldings = useCallback(async () => {
        try {
            const res = await api.get('/api/v1/portfolio');
            setHoldings(res.data);
        } catch (err) {
            console.error('Failed to fetch holdings', err);
        } finally {
            setLoading(false);
        }
    }, []);

    const fetchPrices = useCallback(async () => {
        if (!holdings.length) return;
        try {
            const symbols = holdings.map(h => h.symbol).join(',');
            const res = await api.get('/api/v1/market/prices', { params: { symbols } });
            setPrices(prev => ({ ...prev, ...res.data }));
        } catch {}
    }, [holdings]);

    useEffect(() => {
        fetchHoldings();
    }, [fetchHoldings]);

    useEffect(() => {
        fetchPrices();
        const id = setInterval(fetchPrices, 5000);
        return () => clearInterval(id);
    }, [fetchPrices]);

    const stats = useMemo(() => {
        const invested = holdings.reduce((s, h) => s + ((h.avgPrice ?? 0) * (h.totalQuantity ?? 0)), 0);
        const current = holdings.reduce((s, h) => s + (((marketData[h.symbol] ?? prices[h.symbol] ?? h.avgPrice ?? 0) * (h.totalQuantity ?? 0))), 0);
        const pnl = current - invested;
        const pnlPct = invested ? (pnl / invested) * 100 : 0;
        return { invested, current, pnl, pnlPct };
    }, [holdings, marketData, prices]);

    const chartData = useMemo(() => {
        return holdings.map(h => ({
            name: h.symbol,
            value: (marketData[h.symbol] ?? prices[h.symbol] ?? h.avgPrice ?? 0) * (h.totalQuantity ?? 0)
        })).sort((a, b) => b.value - a.value);
    }, [holdings, marketData, prices]);

    const COLORS = ['#00FFD1', '#00BFFF', '#7000FF', '#FF00A8', '#FFD600', '#FF4D4D'];

    const fmt = (n) => Number(n ?? 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

    return (
        <Layout title="Portfolio Analytics">
            <div className="page-header">
                <div>
                    <h1>Asset Holdings</h1>
                    <p>Comprehensive overview of your equity and derivative positions.</p>
                </div>
            </div>

            <div className="dashboard-grid" style={{ gridTemplateColumns: '2fr 1fr' }}>
                <div className="dashboard-left">
                    <div className="hero-card" style={{ marginBottom: '2rem', background: 'linear-gradient(135deg, rgba(0,255,209,0.05) 0%, rgba(0,191,255,0.05) 100%)' }}>
                        <div className="hero-stats-grid" style={{ gridTemplateColumns: 'repeat(3, 1fr)' }}>
                            <div className="hero-stat-item">
                                <div className="label">TOTAL INVESTED</div>
                                <div className="value">₹{fmt(stats.invested)}</div>
                            </div>
                            <div className="hero-stat-item">
                                <div className="label">TOTAL VALUE</div>
                                <div className="value">₹{fmt(stats.current)}</div>
                            </div>
                            <div className="hero-stat-item">
                                <div className="label">OVERALL P&L</div>
                                <div className={`value ${stats.pnl >= 0 ? 'green' : 'red'}`}>
                                    {stats.pnl >= 0 ? '+' : ''}₹{fmt(stats.pnl)} ({stats.pnlPct.toFixed(2)}%)
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="card" style={{ padding: 0 }}>
                        <div style={{ padding: '1.5rem', fontWeight: 800, borderBottom: '1px solid var(--border)' }}>HOLDINGS LIST</div>
                        {loading ? (
                            <div className="centered-spinner"><div className="spinner" /></div>
                        ) : holdings.length === 0 ? (
                            <div style={{ padding: '4rem', textAlign: 'center', color: 'var(--text-muted)' }}>No holdings found.</div>
                        ) : (
                            <table className="tf-table">
                                <thead>
                                    <tr>
                                        <th>INSTRUMENT</th>
                                        <th>QTY</th>
                                        <th>AVG PRICE</th>
                                        <th>LTP</th>
                                        <th>PnL</th>
                                        <th>TOTAL VALUE</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {holdings.map(h => {
                                        const ltp = marketData[h.symbol] ?? prices[h.symbol] ?? h.avgPrice;
                                        const hPnl = (ltp - h.avgPrice) * h.totalQuantity;
                                        const hPct = h.avgPrice ? ((ltp - h.avgPrice) / h.avgPrice) * 100 : 0;
                                        return (
                                            <tr key={h.id}>
                                                <td>
                                                    <div style={{ fontWeight: 700 }}>{h.symbol}</div>
                                                    <div style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>{h.exchange}</div>
                                                </td>
                                                <td style={{ fontWeight: 600 }}>{h.totalQuantity}</td>
                                                <td>₹{fmt(h.avgPrice)}</td>
                                                <td style={{ color: 'var(--primary)', fontWeight: 700 }}>₹{fmt(ltp)}</td>
                                                <td className={hPnl >= 0 ? 'green' : 'red'} style={{ fontWeight: 700 }}>
                                                    {hPnl >= 0 ? '+' : ''}₹{fmt(hPnl)}
                                                    <div style={{ fontSize: '0.65rem', opacity: 0.8 }}>({hPct.toFixed(2)}%)</div>
                                                </td>
                                                <td style={{ fontWeight: 700 }}>₹{fmt(ltp * h.totalQuantity)}</td>
                                            </tr>
                                        );
                                    })}
                                </tbody>
                            </table>
                        )}
                    </div>
                </div>

                <div className="dashboard-right">
                    <div className="card">
                        <div style={{ fontWeight: 800, marginBottom: '1.5rem' }}>DIVERSIFICATION</div>
                        <div style={{ height: '240px' }}>
                            <ResponsiveContainer width="100%" height="100%">
                                <PieChart>
                                    <Pie
                                        data={chartData}
                                        innerRadius={60}
                                        outerRadius={80}
                                        paddingAngle={5}
                                        dataKey="value"
                                    >
                                        {chartData.map((entry, index) => (
                                            <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                        ))}
                                    </Pie>
                                    <Tooltip 
                                        contentStyle={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '12px' }}
                                        formatter={(val) => `₹${fmt(val)}`}
                                    />
                                </PieChart>
                            </ResponsiveContainer>
                        </div>
                        <div style={{ marginTop: '1rem' }}>
                            {chartData.slice(0, 5).map((entry, index) => (
                                <div key={entry.name} style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem', fontSize: '0.85rem' }}>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                        <div style={{ width: '8px', height: '8px', borderRadius: '50%', background: COLORS[index % COLORS.length] }} />
                                        {entry.name}
                                    </div>
                                    <div style={{ fontWeight: 700 }}>{((entry.value / stats.current) * 100).toFixed(1)}%</div>
                                </div>
                            ))}
                        </div>
                    </div>

                    <div className="card" style={{ background: 'var(--primary-dim)', border: '1px solid var(--primary-dim)' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '1rem' }}>
                            <span style={{ fontSize: '1.5rem' }}>🛡️</span>
                            <div style={{ fontWeight: 800, fontSize: '0.85rem' }}>INSIGHT: Risk Exposure</div>
                        </div>
                        <p style={{ fontSize: '0.8rem', lineHeight: '1.5', color: 'var(--text-2)' }}>
                            Your portfolio is currently concentrated in <strong>{holdings[0]?.symbol}</strong>. 
                            Consider diversifying into broad-market ETFs or different sectors to reduce idiosyncratic risk.
                        </p>
                    </div>
                </div>
            </div>
        </Layout>
    );
}
