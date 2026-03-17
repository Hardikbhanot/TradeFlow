import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../api/axios';

export default function RegisterPage() {
    const [form, setForm] = useState({ username: '', email: '', password: '' });
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    async function handleSubmit(e) {
        e.preventDefault();
        setError(''); setSuccess('');
        setLoading(true);
        try {
            const params = new URLSearchParams();
            params.append('username', form.username);
            params.append('email', form.email);
            params.append('password', form.password);

            const res = await api.post('/auth/register', params);


            if (res.status === 200) {
                setSuccess('Account created! Redirecting to login…');
                setTimeout(() => navigate('/login'), 1500);
            } else {
                setError('Unexpected response from server.');
            }
        } catch (err) {
            let msg = 'Registration failed.';
            if (err.response?.data) {

                msg = typeof err.response.data === 'string'
                    ? err.response.data
                    : (err.response.data.message || err.response.data.error || 'Registration failed.');
            } else if (err.message) {
                msg = err.message;
            }
            setError(msg);
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="auth-page">
            <div className="auth-card">
                <div className="auth-logo">▲ TradeFlow</div>
                <div className="auth-subtitle">Create your trading account in seconds.</div>

                {error && <div className="auth-error">{error}</div>}
                {success && <div style={{ background: 'var(--green-dim)', color: 'var(--green)', borderRadius: 'var(--radius)', padding: '0.6rem 0.9rem', fontSize: '0.83rem' }}>{success}</div>}

                <form className="auth-form" onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label>Username</label>
                        <input className="form-input" type="text" placeholder="trader_pro" value={form.username}
                            onChange={e => setForm(f => ({ ...f, username: e.target.value }))} required autoFocus />
                    </div>
                    <div className="form-group">
                        <label>Email</label>
                        <input className="form-input" type="email" placeholder="you@example.com" value={form.email}
                            onChange={e => setForm(f => ({ ...f, email: e.target.value }))} required />
                    </div>
                    <div className="form-group">
                        <label>Password</label>
                        <input className="form-input" type="password" placeholder="min. 8 characters" value={form.password}
                            onChange={e => setForm(f => ({ ...f, password: e.target.value }))} required />
                    </div>
                    <button className="btn btn-green btn-full" type="submit" disabled={loading}>
                        {loading ? 'Creating Account…' : 'Create Account →'}
                    </button>
                </form>

                <div className="auth-switch">
                    Already have an account?<Link to="/login">Sign in</Link>
                </div>
            </div>
        </div>
    );
}
