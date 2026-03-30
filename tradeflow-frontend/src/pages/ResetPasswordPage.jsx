import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import api from '../api/axios';

export default function ResetPasswordPage() {
    const [searchParams] = useSearchParams();
    const token = searchParams.get('token');
    const [form, setForm] = useState({ password: '', confirmPassword: '' });
    const [error, setError] = useState('');
    const [message, setMessage] = useState('');
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    useEffect(() => {
        if (!token) {
            setError('Invalid or missing reset token.');
        }
    }, [token]);

    async function handleSubmit(e) {
        e.preventDefault();
        setError('');
        if (!token) return;

        if (form.password !== form.confirmPassword) {
            setError('Passwords do not match.');
            return;
        }

        setLoading(true);
        try {
            const params = new URLSearchParams();
            params.append('token', token);
            params.append('newPassword', form.password);

            const res = await api.post('/auth/reset-password', params);
            setMessage(res.data?.message || 'Password successfully reset.');
            setTimeout(() => navigate('/login'), 3000);
        } catch (err) {
            const data = err.response?.data;
            const msg = typeof data === 'string' ? data : (data?.message || data?.error || 'Reset failed.');
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
                    Set your new password.
                </div>

                {error && <div className="auth-error" style={{ marginBottom: '1.5rem' }}>{error}</div>}
                {message && <div className="badge badge-green" style={{ marginBottom: '1.5rem', width: '100%', padding: '0.8rem', justifyContent: 'center' }}>{message}</div>}

                {!message && token ? (
                    <form className="auth-form" onSubmit={handleSubmit}>
                        <div className="form-group">
                            <label className="auth-label">New Password</label>
                            <input
                                className="form-input"
                                type="password"
                                placeholder="••••••••"
                                value={form.password}
                                onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
                                required
                                autoFocus
                            />
                        </div>
                        <div className="form-group" style={{ marginBottom: '1rem' }}>
                            <label className="auth-label">Confirm Password</label>
                            <input
                                className="form-input"
                                type="password"
                                placeholder="••••••••"
                                value={form.confirmPassword}
                                onChange={e => setForm(f => ({ ...f, confirmPassword: e.target.value }))}
                                required
                            />
                        </div>
                        <button className="btn-auth-primary" type="submit" disabled={loading}>
                            {loading ? 'Updating…' : 'Update Password →'}
                        </button>
                    </form>
                ) : (
                   !token && (
                        <div style={{ textAlign: 'center', marginTop: '1rem' }}>
                            <Link to="/login" className="btn btn-ghost btn-full">Back to Login</Link>
                        </div>
                   )
                )}
            </div>
        </div>
    );
}
