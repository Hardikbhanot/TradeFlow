import React, { useState, useEffect } from 'react';
import axios from 'axios';

export default function MarketNews() {
    const [news, setNews] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        // Using a public proxy/aggregator for Indian Market News if possible
        // For now, using high-quality mock data that looks like real-time feed
        // as per the KINETIC mockup requirement.
        
        const mockNews = [
            { id: 1, title: "HDFC Bank shares surge 3% on strong Q4 credit growth", source: "Mint", time: "12m ago", sentiment: "positive" },
            { id: 2, title: "Nifty 50 approaches record high amid global rally", source: "ET Markets", time: "45m ago", sentiment: "neutral" },
            { id: 3, title: "Reliance Industries to invest $2B in new energy venture", source: "MoneyControl", time: "1h ago", sentiment: "positive" },
            { id: 4, title: "SEBI introduces new margin rules for intra-day traders", source: "Bloomberg", time: "2h ago", sentiment: "negative" },
            { id: 5, title: "TCS beats estimates with 15% jump in net profit", source: "CNBC TV18", time: "3h ago", sentiment: "positive" }
        ];

        setTimeout(() => {
            setNews(mockNews);
            setLoading(false);
        }, 800);
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
