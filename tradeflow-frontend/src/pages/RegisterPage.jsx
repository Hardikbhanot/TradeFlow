import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../api/axios';

export default function RegisterPage() {
    const [form, setForm] = useState({ username: '', email: '', password: '', confirmPassword: '' });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    async function handleSubmit(e) {
        e.preventDefault();
        setError('');

        if (form.password !== form.confirmPassword) {
            setError('Passwords do not match.');
            return;
        }

        setLoading(true);
        try {
            await api.post('/auth/register', {
                username: form.username,
                email: form.email,
                password: form.password
            });
            navigate('/login');
        } catch (err) {
            const data = err.response?.data;
            const message = typeof data === 'string' ? data : (data?.message || data?.error || 'Registration failed.');
            setError(message);
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
                    Start your trading journey with KINETIC tools.
                </div>

                {error && <div className="auth-error" style={{ marginBottom: '1.5rem' }}>{error}</div>}

                <form className="auth-form" onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label className="auth-label">Username</label>
                        <input
                            className="form-input"
                            type="text"
                            placeholder="choose_a_username"
                            value={form.username}
                            onChange={e => setForm(f => ({ ...f, username: e.target.value }))}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label className="auth-label">Email Address</label>
                        <input
                            className="form-input"
                            type="email"
                            placeholder="you@example.com"
                            value={form.email}
                            onChange={e => setForm(f => ({ ...f, email: e.target.value }))}
                            required
                        />
                    </div>
                    <div className="form-row">
                        <div className="form-group">
                            <label className="auth-label">Password</label>
                            <input
                                className="form-input"
                                type="password"
                                placeholder="••••••••"
                                value={form.password}
                                onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
                                required
                            />
                        </div>
                        <div className="form-group">
                            <label className="auth-label">Confirm</label>
                            <input
                                className="form-input"
                                type="password"
                                placeholder="••••••••"
                                value={form.confirmPassword}
                                onChange={e => setForm(f => ({ ...f, confirmPassword: e.target.value }))}
                                required
                            />
                        </div>
                    </div>

                    <button className="btn-auth-primary" type="submit" style={{ marginTop: '1rem' }} disabled={loading}>
                        {loading ? 'Creating Account…' : 'Create Account →'}
                    </button>
                </form>

                <div className="auth-footer">
                    Already have an account? <Link to="/login">Sign in</Link>
                </div>
            </div>
        </div>
    );
}
