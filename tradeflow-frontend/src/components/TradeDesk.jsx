import React, { useEffect, useRef } from 'react';
import { createChart } from 'lightweight-charts';

export default function TradeDesk({ symbol = 'RELIANCE', exchange = 'NSE' }) {
    const chartContainerRef = useRef();

    useEffect(() => {
        if (!chartContainerRef.current) return;

        let chart;
        // Introduce a minor delay to let the DOM paint and calculate layout dimensions correctly,
        // preventing the lightweight-charts zero-width initialization bug in conditional rendering contexts.
        const timer = setTimeout(() => {
            const container = chartContainerRef.current;
            if (!container) return;

            const width = container.clientWidth || 800;

            chart = createChart(container, {
                layout: {
                    background: { color: 'transparent' },
                    textColor: '#9BA3AF',
                },
                grid: {
                    vertLines: { color: 'rgba(255, 255, 255, 0.05)' },
                    horzLines: { color: 'rgba(255, 255, 255, 0.05)' },
                },
                width: width,
                height: 500,
            });

            const candlestickSeries = chart.addCandlestickSeries({
                upColor: '#00FFD1',
                downColor: '#FF4159',
                borderVisible: false,
                wickUpColor: '#00FFD1',
                wickDownColor: '#FF4159',
            });

            // Mock historical data for KINETIC visualization
            const mockData = [
                { time: '2024-03-20', open: 2450.45, high: 2480.00, low: 2445.00, close: 2472.15 },
                { time: '2024-03-21', open: 2472.15, high: 2495.00, low: 2460.00, close: 2488.30 },
                { time: '2024-03-22', open: 2488.30, high: 2510.00, low: 2480.00, close: 2502.45 },
                { time: '2024-03-23', open: 2502.45, high: 2505.00, low: 2470.00, close: 2475.20 },
                { time: '2024-03-24', open: 2475.20, high: 2490.00, low: 2465.00, close: 2482.10 },
                { time: '2024-03-25', open: 2482.10, high: 2520.00, low: 2480.00, close: 2514.85 },
                { time: '2024-03-26', open: 2514.85, high: 2545.00, low: 2510.00, close: 2538.40 },
            ];

            candlestickSeries.setData(mockData);
        }, 60);

        const handleResize = () => {
            if (chart && chartContainerRef.current) {
                chart.applyOptions({ width: chartContainerRef.current.clientWidth });
            }
        };

        window.addEventListener('resize', handleResize);

        return () => {
            clearTimeout(timer);
            window.removeEventListener('resize', handleResize);
            if (chart) {
                chart.remove();
            }
        };
    }, []);

    return (
        <div style={{ position: 'relative' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
                <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
                    <span style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--text)' }}>{symbol}</span>
                    <span className="badge badge-primary">{exchange}</span>
                    <span style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--primary)' }}>₹2,538.40</span>
                    <span className="green" style={{ fontSize: '0.85rem' }}>+1.2% ▲</span>
                </div>
                <div className="tab-row" style={{ width: 'auto', background: 'var(--surface)', padding: '4px', borderRadius: '8px' }}>
                    <button className="btn" style={{ fontSize: '0.7rem', padding: '6px 12px', background: 'var(--primary-dim)', color: 'var(--primary)' }}>1D</button>
                    <button className="btn" style={{ fontSize: '0.7rem', padding: '6px 12px', background: 'transparent', color: 'var(--text-muted)' }}>1W</button>
                    <button className="btn" style={{ fontSize: '0.7rem', padding: '6px 12px', background: 'transparent', color: 'var(--text-muted)' }}>1M</button>
                    <button className="btn" style={{ fontSize: '0.7rem', padding: '6px 12px', background: 'transparent', color: 'var(--text-muted)' }}>1Y</button>
                </div>
            </div>
            <div ref={chartContainerRef} style={{ width: '100%', height: '500px', borderRadius: '12px', overflow: 'hidden', border: '1px solid var(--border)' }} />
        </div>
    );
}
