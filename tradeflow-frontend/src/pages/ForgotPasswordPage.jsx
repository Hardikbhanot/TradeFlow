import { useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/axios';

export default function ForgotPasswordPage() {
    const [email, setEmail] = useState('');
    const [message, setMessage] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    async function handleSubmit(e) {
        e.preventDefault();
        setError('');
        setMessage('');
        setLoading(true);

        try {
            const params = new URLSearchParams();
            params.append('email', email);
            const res = await api.post('/auth/forgot-password', params);
            setMessage(res.data?.message || 'Check your email for reset instructions.');
        } catch (err) {
            const data = err.response?.data;
            const msg = typeof data === 'string' ? data : (data?.message || data?.error || 'Failed to send reset link.');
            setError(msg);
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="auth-container">
            <div className="auth-card-v2">
                <div className="auth-logo-large">
                    <span style={{ color: 'var(--primary)' }}>▲</span> TradeFlow
                </div>
                <div className="auth-subtitle-v2">
                    Forgot your password? No problem.
                </div>

                {error && <div className="auth-error" style={{ marginBottom: '1.5rem' }}>{error}</div>}
                {message && <div className="badge badge-green" style={{ marginBottom: '1.5rem', width: '100%', padding: '0.8rem', justifyContent: 'center' }}>{message}</div>}

                {!message ? (
                    <form className="auth-form" onSubmit={handleSubmit}>
                        <div className="form-group" style={{ marginBottom: '1rem' }}>
                            <label className="auth-label">Email Address</label>
                            <input
                                className="form-input"
                                type="email"
                                placeholder="you@example.com"
                                value={email}
                                onChange={e => setEmail(e.target.value)}
                                required
                                autoFocus
                            />
                        </div>
                        <button className="btn-auth-primary" type="submit" disabled={loading}>
                            {loading ? 'Sending link…' : 'Send Reset Link →'}
                        </button>
                    </form>
                ) : (
                    <div style={{ textAlign: 'center', marginTop: '1rem' }}>
                        <Link to="/login" className="btn btn-ghost btn-full">Return to Login</Link>
                    </div>
                )}

                <div className="auth-footer">
                    Remembered it? <Link to="/login">Sign in</Link>
                </div>
            </div>
        </div>
    );
}
