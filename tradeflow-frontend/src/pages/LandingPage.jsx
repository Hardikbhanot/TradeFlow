import { Link } from 'react-router-dom';

export default function LandingPage() {
    return (
        <div className="landing-page">
            <nav className="landing-nav">
                <div className="logo">
                    <span>▲</span> TradeFlow
                </div>
                <div>
                    <Link to="/login" className="btn btn-ghost" style={{ marginRight: '1rem' }}>Sign In</Link>
                    <Link to="/register" className="btn btn-primary" style={{ background: 'linear-gradient(90deg, #00FFD1 0%, #00BFFF 100%)', color: '#000', fontWeight: 800 }}>Get Started</Link>
                </div>
            </nav>

            <div className="landing-hero">
                <div className="landing-hero-content">
                    <h1 className="landing-hero-title">
                        Next-Generation <br />
                        <span>Market Intelligence</span>
                    </h1>
                    <p className="landing-hero-subtitle">
                        Experience lightning-fast execution, AI-driven portfolio insights, and a professional-grade trade desk built for modern traders.
                    </p>
                    <div className="landing-hero-actions">
                        <Link to="/register" className="btn btn-primary" style={{ padding: '1rem 2rem', fontSize: '1rem', background: 'linear-gradient(90deg, #00FFD1 0%, #00BFFF 100%)', color: '#000', fontWeight: 800 }}>
                            Start Trading Now
                        </Link>
                    </div>
                </div>

                <div className="landing-hero-visual">
                    <div className="glow-orb"></div>
                    <div className="landing-mockup">
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '2rem', borderBottom: '1px solid rgba(255,255,255,0.1)', paddingBottom: '1rem' }}>
                            <div style={{ fontSize: '0.8rem', fontWeight: 800, color: 'var(--text-muted)', letterSpacing: '0.1em' }}>KINETIC PORTFOLIO</div>
                            <div style={{ color: 'var(--primary)', fontWeight: 800, fontSize: '0.8rem' }}>LIVE</div>
                        </div>
                        <div style={{ fontSize: '3rem', fontWeight: 800, color: 'var(--text)', marginBottom: '1rem', fontFamily: 'Outfit, sans-serif' }}>₹2,45,890.50</div>
                        <div style={{ color: 'var(--primary)', fontWeight: 700, marginBottom: '2rem' }}>+12.4% (1D)</div>
                        
                        <div style={{ display: 'flex', gap: '1rem', marginBottom: '1rem' }}>
                            <div style={{ flex: 1, padding: '1rem', background: 'rgba(0,0,0,0.3)', borderRadius: '8px' }}>
                                <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginBottom: '4px' }}>RELIANCE</div>
                                <div style={{ fontWeight: 700 }}>₹2,980.45</div>
                            </div>
                            <div style={{ flex: 1, padding: '1rem', background: 'rgba(0,0,0,0.3)', borderRadius: '8px' }}>
                                <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginBottom: '4px' }}>TCS</div>
                                <div style={{ fontWeight: 700 }}>₹3,845.20</div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
