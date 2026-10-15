import { Link } from 'react-router-dom';

export default function LandingPage() {
    return (
        <div className="landing-page">
            {/* Top Navigation */}
            <nav className="landing-nav">
                <div className="logo">
                    <img src="/favicon.svg" alt="TradeFlow Logo" style={{ width: '30px', height: '30px', borderRadius: '8px' }} />
                    <span>TRADEFLOW</span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                    <Link to="/login" className="btn btn-ghost" style={{ padding: '0.6rem 1.35rem', fontSize: '0.88rem' }}>
                        Sign In
                    </Link>
                    <Link 
                        to="/register" 
                        className="btn btn-primary" 
                        style={{ 
                            padding: '0.6rem 1.5rem', 
                            fontSize: '0.88rem', 
                            fontWeight: 800
                        }}
                    >
                        Get Started Free
                    </Link>
                </div>
            </nav>

            {/* Hero Section */}
            <div className="landing-hero">
                <div className="landing-hero-content">
                    <div className="landing-pill">
                        <span style={{ width: '7px', height: '7px', borderRadius: '50%', background: 'var(--green)', boxShadow: '0 0 8px var(--green)', display: 'inline-block' }} />
                        Real-Time NSE &amp; BSE Market Intelligence
                    </div>

                    <h1 className="landing-hero-title">
                        Next-Generation <br />
                        <span>Market Intelligence</span>
                    </h1>

                    <p className="landing-hero-subtitle">
                        Deploy virtual capital with institutional-grade order execution, real-time depth of market, and Gemini-powered AI portfolio intelligence built for ambitious traders.
                    </p>

                    <div className="landing-hero-actions">
                        <Link 
                            to="/register" 
                            className="btn btn-primary" 
                            style={{ 
                                padding: '0.9rem 2.2rem', 
                                fontSize: '1rem', 
                                fontWeight: 800
                            }}
                        >
                            Launch Trade Desk →
                        </Link>
                        <Link 
                            to="/login" 
                            className="btn btn-ghost" 
                            style={{ padding: '0.9rem 1.8rem', fontSize: '0.95rem', fontWeight: 600 }}
                        >
                            Explore Terminal
                        </Link>
                    </div>

                    <div className="landing-trust-row">
                        <div className="landing-trust-item">
                            <span style={{ color: 'var(--green)' }}>✓</span> 2FA OTP Email Guard
                        </div>
                        <div className="landing-trust-item">
                            <span style={{ color: 'var(--primary)' }}>⚡</span> Event-Driven Microservices
                        </div>
                        <div className="landing-trust-item">
                            <span style={{ color: 'var(--blue)' }}>✨</span> Gemini AI Analytics
                        </div>
                    </div>
                </div>

                {/* Hero Visual: Terminal Observatory Card with Authentic Candlestick Chart */}
                <div className="landing-hero-visual">
                    <div className="glow-orb" />
                    
                    <div className="landing-mockup">
                        {/* Terminal Window Header */}
                        <div className="mockup-window-bar">
                            <div className="mockup-dots">
                                <div className="mockup-dot" style={{ background: '#EF4444' }} />
                                <div className="mockup-dot" style={{ background: '#F59E0B' }} />
                                <div className="mockup-dot" style={{ background: '#22C55E' }} />
                            </div>
                            <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', fontFamily: 'monospace', letterSpacing: '0.04em' }}>
                                tradeflow.terminal ~ session-live
                            </div>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                                <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: 'var(--green)' }} />
                                <span style={{ fontSize: '0.68rem', color: 'var(--green)', fontWeight: 700 }}>12ms</span>
                            </div>
                        </div>

                        {/* Balance & Returns */}
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: '1rem' }}>
                            <div>
                                <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.08em', marginBottom: '2px' }}>
                                    Total Net Worth
                                </div>
                                <div style={{ fontSize: '2.4rem', fontWeight: 800, color: 'var(--text)', fontFamily: 'Outfit, sans-serif', letterSpacing: '-0.03em' }}>
                                    ₹2,48,590.50
                                </div>
                            </div>
                            <div style={{ textAlign: 'right' }}>
                                <div style={{ fontSize: '0.95rem', fontWeight: 800, color: 'var(--green)', background: 'var(--green-dim)', padding: '2px 8px', borderRadius: '6px' }}>
                                    +18.4%
                                </div>
                                <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                                    All-Time Profit
                                </div>
                            </div>
                        </div>

                        {/* Candlestick & Volume Mini SVG Chart */}
                        <div style={{ background: 'rgba(0,0,0,0.25)', borderRadius: '12px', padding: '10px 12px 6px', border: '1px solid var(--border)', marginBottom: '1.25rem' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                                <span style={{ fontSize: '0.72rem', fontWeight: 800, color: 'var(--text-2)' }}>NIFTY 50 • INTRADAY</span>
                                <span style={{ fontSize: '0.7rem', fontWeight: 700, color: 'var(--primary)' }}>24,850.30 ▲</span>
                            </div>
                            <svg viewBox="0 0 360 85" width="100%" height="85">
                                {/* Grid lines */}
                                <line x1="0" y1="20" x2="360" y2="20" stroke="rgba(255,255,255,0.04)" strokeDasharray="3 3" />
                                <line x1="0" y1="50" x2="360" y2="50" stroke="rgba(255,255,255,0.04)" strokeDasharray="3 3" />
                                
                                {/* Candles: x, wickY1, wickY2, bodyY, bodyH, isUp */}
                                {/* Candle 1 (Up) */}
                                <line x1="20" y1="35" x2="20" y2="65" stroke="#22C55E" strokeWidth="1" />
                                <rect x="15" y="42" width="10" height="15" rx="1" fill="#22C55E" />
                                
                                {/* Candle 2 (Down) */}
                                <line x1="50" y1="40" x2="50" y2="70" stroke="#EF4444" strokeWidth="1" />
                                <rect x="45" y="45" width="10" height="18" rx="1" fill="#EF4444" />

                                {/* Candle 3 (Up) */}
                                <line x1="80" y1="30" x2="80" y2="60" stroke="#22C55E" strokeWidth="1" />
                                <rect x="75" y="38" width="10" height="14" rx="1" fill="#22C55E" />

                                {/* Candle 4 (Up) */}
                                <line x1="110" y1="25" x2="110" y2="55" stroke="#22C55E" strokeWidth="1" />
                                <rect x="105" y="30" width="10" height="16" rx="1" fill="#22C55E" />

                                {/* Candle 5 (Down) */}
                                <line x1="140" y1="28" x2="140" y2="58" stroke="#EF4444" strokeWidth="1" />
                                <rect x="135" y="32" width="10" height="14" rx="1" fill="#EF4444" />

                                {/* Candle 6 (Up) */}
                                <line x1="170" y1="20" x2="170" y2="50" stroke="#22C55E" strokeWidth="1" />
                                <rect x="165" y="24" width="10" height="18" rx="1" fill="#22C55E" />

                                {/* Candle 7 (Up) */}
                                <line x1="200" y1="15" x2="200" y2="45" stroke="#22C55E" strokeWidth="1" />
                                <rect x="195" y="18" width="10" height="20" rx="1" fill="#22C55E" />

                                {/* Candle 8 (Down) */}
                                <line x1="230" y1="22" x2="230" y2="52" stroke="#EF4444" strokeWidth="1" />
                                <rect x="225" y="26" width="10" height="15" rx="1" fill="#EF4444" />

                                {/* Candle 9 (Up) */}
                                <line x1="260" y1="12" x2="260" y2="40" stroke="#22C55E" strokeWidth="1" />
                                <rect x="255" y="16" width="10" height="18" rx="1" fill="#22C55E" />

                                {/* Candle 10 (Up) */}
                                <line x1="290" y1="8" x2="290" y2="35" stroke="#22C55E" strokeWidth="1" />
                                <rect x="285" y="10" width="10" height="20" rx="1" fill="#22C55E" />

                                {/* Candle 11 (Down) */}
                                <line x1="320" y1="14" x2="320" y2="42" stroke="#EF4444" strokeWidth="1" />
                                <rect x="315" y="18" width="10" height="14" rx="1" fill="#EF4444" />

                                {/* Candle 12 (Up - Latest) */}
                                <line x1="345" y1="5" x2="345" y2="30" stroke="#22C55E" strokeWidth="1" />
                                <rect x="340" y="8" width="10" height="18" rx="1" fill="#22C55E" />

                                {/* EMA Curve */}
                                <path d="M15,50 Q80,45 170,30 T260,20 T350,14" fill="none" stroke="var(--primary)" strokeWidth="1.5" opacity="0.8" />

                                {/* Volume Bars at Bottom */}
                                <rect x="16" y="72" width="8" height="8" rx="1" fill="#22C55E" opacity="0.4" />
                                <rect x="46" y="70" width="8" height="10" rx="1" fill="#EF4444" opacity="0.4" />
                                <rect x="76" y="71" width="8" height="9" rx="1" fill="#22C55E" opacity="0.4" />
                                <rect x="106" y="68" width="8" height="12" rx="1" fill="#22C55E" opacity="0.4" />
                                <rect x="136" y="73" width="8" height="7" rx="1" fill="#EF4444" opacity="0.4" />
                                <rect x="166" y="67" width="8" height="13" rx="1" fill="#22C55E" opacity="0.4" />
                                <rect x="196" y="65" width="8" height="15" rx="1" fill="#22C55E" opacity="0.4" />
                                <rect x="226" y="72" width="8" height="8" rx="1" fill="#EF4444" opacity="0.4" />
                                <rect x="256" y="66" width="8" height="14" rx="1" fill="#22C55E" opacity="0.4" />
                                <rect x="286" y="64" width="8" height="16" rx="1" fill="#22C55E" opacity="0.4" />
                                <rect x="316" y="73" width="8" height="7" rx="1" fill="#EF4444" opacity="0.4" />
                                <rect x="341" y="62" width="8" height="18" rx="1" fill="#22C55E" opacity="0.4" />
                            </svg>
                        </div>

                        {/* Live Holdings Mini Table */}
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', marginBottom: '1rem' }}>
                            <div style={{ padding: '0.85rem', background: 'var(--surface2)', borderRadius: '12px', border: '1px solid var(--border)' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '2px' }}>
                                    <span style={{ fontWeight: 800, fontSize: '0.82rem' }}>RELIANCE</span>
                                    <span style={{ fontSize: '0.72rem', color: 'var(--green)', fontWeight: 700 }}>+1.85%</span>
                                </div>
                                <div style={{ fontSize: '1.05rem', fontWeight: 700, color: 'var(--text)', fontFamily: 'Outfit, sans-serif' }}>₹2,948.50</div>
                                <div style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>50 Shares • ₹1.47L</div>
                            </div>

                            <div style={{ padding: '0.85rem', background: 'var(--surface2)', borderRadius: '12px', border: '1px solid var(--border)' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '2px' }}>
                                    <span style={{ fontWeight: 800, fontSize: '0.82rem' }}>TCS</span>
                                    <span style={{ fontSize: '0.72rem', color: 'var(--green)', fontWeight: 700 }}>+0.92%</span>
                                </div>
                                <div style={{ fontSize: '1.05rem', fontWeight: 700, color: 'var(--text)', fontFamily: 'Outfit, sans-serif' }}>₹3,845.20</div>
                                <div style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>30 Shares • ₹1.15L</div>
                            </div>
                        </div>

                        {/* AI Portfolio Health Score */}
                        <div 
                            style={{ 
                                display: 'flex', 
                                alignItems: 'center', 
                                justifyContent: 'space-between', 
                                padding: '0.7rem 1rem', 
                                background: 'rgba(245, 158, 11, 0.08)', 
                                border: '1px solid rgba(245, 158, 11, 0.22)', 
                                borderRadius: '10px',
                                fontSize: '0.78rem'
                            }}
                        >
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                <span>✨</span>
                                <span style={{ fontWeight: 700, color: 'var(--text)' }}>AI Portfolio Health Score</span>
                            </div>
                            <span style={{ fontWeight: 800, color: 'var(--primary)' }}>94/100 (Optimal)</span>
                        </div>
                    </div>
                </div>
            </div>

            {/* Stats Ribbon */}
            <div className="landing-stats-ribbon">
                <div className="stats-grid-inner">
                    <div>
                        <div className="stat-metric-number">₹150Cr+</div>
                        <div className="stat-metric-label">Simulated Capital Traded</div>
                    </div>
                    <div>
                        <div className="stat-metric-number">9</div>
                        <div className="stat-metric-label">Decoupled Microservices</div>
                    </div>
                    <div>
                        <div className="stat-metric-number">&lt; 12ms</div>
                        <div className="stat-metric-label">Order Matching Latency</div>
                    </div>
                    <div>
                        <div className="stat-metric-number">100%</div>
                        <div className="stat-metric-label">Free Paper Trading</div>
                    </div>
                </div>
            </div>

            {/* Feature Showcase Grid */}
            <div className="landing-features-section">
                <div className="section-tag">Enterprise Architecture</div>
                <h2 className="section-heading">Engineered for Precision Trading</h2>

                <div className="landing-features-grid">
                    <div className="landing-feature-card">
                        <div className="feature-icon-box">⚡</div>
                        <h3>Sub-Millisecond Execution</h3>
                        <p>In-memory limit price queue order book supporting Market, Limit, Stop-Loss, and Bracket orders with zero slippage simulation.</p>
                    </div>

                    <div className="landing-feature-card">
                        <div className="feature-icon-box">🤖</div>
                        <h3>Gemini AI Strategist</h3>
                        <p>Continuous AI-powered portfolio diagnostics, concentration risk analysis, and actionable asset rebalancing suggestions.</p>
                    </div>

                    <div className="landing-feature-card">
                        <div className="feature-icon-box">🔐</div>
                        <h3>Kafka-Powered 2FA Security</h3>
                        <p>Asynchronous OTP authentication pipeline with immutable double-entry ledger verification for every deposit, buy, and sell trade.</p>
                    </div>
                </div>
            </div>
        </div>
    );
}
