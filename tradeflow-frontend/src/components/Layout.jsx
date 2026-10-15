import { useState, useEffect } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import ThemeModal from './ThemeModal';
import TickerBar from './TickerBar';
import { isMarketOpen } from '../utils/marketHours';

// Clean SVG Icons
const Icons = {
    Dashboard: () => (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <rect x="3" y="3" width="7" height="7" rx="1.5"/>
            <rect x="14" y="3" width="7" height="7" rx="1.5"/>
            <rect x="14" y="14" width="7" height="7" rx="1.5"/>
            <rect x="3" y="14" width="7" height="7" rx="1.5"/>
        </svg>
    ),
    Portfolio: () => (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <rect x="2" y="7" width="20" height="14" rx="2" ry="2"/>
            <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/>
        </svg>
    ),
    Market: () => (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="22 7 13.5 15.5 8.5 10.5 2 17"/>
            <polyline points="16 7 22 7 22 13"/>
        </svg>
    ),
    Orders: () => (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
        </svg>
    ),
    Wallet: () => (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <rect x="2" y="5" width="20" height="14" rx="2"/>
            <line x1="2" y1="10" x2="22" y2="10"/>
        </svg>
    ),
    Palette: () => (
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="13.5" cy="6.5" r=".5" fill="currentColor"/>
            <circle cx="17.5" cy="10.5" r=".5" fill="currentColor"/>
            <circle cx="8.5" cy="7.5" r=".5" fill="currentColor"/>
            <circle cx="6.5" cy="12.5" r=".5" fill="currentColor"/>
            <path d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10c.926 0 1.648-.746 1.648-1.688 0-.437-.18-.835-.437-1.125-.29-.289-.438-.652-.438-1.125a1.64 1.64 0 0 1 1.668-1.668h1.996c3.051 0 5.563-2.512 5.563-5.563C22 6.5 17.5 2 12 2z"/>
        </svg>
    ),
    Logout: () => (
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
            <polyline points="16 17 21 12 16 7"/>
            <line x1="21" y1="12" x2="9" y2="12"/>
        </svg>
    ),
};

const NAV = [
    { to: '/dashboard', Icon: Icons.Dashboard, label: 'Dashboard' },
    { to: '/portfolio', Icon: Icons.Portfolio, label: 'Portfolio' },
    { to: '/market', Icon: Icons.Market, label: 'Market' },
    { to: '/orders', Icon: Icons.Orders, label: 'Orders' },
    { to: '/wallet', Icon: Icons.Wallet, label: 'Wallet' },
];

function Clock() {
    const [time, setTime] = useState(new Date());
    useEffect(() => {
        const id = setInterval(() => setTime(new Date()), 1000);
        return () => clearInterval(id);
    }, []);
    return (
        <span style={{ fontVariantNumeric: 'tabular-nums' }}>
            {time.toLocaleTimeString('en-IN', { timeZone: 'Asia/Kolkata', hour: '2-digit', minute: '2-digit', second: '2-digit' })} IST
        </span>
    );
}

function MarketStatus() {
    const [isOpen, setIsOpen] = useState(isMarketOpen());

    useEffect(() => {
        const id = setInterval(() => setIsOpen(isMarketOpen()), 60000);
        return () => clearInterval(id);
    }, []);

    return (
        <div style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
            <span 
                style={{ 
                    width: '8px', 
                    height: '8px', 
                    borderRadius: '50%', 
                    background: isOpen ? 'var(--green)' : 'var(--red)',
                    boxShadow: isOpen ? '0 0 8px var(--green)' : '0 0 8px var(--red)',
                    display: 'inline-block' 
                }} 
            />
            <span style={{ fontWeight: 600, fontSize: '0.78rem' }}>
                {isOpen ? 'Market Open' : 'Market Closed (IST)'}
            </span>
        </div>
    );
}

export default function Layout({ children, title }) {
    const { user, logout } = useAuth();
    const { activeThemeObj } = useTheme();
    const navigate = useNavigate();
    const [isThemeModalOpen, setIsThemeModalOpen] = useState(false);

    function handleLogout() {
        logout();
        navigate('/login');
    }

    const initials = (user?.username ?? 'T').slice(0, 2).toUpperCase();

    return (
        <div className="app-layout">
            <ThemeModal isOpen={isThemeModalOpen} onClose={() => setIsThemeModalOpen(false)} />

            {/* Sidebar */}
            <aside className="sidebar">
                <div className="sidebar-brand">
                    <div className="sidebar-title">
                        <img 
                            src="/favicon.svg" 
                            alt="TradeFlow Logo" 
                            style={{ 
                                width: '30px', 
                                height: '30px', 
                                borderRadius: '8px', 
                                boxShadow: '0 0 12px var(--primary-glow)' 
                            }} 
                        />
                        <span>TRADEFLOW</span>
                    </div>
                    <div className="sidebar-subtitle">Terminal V2.0</div>
                </div>

                <div className="nav-section-label">Main Console</div>
                {NAV.map((n) => (
                    <NavLink
                        key={n.to}
                        to={n.to}
                        className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                    >
                        <span className="nav-icon"><n.Icon /></span>
                        {n.label}
                    </NavLink>
                ))}

                <div className="nav-section-label" style={{ marginTop: '1.25rem' }}>Preferences</div>
                <button 
                    type="button"
                    className="nav-link"
                    style={{ background: 'transparent', width: '100%', border: 'none', textAlign: 'left', cursor: 'pointer' }}
                    onClick={() => setIsThemeModalOpen(true)}
                >
                    <span className="nav-icon"><Icons.Palette /></span>
                    <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flex: 1 }}>
                        Theme Settings
                        <span 
                            style={{ 
                                width: '10px', 
                                height: '10px', 
                                borderRadius: '50%', 
                                background: activeThemeObj.primaryColor,
                                border: '1px solid rgba(255,255,255,0.4)' 
                            }} 
                        />
                    </span>
                </button>

                <div className="sidebar-footer">
                    <div className="sidebar-user">
                        <div className="sidebar-avatar">{initials}</div>
                        <div className="sidebar-user-info">
                            <div className="name">{user?.username ?? 'Trader'}</div>
                            <div className="role">Verified Investor</div>
                        </div>
                    </div>
                    <button className="sidebar-logout" onClick={handleLogout}>
                        <Icons.Logout />
                        <span>Logout</span>
                    </button>
                </div>
            </aside>

            {/* Main Content Area */}
            <div className="main-area">
                <TickerBar />
                
                <header className="topbar">
                    <div className="topbar-left" style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
                        <span className="topbar-title">{title}</span>
                        
                        {/* Live Index Pills */}
                        <div className="hide-mobile" style={{ display: 'flex', gap: '10px' }}>
                            <div className="topbar-index-chip">
                                <span className="chip-name">NIFTY 50</span>
                                <span className="chip-val">24,850.30</span>
                                <span className="chip-change green">+0.65%</span>
                            </div>
                            <div className="topbar-index-chip">
                                <span className="chip-name">SENSEX</span>
                                <span className="chip-val">81,420.10</span>
                                <span className="chip-change green">+0.52%</span>
                            </div>
                        </div>
                    </div>
                    
                    <div className="topbar-right">
                        {localStorage.getItem('tf_demo_mode') === 'true' && (
                            <div className="badge hide-mobile" style={{ background: 'rgba(245, 158, 11, 0.15)', color: 'var(--primary)', border: '1px solid rgba(245, 158, 11, 0.3)', fontSize: '0.72rem', fontWeight: 700, padding: '4px 10px', borderRadius: '6px' }}>
                                ⚡ DEMO MODE
                            </div>
                        )}
                        <div className="topbar-item hide-mobile">
                            <MarketStatus />
                        </div>
                        <div className="topbar-item hide-mobile" style={{ borderLeft: '1px solid var(--border)', paddingLeft: '1rem' }}>
                            <Clock />
                        </div>

                        {/* Theme Switcher Quick Trigger */}
                        <button
                            type="button"
                            className="theme-selector-pill"
                            onClick={() => setIsThemeModalOpen(true)}
                            title="Switch Theme Palette"
                        >
                            <span 
                                style={{ 
                                    width: '10px', 
                                    height: '10px', 
                                    borderRadius: '50%', 
                                    background: activeThemeObj.primaryColor,
                                    boxShadow: `0 0 6px ${activeThemeObj.primaryColor}`
                                }} 
                            />
                            <span style={{ fontSize: '0.78rem', fontWeight: 600 }}>{activeThemeObj.name}</span>
                        </button>
                    </div>
                </header>

                <main className="page-content">
                    {children}
                </main>
            </div>
        </div>
    );
}
