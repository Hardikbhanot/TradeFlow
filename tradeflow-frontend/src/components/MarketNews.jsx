import React, { useState, useEffect } from 'react';
import api from '../api/axios';

export default function MarketNews() {
    const [news, setNews] = useState([]);
    const [loading, setLoading] = useState(true);
    const [visibleCount, setVisibleCount] = useState(4);

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

    const hasMore = visibleCount < news.length;

    return (
        <div className="news-list">
            <style>{`
                .news-link-wrapper {
                    text-decoration: none;
                    color: inherit;
                    display: block;
                    transition: transform 0.2s ease, background 0.2s ease;
                    border-radius: 8px;
                }
                .news-link-wrapper:hover {
                    transform: translateY(-2px);
                    background: rgba(255, 255, 255, 0.02);
                }
            `}</style>

            {news.slice(0, visibleCount).map((item, idx) => {
                const sentiment = item.sentiment ? item.sentiment.toLowerCase() : 'neutral';
                const articleUrl = item.link || 'https://news.google.com';
                
                return (
                    <a 
                        key={item.id || idx} 
                        href={articleUrl} 
                        target="_blank" 
                        rel="noopener noreferrer" 
                        className="news-link-wrapper"
                    >
                        <div className="news-item" style={{ cursor: 'pointer' }}>
                            <div className="news-header">
                                <span className="news-source">{item.source}</span>
                                <span className="news-time">{item.time}</span>
                            </div>
                            <div className="news-title">{item.title}</div>
                            <div className={`news-sentiment ${sentiment}`}>
                                {sentiment === 'positive' ? '▲ POSITIVE' : sentiment === 'negative' ? '▼ NEGATIVE' : '○ NEUTRAL'}
                            </div>
                        </div>
                    </a>
                );
            })}

            {news.length > 4 && (
                <button 
                    className="btn btn-full" 
                    style={{ 
                        marginTop: '1rem', 
                        fontSize: '0.75rem', 
                        background: 'var(--surface)', 
                        border: '1px solid var(--border)',
                        fontWeight: 800,
                        letterSpacing: '0.05em'
                    }}
                    onClick={() => {
                        if (hasMore) {
                            setVisibleCount(prev => Math.min(prev + 4, news.length));
                        } else {
                            setVisibleCount(4);
                        }
                    }}
                >
                    {hasMore ? `LOAD MORE NEWS (${news.length - visibleCount} LEFT)` : 'COLLAPSE NEWS FEED'}
                </button>
            )}
        </div>
    );
}
