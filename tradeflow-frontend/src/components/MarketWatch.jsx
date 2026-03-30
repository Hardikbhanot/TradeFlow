import React, { useState, useEffect } from 'react';
import api from '../api/axios';

export default function MarketWatch() {
    const [alerts, setAlerts] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchAlerts = async () => {
            try {
                const res = await api.get('/api/v1/market/news');
                // Use the first 2 as 'Alerts' for the compact Market Watch section
                setAlerts(res.data.slice(0, 2).map((item, idx) => ({
                    id: idx,
                    type: item.sentiment === 'Negative' ? 'warning' : 'info',
                    msg: item.title,
                    time: item.time
                })));
            } catch (err) {
                setAlerts([]);
            } finally {
                setLoading(false);
            }
        };
        fetchAlerts();
    }, []);

    if (loading) return <div className="spinner" style={{ width: '20px', height: '20px', margin: '1rem auto' }} />;

    return (
        <div className="market-watch">
            {alerts.length === 0 ? (
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textAlign: 'center', padding: '1rem' }}>No active alerts</div>
            ) : (
                alerts.map(a => (
                    <div key={a.id} className={`alert-item alert-${a.type}`}>
                        <div className="alert-content">
                            <span className="alert-msg">{a.msg}</span>
                            <span className="alert-time">{a.time}</span>
                        </div>
                    </div>
                ))
            )}
        </div>
    );
}
