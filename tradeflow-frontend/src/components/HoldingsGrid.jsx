import React from 'react';

export default function HoldingsGrid({ holdings, marketData, prices }) {
    const totalValue = holdings.reduce((acc, h) => {
        const ltp = marketData[h.symbol] ?? prices[h.symbol] ?? h.avgPrice ?? 0;
        return acc + (ltp * (h.totalQuantity ?? 0));
    }, 0);

    const fmt = (v) => v.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

    return (
        <div className="holdings-grid">
            {holdings.slice(0, 4).map((h) => {
                const ltp = marketData[h.symbol] ?? prices[h.symbol] ?? h.avgPrice ?? 0;
                const value = ltp * (h.totalQuantity ?? 0);
                const weight = totalValue ? (value / totalValue) * 100 : 0;
                const pnl = (ltp - (h.avgPrice ?? 0)) * (h.totalQuantity ?? 0);
                const pct = h.avgPrice ? ((ltp - h.avgPrice) / h.avgPrice) * 100 : 0;

                return (
                    <div key={h.symbol} className="card asset-card">
                        <div className="asset-card-header">
                            <div className="asset-card-info">
                                <span className="symbol">{h.symbol}</span>
                                <span className="exchange">{h.exchange ?? 'NSE'}</span>
                            </div>
                            <div className={`asset-badge ${pct >= 0 ? 'up' : 'down'}`}>
                                {pct >= 0 ? '▲' : '▼'} {Math.abs(pct).toFixed(2)}%
                            </div>
                        </div>

                        <div className="asset-card-price">
                            <div className="ltp">₹{fmt(ltp)}</div>
                            <div className="qty">{h.totalQuantity} Units</div>
                        </div>

                        <div className="asset-card-footer">
                            <div className="weight-container">
                                <div className="weight-header">
                                    <span>PORTFOLIO WEIGHT</span>
                                    <span>{weight.toFixed(1)}%</span>
                                </div>
                                <div className="weight-bar-bg">
                                    <div className="weight-bar-fill" style={{ width: `${weight}%` }} />
                                </div>
                            </div>
                        </div>
                    </div>
                );
            })}
        </div>
    );
}
