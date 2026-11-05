import React, { useState, useEffect } from 'react';
import api from '../api/axios';

export default function MarketDepth({ symbol, onPriceSelect }) {
    const [depth, setDepth] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!symbol || !symbol.trim()) {
            setDepth(null);
            return;
        }

        let isMounted = true;
        const fetchDepth = async () => {
            try {
                const res = await api.get(`/api/v1/market/depth/${symbol.trim().toUpperCase()}`);
                if (isMounted) {
                    setDepth(res.data);
                    setLoading(false);
                }
            } catch (err) {
                // Defensive fallback
                if (isMounted) setLoading(false);
            }
        };

        fetchDepth();
        const intervalId = setInterval(fetchDepth, 2500);

        return () => {
            isMounted = false;
            clearInterval(intervalId);
        };
    }, [symbol]);

    if (!symbol) return null;

    if (loading && !depth) {
        return (
            <div style={{
                background: 'var(--surface-opaque, #0f172a)',
                border: '1px solid var(--border)',
                borderRadius: 'var(--radius-lg, 12px)',
                padding: '1.5rem',
                textAlign: 'center',
                color: 'var(--text-muted)'
            }}>
                <div className="spinner" style={{ width: '24px', height: '24px', margin: '0 auto 8px auto' }} />
                <span style={{ fontSize: '0.8rem' }}>Loading market depth for {symbol}...</span>
            </div>
        );
    }

    if (!depth) return null;

    const maxBidQty = Math.max(...(depth.bids?.map(b => b.quantity) || [1]));
    const maxAskQty = Math.max(...(depth.asks?.map(a => a.quantity) || [1]));
    const maxQty = Math.max(maxBidQty, maxAskQty, 1);

    return (
        <div style={{
            background: 'var(--surface-opaque, #0f172a)',
            border: '1px solid var(--border)',
            borderRadius: 'var(--radius-lg, 12px)',
            padding: '1.25rem',
            boxShadow: '0 8px 24px rgba(0, 0, 0, 0.2)',
            fontFamily: 'Inter, sans-serif'
        }}>
            {/* Header */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span style={{ fontWeight: 700, fontSize: '0.95rem', color: 'var(--text)' }}>{depth.symbol}</span>
                    <span style={{
                        fontSize: '0.7rem',
                        fontWeight: 600,
                        padding: '2px 6px',
                        borderRadius: '4px',
                        background: 'rgba(59, 130, 246, 0.1)',
                        color: 'var(--blue, #3b82f6)'
                    }}>L2 DEPTH</span>
                    {depth.marketOpen === false && (
                        <span style={{
                            fontSize: '0.65rem',
                            fontWeight: 600,
                            padding: '2px 6px',
                            borderRadius: '4px',
                            background: 'rgba(239, 68, 68, 0.1)',
                            color: 'var(--red, #f43f5e)'
                        }}>MARKET CLOSED</span>
                    )}
                </div>
                <div style={{ textAlign: 'right' }}>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>LTP: </span>
                    <span style={{ fontWeight: 700, fontSize: '0.95rem', color: 'var(--green, #10b981)' }}>
                        ₹{Number(depth.lastPrice).toFixed(2)}
                    </span>
                </div>
            </div>

            {/* Buyer vs Seller Imbalance Bar */}
            <div style={{ marginBottom: '14px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.7rem', marginBottom: '4px' }}>
                    <span style={{ color: 'var(--green, #10b981)', fontWeight: 600 }}>Buyers: {depth.buyerPercentage}%</span>
                    <span style={{ color: 'var(--red, #f43f5e)', fontWeight: 600 }}>Sellers: {depth.sellerPercentage}%</span>
                </div>
                <div style={{ height: '5px', width: '100%', background: 'var(--red-dim, rgba(244, 63, 94, 0.2))', borderRadius: '3px', overflow: 'hidden', display: 'flex' }}>
                    <div style={{ width: `${depth.buyerPercentage}%`, background: 'var(--green, #10b981)', transition: 'width 0.4s ease' }} />
                    <div style={{ width: `${depth.sellerPercentage}%`, background: 'var(--red, #f43f5e)', transition: 'width 0.4s ease' }} />
                </div>
            </div>

            {/* 5-Level Depth Table */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
                {/* BIDS Column */}
                <div>
                    <div style={{
                        display: 'grid',
                        gridTemplateColumns: '1fr 1fr 1.2fr',
                        fontSize: '0.65rem',
                        fontWeight: 700,
                        color: 'var(--text-muted)',
                        paddingBottom: '6px',
                        borderBottom: '1px solid var(--border)',
                        textTransform: 'uppercase'
                    }}>
                        <span>Orders</span>
                        <span style={{ textAlign: 'right' }}>Qty</span>
                        <span style={{ textAlign: 'right', color: 'var(--green, #10b981)' }}>Bid</span>
                    </div>
                    {depth.bids?.map((bid, i) => {
                        const widthPct = Math.min(100, Math.round((bid.quantity / maxQty) * 100));
                        return (
                            <div
                                key={i}
                                onClick={() => onPriceSelect && onPriceSelect(Number(bid.price).toFixed(2))}
                                title="Click to fill price in order form"
                                style={{
                                    position: 'relative',
                                    display: 'grid',
                                    gridTemplateColumns: '1fr 1fr 1.2fr',
                                    fontSize: '0.75rem',
                                    padding: '5px 0',
                                    cursor: onPriceSelect ? 'pointer' : 'default',
                                    borderBottom: '1px solid rgba(255, 255, 255, 0.02)'
                                }}
                            >
                                <div style={{
                                    position: 'absolute',
                                    right: 0,
                                    top: 1,
                                    bottom: 1,
                                    width: `${widthPct}%`,
                                    background: 'rgba(16, 185, 129, 0.08)',
                                    borderRadius: '2px',
                                    pointerEvents: 'none'
                                }} />
                                <span style={{ color: 'var(--text-muted)', position: 'relative' }}>{bid.orders}</span>
                                <span style={{ textAlign: 'right', color: 'var(--text-2)', position: 'relative' }}>{bid.quantity.toLocaleString()}</span>
                                <span style={{ textAlign: 'right', fontWeight: 600, color: 'var(--green, #10b981)', position: 'relative' }}>
                                    {Number(bid.price).toFixed(2)}
                                </span>
                            </div>
                        );
                    })}
                </div>

                {/* ASKS Column */}
                <div>
                    <div style={{
                        display: 'grid',
                        gridTemplateColumns: '1.2fr 1fr 1fr',
                        fontSize: '0.65rem',
                        fontWeight: 700,
                        color: 'var(--text-muted)',
                        paddingBottom: '6px',
                        borderBottom: '1px solid var(--border)',
                        textTransform: 'uppercase'
                    }}>
                        <span style={{ color: 'var(--red, #f43f5e)' }}>Offer</span>
                        <span style={{ textAlign: 'right' }}>Qty</span>
                        <span style={{ textAlign: 'right' }}>Orders</span>
                    </div>
                    {depth.asks?.map((ask, i) => {
                        const widthPct = Math.min(100, Math.round((ask.quantity / maxQty) * 100));
                        return (
                            <div
                                key={i}
                                onClick={() => onPriceSelect && onPriceSelect(Number(ask.price).toFixed(2))}
                                title="Click to fill price in order form"
                                style={{
                                    position: 'relative',
                                    display: 'grid',
                                    gridTemplateColumns: '1.2fr 1fr 1fr',
                                    fontSize: '0.75rem',
                                    padding: '5px 0',
                                    cursor: onPriceSelect ? 'pointer' : 'default',
                                    borderBottom: '1px solid rgba(255, 255, 255, 0.02)'
                                }}
                            >
                                <div style={{
                                    position: 'absolute',
                                    left: 0,
                                    top: 1,
                                    bottom: 1,
                                    width: `${widthPct}%`,
                                    background: 'rgba(244, 63, 94, 0.08)',
                                    borderRadius: '2px',
                                    pointerEvents: 'none'
                                }} />
                                <span style={{ fontWeight: 600, color: 'var(--red, #f43f5e)', position: 'relative' }}>
                                    {Number(ask.price).toFixed(2)}
                                </span>
                                <span style={{ textAlign: 'right', color: 'var(--text-2)', position: 'relative' }}>{ask.quantity.toLocaleString()}</span>
                                <span style={{ textAlign: 'right', color: 'var(--text-muted)', position: 'relative' }}>{ask.orders}</span>
                            </div>
                        );
                    })}
                </div>
            </div>

            {/* Total Quantities Footer */}
            <div style={{
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                gap: '8px',
                marginTop: '10px',
                paddingTop: '8px',
                borderTop: '1px solid var(--border)',
                fontSize: '0.72rem',
                fontWeight: 700
            }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--green, #10b981)' }}>
                    <span>Total Bid:</span>
                    <span>{depth.totalBuyQuantity?.toLocaleString()}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--red, #f43f5e)' }}>
                    <span>Total Ask:</span>
                    <span>{depth.totalSellQuantity?.toLocaleString()}</span>
                </div>
            </div>
        </div>
    );
}
