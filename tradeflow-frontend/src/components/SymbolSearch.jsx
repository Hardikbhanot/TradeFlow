import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axios';

export default function SymbolSearch({ onSelect }) {
    const navigate = useNavigate();
    const [query, setQuery] = useState('');
    const [results, setResults] = useState([]);
    const [isFocused, setIsFocused] = useState(false);
    const [loading, setLoading] = useState(false);
    const wrapperRef = useRef(null);

    useEffect(() => {
        function handleClickOutside(event) {
            if (wrapperRef.current && !wrapperRef.current.contains(event.target)) {
                setIsFocused(false);
            }
        }
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    useEffect(() => {
        const trimmed = query.trim();
        if (trimmed.length < 2) {
            setResults([]);
            return;
        }

        const timeoutId = setTimeout(async () => {
            setLoading(true);
            try {
                const res = await api.get('/api/v1/market/search', {
                    params: { q: trimmed }
                });
                setResults(res.data || []);
            } catch (err) {
                console.error("Search failed", err);
                setResults([]);
            } finally {
                setLoading(false);
            }
        }, 300);

        return () => clearTimeout(timeoutId);
    }, [query]);

    function handleSelect(symbol) {
        setQuery('');
        setResults([]);
        setIsFocused(false);
        if (onSelect) onSelect(symbol);
    }

    const isOpen = isFocused && query.trim().length >= 2;

    return (
        <div ref={wrapperRef} style={{ position: 'relative', width: '100%', maxWidth: '350px' }}>
            <input
                className="form-input"
                style={{ width: '100%' }}
                type="text"
                placeholder="🔍 Search NSE stocks (e.g. RELIANCE, ZOMATO...)"
                value={query}
                onChange={e => setQuery(e.target.value)}
                onFocus={() => setIsFocused(true)}
            />

            {loading && (
                <div style={{ position: 'absolute', right: '10px', top: '50%', transform: 'translateY(-50%)', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                    ...
                </div>
            )}

            {isOpen && (
                <div style={{
                    position: 'absolute',
                    top: '100%',
                    left: 0,
                    right: 0,
                    background: 'var(--surface)',
                    border: '1px solid var(--border)',
                    borderRadius: 'var(--radius)',
                    marginTop: '4px',
                    zIndex: 50,
                    boxShadow: '0 10px 25px rgba(0,0,0,0.5)',
                    maxHeight: '300px',
                    overflowY: 'auto'
                }}>
                    {results.length > 0 ? (
                        results.map(symbol => (
                            <div
                                key={symbol}
                                style={{
                                    padding: '10px 15px',
                                    borderBottom: '1px solid var(--border)',
                                    display: 'flex',
                                    justifyContent: 'space-between',
                                    alignItems: 'center',
                                    cursor: 'pointer',
                                    transition: 'var(--transition)'
                                }}
                                onClick={() => handleSelect(symbol)}
                                onMouseEnter={e => { e.currentTarget.style.background = 'var(--surface2)'; }}
                                onMouseLeave={e => { e.currentTarget.style.background = 'transparent'; }}
                            >
                                <div style={{ fontWeight: 700, fontSize: '0.9rem' }}>{symbol}</div>
                                <button className="btn btn-green" style={{ padding: '2px 8px', fontSize: '0.65rem' }}
                                    onClick={(e) => {
                                        e.stopPropagation();
                                        navigate(`/orders?symbol=${symbol}`);
                                        setIsFocused(false);
                                        setQuery('');
                                    }}>
                                    BUY
                                </button>
                            </div>
                        ))
                    ) : !loading && (
                        <div style={{ padding: '15px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                            No matching symbols found
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}
