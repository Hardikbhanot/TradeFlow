import { useMemo, useState, useEffect, useRef } from 'react';

// Top Indian Stocks Dictionary
const STOCKS = [
    { symbol: 'RELIANCE', name: 'Reliance Industries Limited' },
    { symbol: 'TCS', name: 'Tata Consultancy Services Limited' },
    { symbol: 'HDFCBANK', name: 'HDFC Bank Limited' },
    { symbol: 'ICICIBANK', name: 'ICICI Bank Limited' },
    { symbol: 'INFY', name: 'Infosys Limited' },
    { symbol: 'ITC', name: 'ITC Limited' },
    { symbol: 'SBIN', name: 'State Bank of India' },
    { symbol: 'BHARTIARTL', name: 'Bharti Airtel Limited' },
    { symbol: 'BAJFINANCE', name: 'Bajaj Finance Limited' },
    { symbol: 'L&T', name: 'Larsen & Toubro Limited' },
    { symbol: 'HUL', name: 'Hindustan Unilever Limited' },
    { symbol: 'KOTAKBANK', name: 'Kotak Mahindra Bank Limited' },
    { symbol: 'AXISBANK', name: 'Axis Bank Limited' },
    { symbol: 'ASIANPAINT', name: 'Asian Paints Limited' },
    { symbol: 'MARUTI', name: 'Maruti Suzuki India Limited' },
    { symbol: 'SUNPHARMA', name: 'Sun Pharmaceutical Industries Limited' },
    { symbol: 'TITAN', name: 'Titan Company Limited' },
    { symbol: 'ULTRACEMCO', name: 'UltraTech Cement Limited' },
    { symbol: 'WIPRO', name: 'Wipro Limited' },
    { symbol: 'TATASTEEL', name: 'Tata Steel Limited' },
    { symbol: 'ADANIENT', name: 'Adani Enterprises Limited' },
    { symbol: 'NTPC', name: 'NTPC Limited' },
    { symbol: 'POWERGRID', name: 'Power Grid Corporation of India Limited' },
    { symbol: 'M&M', name: 'Mahindra & Mahindra Limited' },
    { symbol: 'ONGC', name: 'Oil & Natural Gas Corporation Limited' },
    { symbol: 'HCLTECH', name: 'HCL Technologies Limited' },
    { symbol: 'BAJAJFINSV', name: 'Bajaj Finserv Limited' },
    { symbol: 'HAL', name: 'Hindustan Aeronautics Limited' },
    { symbol: 'JSWSTEEL', name: 'JSW Steel Limited' },
    { symbol: 'COALINDIA', name: 'Coal India Limited' },
];

export default function SymbolSearch({ onSelect }) {
    const [query, setQuery] = useState('');
    const [isFocused, setIsFocused] = useState(false);
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

    const results = useMemo(() => {
        const trimmed = query.trim();
        if (!trimmed) return [];

        const lower = trimmed.toLowerCase();
        return STOCKS.filter(
            s => s.symbol.toLowerCase().includes(lower) || s.name.toLowerCase().includes(lower)
        ).slice(0, 8);
    }, [query]);

    function handleSelect(symbol) {
        setQuery('');
        setIsFocused(false);
        if (onSelect) onSelect(symbol);
    }

    const isOpen = isFocused && query.trim() && results.length > 0;

    return (
        <div ref={wrapperRef} style={{ position: 'relative', width: '100%', maxWidth: '350px' }}>
            <input
                className="form-input"
                style={{ width: '100%' }}
                type="text"
                placeholder="🔍 Search stocks (e.g. Reliance, HDFC...)"
                value={query}
                onChange={e => setQuery(e.target.value)}
                onFocus={() => setIsFocused(true)}
            />

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
                    overflow: 'hidden'
                }}>
                    {results.map(stock => (
                        <div
                            key={stock.symbol}
                            onClick={() => handleSelect(stock.symbol)}
                            style={{
                                padding: '10px 15px',
                                borderBottom: '1px solid var(--border)',
                                cursor: 'pointer',
                                transition: 'var(--transition)'
                            }}
                            onMouseEnter={e => { e.currentTarget.style.background = 'var(--surface2)'; }}
                            onMouseLeave={e => { e.currentTarget.style.background = 'transparent'; }}
                        >
                            <div style={{ fontWeight: 700, fontSize: '0.9rem' }}>{stock.symbol}</div>
                            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{stock.name}</div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
