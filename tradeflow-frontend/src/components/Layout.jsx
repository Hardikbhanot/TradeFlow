import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import TickerBar from './TickerBar';
import { useState, useEffect } from 'react';
import { isMarketOpen } from '../utils/marketHours';

const NAV = [
    { to: '/dashboard', icon: '⊞', label: 'Dashboard' },
    { to: '/portfolio', icon: '💼', label: 'Portfolio' },
    { to: '/market', icon: '📈', label: 'Market' },
    { to: '/orders', icon: '⚡', label: 'Orders' },
    { to: '/wallet', icon: '💳', label: 'Wallet' },
];

function Clock() {
    const [time, setTime] = useState(new Date());
    useEffect(() => {
        const id = setInterval(() => setTime(new Date()), 1000);
        return () => clearInterval(id);
    }, []);
    return (
        <span>{time.toLocaleTimeString('en-IN', { timeZone: 'Asia/Kolkata', hour: '2-digit', minute: '2-digit', second: '2-digit' })} IST</span>
    );
}

function MarketStatus() {
    const [isOpen, setIsOpen] = useState(isMarketOpen());

    useEffect(() => {
        const id = setInterval(() => setIsOpen(isMarketOpen()), 60000);
        return () => clearInterval(id);
    }, []);

    return isOpen ? (
        <span><span style={{ color: 'var(--green)', fontWeight: 600 }}>● </span>Market Open</span>
    ) : (
        <span><span style={{ color: 'var(--red)', fontWeight: 600 }}>● </span>Market Closed (IST)</span>
    );
}

export default function Layout({ children, title }) {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    // Theme toggle Logic
    const [isLight, setIsLight] = useState(() => localStorage.getItem('tf_theme') === 'light');

    useEffect(() => {
        if (isLight) {
            document.body.classList.add('light-theme');
            localStorage.setItem('tf_theme', 'light');
        } else {
            document.body.classList.remove('light-theme');
            localStorage.setItem('tf_theme', 'dark');
        }
    }, [isLight]);

    function handleLogout() {
        logout();
        navigate('/login');
    }

    const initials = (user?.username ?? 'T').slice(0, 2).toUpperCase();

    return (
        <div className="app-layout">
            {/* Sidebar */}
            <aside className="sidebar">
                <div className="sidebar-brand">
                    <div className="sidebar-title">
                        <img src="/logo.png" alt="TradeFlow Logo" style={{ width: '32px', height: '32px', borderRadius: '6px' }} />
                        TRADEFLOW
                    </div>
                    <div className="sidebar-subtitle">Observatory V2.0</div>
                </div>

                <div className="nav-section-label">Main Console</div>
                {NAV.map((n) => (
                    <NavLink
                        key={n.to}
                        to={n.to}
                        className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                    >
                        <span className="nav-icon">{n.icon}</span>
                        {n.label}
                    </NavLink>
                ))}

                <div className="sidebar-footer">
                    <div className="sidebar-user">
                        <div className="sidebar-avatar">{initials}</div>
                        <div className="sidebar-user-info">
                            <div className="name">{user?.username ?? 'Trader'}</div>
                            <div className="role">Verified Investor</div>
                        </div>
                    </div>
                    <button className="sidebar-logout" onClick={handleLogout}>
                        <span>↩</span> Logout
                    </button>
                </div>
            </aside>

            {/* Main Content Area */}
            <div className="main-area">
                <TickerBar />
                
                <header className="topbar">
                    <div className="topbar-left">
                        <span className="topbar-title">{title}</span>
                    </div>
                    
                    <div className="topbar-right">
                        <div className="topbar-item hide-mobile">
                            <MarketStatus />
                        </div>
                        <div className="topbar-item hide-mobile" style={{ borderLeft: '1px solid var(--border)', paddingLeft: '1rem' }}>
                            <Clock />
                        </div>
                        <button
                            className="theme-toggle"
                            onClick={() => setIsLight(!isLight)}
                            style={{ 
                                background: 'var(--surface-opaque)', 
                                border: '1px solid var(--border)',
                                borderRadius: '6px',
                                width: '32px',
                                height: '32px',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                cursor: 'pointer',
                                color: 'var(--text-2)'
                             }}
                            title="Toggle Theme"
                        >
                            {isLight ? '🌙' : '☀️'}
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
