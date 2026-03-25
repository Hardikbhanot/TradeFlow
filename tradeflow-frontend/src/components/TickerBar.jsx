import { useEffect, useState } from 'react';
import api from '../api/axios';

const SYMBOLS = ['RELIANCE', 'TCS', 'INFY', 'HDFCBANK', 'ICICIBANK', 'WIPRO', 'SBIN', 'BAJFINANCE'];

export default function TickerBar() {
    const [prices, setPrices] = useState({});

    useEffect(() => {
        async function fetchAll() {
            const results = {};
            await Promise.all(
                SYMBOLS.map(async (sym) => {
                    try {
                        const res = await api.get(`/api/v1/market/price/${sym}`);
                        results[sym] = res.data;
                    } catch {
                        results[sym] = null;
                    }
                })
            );
            setPrices(results);
        }
        fetchAll();
        const id = setInterval(fetchAll, 15000);
        return () => clearInterval(id);
    }, []);

    const items = SYMBOLS.map((sym) => ({
        sym,
        price: prices[sym] ?? '---',
    }));

    const doubled = [...items, ...items];

    return (
        <div className="ticker-bar">
            <div className="ticker-track">
                {doubled.map((item, i) => (
                    <span className="ticker-item" key={i}>
                        <span className="ticker-symbol">{item.sym}</span>
                        <span className="ticker-price" style={{ color: 'var(--primary)', fontWeight: 700 }}>
                            {item.price !== '---' ? `₹${Number(item.price).toLocaleString('en-IN', { minimumFractionDigits: 2 })}` : '---'}
                        </span>
                        {item.price !== '---' && (
                            <span style={{ fontSize: '0.65rem', color: 'var(--text-muted)', marginLeft: '-4px' }}>
                                +0.45%
                            </span>
                        )}
                    </span>
                ))}
            </div>
        </div>
    );
}
