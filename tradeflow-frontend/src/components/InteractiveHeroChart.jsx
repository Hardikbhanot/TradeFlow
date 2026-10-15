import React, { useState, useEffect, useRef, useMemo } from 'react';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { createChart, ColorType, CandlestickSeries, HistogramSeries } from 'lightweight-charts';
import { useTheme } from '../context/ThemeContext';

const TIMEFRAMES = ['1D', '1W', '1M', '1Y', 'ALL'];

const DEFAULT_SYMBOLS = ['NIFTY 50', 'RELIANCE', 'TCS', 'HDFCBANK', 'INFY'];

// Generate synthetic but realistic OHLC candlestick data
function generateCandleData(symbol, timeframe, basePrice = 2500) {
    const count = timeframe === '1D' ? 24 : timeframe === '1W' ? 35 : timeframe === '1M' ? 30 : 50;
    const data = [];
    const volumeData = [];

    const now = new Date();
    let currentPrice = basePrice;
    
    // Time stepping
    const stepMs = timeframe === '1D' 
        ? 15 * 60 * 1000 // 15 mins
        : timeframe === '1W'
        ? 2 * 3600 * 1000 // 2 hours
        : 24 * 3600 * 1000; // 1 day

    const startTime = new Date(now.getTime() - count * stepMs);

    for (let i = 0; i < count; i++) {
        const timePoint = new Date(startTime.getTime() + i * stepMs);
        const isoDate = timePoint.toISOString().slice(0, 10);
        const unixTime = Math.floor(timePoint.getTime() / 1000);
        const timeVal = timeframe === '1D' || timeframe === '1W' ? unixTime : isoDate;

        const delta = (Math.random() - 0.48) * (currentPrice * 0.015);
        const open = currentPrice;
        const close = parseFloat((open + delta).toFixed(2));
        const high = parseFloat((Math.max(open, close) + Math.random() * (currentPrice * 0.008)).toFixed(2));
        const low = parseFloat((Math.min(open, close) - Math.random() * (currentPrice * 0.008)).toFixed(2));
        const volume = Math.floor(Math.random() * 50000) + 15000;

        currentPrice = close;

        data.push({
            time: timeVal,
            open,
            high,
            low,
            close,
        });

        volumeData.push({
            time: timeVal,
            value: volume,
            color: close >= open ? 'rgba(34, 197, 94, 0.45)' : 'rgba(239, 68, 68, 0.45)',
        });
    }

    return { candles: data, volumes: volumeData, lastPrice: currentPrice };
}

export default function InteractiveHeroChart({ 
    currentValue = 0, 
    holdings = [], 
    prices = {} 
}) {
    const { activeThemeObj, theme } = useTheme();
    const [chartMode, setChartMode] = useState('area'); // 'area' | 'candlestick'
    const [timeframe, setTimeframe] = useState('1D');
    const [selectedSymbol, setSelectedSymbol] = useState('RELIANCE');
    const chartContainerRef = useRef(null);
    const chartInstanceRef = useRef(null);

    // Symbols available for candlestick mode
    const availableSymbols = useMemo(() => {
        const set = new Set(DEFAULT_SYMBOLS);
        holdings.forEach(h => set.add(h.symbol));
        return Array.from(set);
    }, [holdings]);

    // Calculate base price for selected symbol
    const symbolBasePrice = useMemo(() => {
        if (prices[selectedSymbol]) return Number(prices[selectedSymbol]);
        if (selectedSymbol === 'NIFTY 50') return 24850.30;
        if (selectedSymbol === 'RELIANCE') return 2948.50;
        if (selectedSymbol === 'TCS') return 3980.20;
        if (selectedSymbol === 'HDFCBANK') return 1640.75;
        if (selectedSymbol === 'INFY') return 1520.40;
        return 2000;
    }, [selectedSymbol, prices]);

    // Candlestick & Volume data
    const { candles, volumes, lastPrice } = useMemo(() => {
        return generateCandleData(selectedSymbol, timeframe, symbolBasePrice);
    }, [selectedSymbol, timeframe, symbolBasePrice]);

    // Recharts Area Chart Data (Portfolio Valuation)
    const areaData = useMemo(() => {
        const val = currentValue > 0 ? currentValue : 2485920;
        const count = timeframe === '1D' ? 7 : timeframe === '1W' ? 7 : timeframe === '1M' ? 10 : 12;
        const labels = timeframe === '1D' 
            ? ['09:30', '10:30', '11:30', '12:30', '13:30', '14:30', '15:30']
            : timeframe === '1W'
            ? ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Today']
            : timeframe === '1M'
            ? ['W1', 'W2', 'W3', 'W4', 'Today']
            : ['Jan', 'Mar', 'May', 'Jul', 'Sep', 'Nov', 'Today'];

        const multipliers = [0.965, 0.978, 0.972, 0.985, 0.981, 0.994, 1.000];

        return labels.map((lbl, idx) => {
            const m = multipliers[idx % multipliers.length];
            return {
                name: lbl,
                value: parseFloat((val * m).toFixed(2)),
            };
        });
    }, [currentValue, timeframe]);

    // Active primary color for area chart
    const primaryColor = activeThemeObj?.primaryColor || '#F59E0B';
    const isLight = theme === 'light';

    // Render lightweight-charts candlestick chart when mode === 'candlestick'
    useEffect(() => {
        if (chartMode !== 'candlestick' || !chartContainerRef.current) return;

        // Clean up any existing chart
        if (chartInstanceRef.current) {
            chartInstanceRef.current.remove();
            chartInstanceRef.current = null;
        }

        const container = chartContainerRef.current;
        const width = container.clientWidth || 800;

        const chart = createChart(container, {
            layout: {
                background: { type: ColorType.Solid, color: 'transparent' },
                textColor: isLight ? '#475569' : '#94A3B8',
                fontSize: 11,
                fontFamily: "'Inter', sans-serif",
            },
            grid: {
                vertLines: { color: isLight ? 'rgba(0,0,0,0.04)' : 'rgba(255,255,255,0.04)' },
                horzLines: { color: isLight ? 'rgba(0,0,0,0.04)' : 'rgba(255,255,255,0.04)' },
            },
            crosshair: {
                vertLine: {
                    color: primaryColor,
                    width: 1,
                    style: 3,
                    labelBackgroundColor: primaryColor,
                },
                horzLine: {
                    color: primaryColor,
                    width: 1,
                    style: 3,
                    labelBackgroundColor: primaryColor,
                },
            },
            rightPriceScale: {
                borderColor: isLight ? 'rgba(0,0,0,0.1)' : 'rgba(255,255,255,0.08)',
                visible: true,
            },
            timeScale: {
                borderColor: isLight ? 'rgba(0,0,0,0.1)' : 'rgba(255,255,255,0.08)',
                visible: true,
                timeVisible: timeframe === '1D' || timeframe === '1W',
            },
            width: width,
            height: 310,
        });

        chartInstanceRef.current = chart;

        // Add volume histogram series
        const volumeSeries = chart.addSeries(HistogramSeries, {
            priceFormat: { type: 'volume' },
            priceScaleId: '', // Set as an overlay
        });
        volumeSeries.priceScale().applyOptions({
            scaleMargins: {
                top: 0.8, // Place volume at the bottom 20%
                bottom: 0,
            },
        });
        volumeSeries.setData(volumes);

        // Add candlestick series with natural, non-neon green and crimson
        const candleSeries = chart.addSeries(CandlestickSeries, {
            upColor: '#22C55E',          // Natural crisp emerald green (Non-neon)
            downColor: '#EF4444',        // Clean crimson red (Non-neon)
            borderVisible: false,
            wickUpColor: '#22C55E',
            wickDownColor: '#EF4444',
        });
        candleSeries.setData(candles);

        chart.timeScale().fitContent();

        const handleResize = () => {
            if (chartContainerRef.current && chartInstanceRef.current) {
                chartInstanceRef.current.applyOptions({
                    width: chartContainerRef.current.clientWidth,
                });
            }
        };

        window.addEventListener('resize', handleResize);

        return () => {
            window.removeEventListener('resize', handleResize);
            if (chartInstanceRef.current) {
                chartInstanceRef.current.remove();
                chartInstanceRef.current = null;
            }
        };
    }, [chartMode, candles, volumes, isLight, primaryColor, timeframe]);

    const firstVal = areaData[0]?.value ?? 0;
    const lastVal = areaData[areaData.length - 1]?.value ?? 0;
    const isUp = lastVal >= firstVal;
    const areaStroke = primaryColor;

    return (
        <div className="interactive-chart-wrapper" style={{ marginTop: '1.25rem' }}>
            {/* Chart Sub-Header Controls */}
            <div className="chart-controls-bar" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px', marginBottom: '1rem' }}>
                
                {/* Left: Mode Toggle (Area vs Candlesticks) */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <div className="segmented-switch">
                        <button 
                            type="button"
                            className={`switch-btn ${chartMode === 'area' ? 'active' : ''}`}
                            onClick={() => setChartMode('area')}
                        >
                            <span>📈</span> Area Trend
                        </button>
                        <button 
                            type="button"
                            className={`switch-btn ${chartMode === 'candlestick' ? 'active' : ''}`}
                            onClick={() => setChartMode('candlestick')}
                        >
                            <span>🕯️</span> Candlesticks
                        </button>
                    </div>

                    {/* Symbol Selector when in Candlestick Mode */}
                    {chartMode === 'candlestick' && (
                        <select 
                            className="chart-symbol-select"
                            value={selectedSymbol}
                            onChange={(e) => setSelectedSymbol(e.target.value)}
                        >
                            {availableSymbols.map((s) => (
                                <option key={s} value={s}>{s}</option>
                            ))}
                        </select>
                    )}
                </div>

                {/* Right: Timeframe Selectors */}
                <div className="timeframe-pill-group">
                    {TIMEFRAMES.map((tf) => (
                        <button
                            key={tf}
                            type="button"
                            className={`timeframe-btn ${timeframe === tf ? 'active' : ''}`}
                            onClick={() => setTimeframe(tf)}
                        >
                            {tf}
                        </button>
                    ))}
                </div>
            </div>

            {/* Main Chart Rendering Container */}
            <div className="chart-display-surface" style={{ height: '310px', position: 'relative' }}>
                {chartMode === 'area' ? (
                    <ResponsiveContainer width="100%" height="100%">
                        <AreaChart data={areaData} margin={{ top: 10, right: 10, left: 10, bottom: 0 }}>
                            <defs>
                                <linearGradient id="heroGradient" x1="0" y1="0" x2="0" y2="1">
                                    <stop offset="5%" stopColor={areaStroke} stopOpacity={0.28}/>
                                    <stop offset="95%" stopColor={areaStroke} stopOpacity={0.0}/>
                                </linearGradient>
                            </defs>
                            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="rgba(255,255,255,0.04)" />
                            <XAxis 
                                dataKey="name" 
                                axisLine={false} 
                                tickLine={false} 
                                tick={{ fill: 'var(--text-muted)', fontSize: 11 }} 
                                dy={8}
                            />
                            <YAxis hide domain={['dataMin * 0.98', 'dataMax * 1.02']} />
                            <Tooltip 
                                content={({ active, payload }) => {
                                    if (active && payload && payload.length) {
                                        return (
                                            <div className="chart-tooltip">
                                                <div className="tooltip-time">{payload[0].payload.name}</div>
                                                <div className="tooltip-val" style={{ color: areaStroke }}>
                                                    ₹{Number(payload[0].value).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                                                </div>
                                            </div>
                                        );
                                    }
                                    return null;
                                }}
                                cursor={{ stroke: areaStroke, strokeWidth: 1, strokeDasharray: '4 4' }}
                            />
                            <Area 
                                type="monotone" 
                                dataKey="value" 
                                stroke={areaStroke} 
                                strokeWidth={2.5} 
                                fillOpacity={1} 
                                fill="url(#heroGradient)" 
                                animationDuration={800}
                            />
                        </AreaChart>
                    </ResponsiveContainer>
                ) : (
                    <div 
                        ref={chartContainerRef} 
                        style={{ width: '100%', height: '310px' }} 
                    />
                )}
            </div>
        </div>
    );
}
