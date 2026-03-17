import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
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

    async function handlePasswordSubmit(e) {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            const params = new URLSearchParams();
            params.append('username', form.username);
            params.append('password', form.password);

            const res = await api.post('/auth/login', params);

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
            setError(err.response?.data || 'Login failed. Check your credentials.');
        } finally {
            setLoading(false);
        }
    }

    async function handleOtpVerify(e) {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            const params = new URLSearchParams();
            params.append('username', form.username);
            params.append('otp', form.otp);

            const res = await api.post('/auth/verify-otp', params);
            const token = res.data?.token || (typeof res.data === 'string' ? res.data : null);

            if (!token) {
                setError('OTP verified but token was not returned.');
                return;
            }

            login(token);
            navigate('/dashboard');
        } catch (err) {
            setError(err.response?.data || 'OTP verification failed.');
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="auth-page">
            <div className="auth-card">
                <div className="auth-logo">▲ TradeFlow</div>
                <div className="auth-subtitle">
                    {otpStep ? 'Enter the 6-digit code sent to your email.' : 'Welcome back. Sign in to your account.'}
                </div>

                {error && <div className="auth-error">{error}</div>}

                {!otpStep ? (
                    <form className="auth-form" onSubmit={handlePasswordSubmit}>
                        <div className="form-group">
                            <label>Username</label>
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
                        <div className="form-group">
                            <label>Password</label>
                            <input
                                className="form-input"
                                type="password"
                                placeholder="••••••••"
                                value={form.password}
                                onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
                                required
                            />
                        </div>
                        <button className="btn btn-primary btn-full" type="submit" disabled={loading}>
                            {loading ? 'Signing in…' : 'Continue →'}
                        </button>
                    </form>
                ) : (
                    <form className="auth-form" onSubmit={handleOtpVerify}>
                        <div className="form-group">
                            <label>Email</label>
                            <input className="form-input" value={otpEmail || 'Registered email'} disabled />
                        </div>
                        <div className="form-group">
                            <label>OTP</label>
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
                        <button className="btn btn-primary btn-full" type="submit" disabled={loading || form.otp.length !== 6}>
                            {loading ? 'Verifying…' : 'Verify OTP →'}
                        </button>
                        <button
                            type="button"
                            className="btn btn-ghost btn-full"
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

                {!otpStep && (
                    <div className="auth-switch">
                        Don't have an account?<Link to="/register">Create one</Link>
                    </div>
                )}
            </div>
        </div>
    );
}
