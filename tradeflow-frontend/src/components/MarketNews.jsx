import React, { useState, useEffect } from 'react';
import api from '../api/axios';

export default function MarketNews() {
    const [news, setNews] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchNews = async () => {
            try {
                const res = await api.get('/api/v1/market/news');
                setNews(res.data);
            } catch (err) {
                console.error("Failed to fetch news", err);
                setNews([]);
            } finally {
                setLoading(false);
            }
        };
        fetchNews();
    }, []);

    if (loading) return <div className="centered-spinner" style={{ padding: '2rem' }}><div className="spinner" /></div>;

    return (
        <div className="news-list">
            {news.map(item => (
                <div key={item.id} className="news-item">
                    <div className="news-header">
                        <span className="news-source">{item.source}</span>
                        <span className="news-time">{item.time}</span>
                    </div>
                    <div className="news-title">{item.title}</div>
                    <div className={`news-sentiment ${item.sentiment}`}>
                        {item.sentiment === 'positive' ? '▲ POSITIVE' : item.sentiment === 'negative' ? '▼ NEGATIVE' : '○ NEUTRAL'}
                    </div>
                </div>
            ))}
            <button className="btn btn-full" style={{ marginTop: '1rem', fontSize: '0.75rem', background: 'var(--surface)', border: '1px solid var(--border)' }}>
                VIEW ALL NEWS
            </button>
        </div>
    );
}
