import { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
    const [form, setForm] = useState({ username: '', password: '', otp: '' });
    const [otpStep, setOtpStep] = useState(false);
    const [otpEmail, setOtpEmail] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const { login } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();
    const [successMessage, setSuccessMessage] = useState(location.state?.message || '');

    async function handlePasswordSubmit(e) {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            const res = await api.post('/auth/login', {
                username: form.username,
                password: form.password
            });

            if (res.data?.requiresOtp) {
                setOtpStep(true);
                setOtpEmail(res.data.email || '');
                return;
            }

            if (typeof res.data === 'string') {
                login(res.data);
                navigate('/dashboard');
                return;
            }

            if (res.data?.token) {
                login(res.data.token);
                navigate('/dashboard');
                return;
            }

            setError('Unexpected login response from server.');
        } catch (err) {
            const data = err.response?.data;
            const message = typeof data === 'string' ? data : (data?.message || data?.error || 'Login failed. Check your credentials.');
            setError(message);
        } finally {
            setLoading(false);
        }
    }

    async function handleOtpVerify(e) {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            const res = await api.post('/auth/verify-otp', {
                username: form.username,
                otp: form.otp
            });
            const token = res.data?.token || (typeof res.data === 'string' ? res.data : null);

            if (!token) {
                setError('OTP verified but token was not returned.');
                return;
            }

            login(token);
            navigate('/dashboard');
        } catch (err) {
            const data = err.response?.data;
            const message = typeof data === 'string' ? data : (data?.message || data?.error || 'OTP verification failed.');
            setError(message);
        } finally {
            setLoading(false);
        }
    }

    function handleDemoLogin() {
        const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' })).replace(/=/g, '');
        const payload = btoa(JSON.stringify({
            userId: 1,
            username: 'Hardik (Trader)',
            email: 'trader@tradeflow.io',
            exp: Math.floor(Date.now() / 1000) + 86400 * 30
        })).replace(/=/g, '');
        const demoToken = `${header}.${payload}.demo_signature`;

        localStorage.setItem('tf_demo_mode', 'true');
        login(demoToken);
        navigate('/dashboard');
    }

    return (
        <div className="auth-container">
            <div className="auth-card-v2">
                <div className="auth-logo-large" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px' }}>
                    <img src="/favicon.svg" alt="TradeFlow Logo" style={{ width: '36px', height: '36px', borderRadius: '10px' }} />
                    <span>TradeFlow</span>
                </div>
                <div className="auth-subtitle-v2">
                    {otpStep ? 'Enter the 6-digit code sent to your email.' : 'Welcome back. Sign in to your trading terminal.'}
                </div>

                {error && <div className="auth-error" style={{ marginBottom: '1.5rem' }}>{error}</div>}
                {successMessage && <div className="auth-success" style={{ marginBottom: '1.5rem', padding: '1rem', backgroundColor: 'rgba(46, 213, 115, 0.1)', color: '#2ed573', borderRadius: '8px', border: '1px solid rgba(46, 213, 115, 0.3)' }}>{successMessage}</div>}

                {!otpStep ? (
                    <form className="auth-form" onSubmit={handlePasswordSubmit}>
                        <div className="form-group">
                            <label className="auth-label">Username</label>
                            <input
                                className="form-input"
                                type="text"
                                placeholder="your_username"
                                value={form.username}
                                onChange={e => setForm(f => ({ ...f, username: e.target.value }))}
                                required
                                autoFocus
                            />
                        </div>
                        <div className="form-group" style={{ marginBottom: '2rem' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <label className="auth-label">Password</label>
                                <Link to="/forgot-password" style={{ fontSize: '0.7rem', color: 'var(--blue)', fontWeight: 600, textTransform: 'uppercase', marginBottom: '0.5rem' }}>Forgot?</Link>
                            </div>
                            <input
                                className="form-input"
                                type="password"
                                placeholder="••••••••"
                                value={form.password}
                                onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
                                required
                            />
                        </div>
                        <button className="btn-auth-primary" type="submit" disabled={loading}>
                            {loading ? 'Signing in…' : 'Continue →'}
                        </button>
                    </form>
                ) : (
                    <form className="auth-form" onSubmit={handleOtpVerify}>
                        <div className="form-group">
                            <label className="auth-label">Email</label>
                            <input className="form-input" value={otpEmail || 'Registered email'} disabled />
                        </div>
                        <div className="form-group" style={{ marginBottom: '2rem' }}>
                            <label className="auth-label">OTP</label>
                            <input
                                className="form-input"
                                type="text"
                                placeholder="6-digit code"
                                maxLength={6}
                                value={form.otp}
                                onChange={e => setForm(f => ({ ...f, otp: e.target.value.replace(/\D/g, '') }))}
                                required
                                autoFocus
                            />
                        </div>
                        <button className="btn-auth-primary" type="submit" disabled={loading || form.otp.length !== 6}>
                            {loading ? 'Verifying…' : 'Verify OTP →'}
                        </button>
                        <button
                            type="button"
                            className="btn btn-ghost btn-full"
                            style={{ marginTop: '1rem' }}
                            onClick={() => {
                                setOtpStep(false);
                                setForm(f => ({ ...f, password: '', otp: '' }));
                                setError('');
                            }}
                        >
                            Use Different Credentials
                        </button>
                    </form>
                )}

                <div style={{ marginTop: '1.75rem', marginBottom: '0.5rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '1.25rem' }}>
                        <div style={{ flex: 1, height: '1px', background: 'var(--border)' }} />
                        <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.1em', fontWeight: 700 }}>
                            OR TEST OFFLINE
                        </span>
                        <div style={{ flex: 1, height: '1px', background: 'var(--border)' }} />
                    </div>

                    <button
                        type="button"
                        className="btn btn-ghost"
                        style={{
                            width: '100%',
                            padding: '0.85rem 1rem',
                            border: '1px solid var(--border2)',
                            background: 'var(--surface2)',
                            color: 'var(--primary)',
                            fontWeight: 700,
                            fontSize: '0.88rem',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            gap: '8px',
                            boxShadow: '0 4px 12px rgba(0,0,0,0.2)'
                        }}
                        onClick={handleDemoLogin}
                    >
                        <span>⚡</span> Explore Demo Mode (No Backend Needed)
                    </button>
                    <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', textAlign: 'center', marginTop: '6px' }}>
                        Immediate access to test the redesigned Dashboard, Charts, and Terminal.
                    </div>
                </div>

                <div className="auth-footer">
                    Don't have an account? <Link to="/register">Create one</Link>
                </div>
            </div>
        </div>
    );
}
