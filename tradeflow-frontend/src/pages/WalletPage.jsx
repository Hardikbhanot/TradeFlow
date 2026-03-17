import { useState, useEffect, useCallback } from 'react';
import Layout from '../components/Layout';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';

export default function WalletPage() {
    const { user } = useAuth();
    const [wallet, setWallet] = useState(null);
    const [ledger, setLedger] = useState([]);
    const [loading, setLoading] = useState(true);

    // Deposit form
    const [amount, setAmount] = useState('');
    const [depositing, setDepositing] = useState(false);
    const [msg, setMsg] = useState(null);

    const fetchWalletData = useCallback(async () => {
        if (!user?.userId) return;

        try {
            const [wRes, lRes] = await Promise.all([
                api.get(`/api/v1/wallets/user/${user.userId}`),
                api.get('/api/v1/wallets/ledger'),
            ]);
            setWallet(wRes.data);
            setLedger(lRes.data);
        } catch {
            // silent
        } finally {
            setLoading(false);
        }
    }, [user?.userId]);

    useEffect(() => {
        fetchWalletData();
    }, [fetchWalletData]);

    async function handleDeposit(e) {
        e.preventDefault();
        if (!amount || Number(amount) <= 0) return;
        setDepositing(true);
        setMsg(null);
        try {
            await api.post(`/api/v1/wallets/add?amount=${amount}`);
            setMsg({ ok: true, text: `Successfully deposited ₹${amount}` });
            setAmount('');
            await fetchWalletData(); // refresh balance and ledger
        } catch {
            setMsg({ ok: false, text: 'Deposit failed. Try again.' });
        } finally {
            setDepositing(false);
            setTimeout(() => setMsg(null), 3000);
        }
    }

    const badgeClass = (type) => {
        switch (type) {
            case 'DEPOSIT': return 'badge-green';
            case 'WITHDRAWAL': return 'badge-red';
            case 'TRADE_BUY': return 'badge-blue';
            case 'TRADE_SELL': return 'badge-gold';
            default: return 'badge-gray';
        }
    };

    const fmt = n => Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    const fmtDate = d => new Date(d).toLocaleString('en-IN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });

    return (
        <Layout title="Wallet & Ledger">
            <div className="page-header">
                <h1>Wallet</h1>
                <p>Manage your funds and view transaction history.</p>
            </div>

            <div className="dashboard-grid">
                {/* Left Col: Balance + Deposit */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>

                    <div className="wallet-balance-card">
                        <div className="wlabel">Available Balance</div>
                        <div className="wamount">₹{wallet ? fmt(wallet.balance) : '0.00'}</div>
                    </div>

                    <div className="card">
                        <div style={{ fontWeight: 600, marginBottom: '1rem' }}>Add Funds</div>
                        {msg && (
                            <div style={{
                                background: msg.ok ? 'var(--green-dim)' : 'var(--red-dim)',
                                color: msg.ok ? 'var(--green)' : 'var(--red)',
                                padding: '0.6rem', borderRadius: 6, fontSize: '0.82rem', marginBottom: '1rem'
                            }}>
                                {msg.text}
                            </div>
                        )}
                        <form onSubmit={handleDeposit} style={{ display: 'flex', gap: '8px' }}>
                            <input
                                className="form-input"
                                style={{ flex: 1, fontSize: '1.1rem', fontWeight: 600 }}
                                type="number"
                                min="1"
                                placeholder="Amount (₹)"
                                value={amount}
                                onChange={e => setAmount(e.target.value)}
                                required
                            />
                            <button className="btn btn-green" type="submit" disabled={depositing || !amount}>
                                {depositing ? 'Processing…' : 'Deposit'}
                            </button>
                        </form>
                    </div>
                </div>

                {/* Right Col: Ledger */}
                <div className="card" style={{ padding: 0 }}>
                    <div style={{ padding: '1.25rem 1.5rem', borderBottom: '1px solid var(--border)' }}>
                        <span style={{ fontWeight: 600 }}>Ledger History</span>
                    </div>

                    {loading ? (
                        <div className="centered-spinner"><div className="spinner" /></div>
                    ) : ledger.length === 0 ? (
                        <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                            No transactions found.
                        </div>
                    ) : (
                        <div style={{ overflowY: 'auto', maxHeight: 'calc(100vh - 280px)' }}>
                            <table className="tf-table">
                                <thead>
                                    <tr>
                                        <th>Date</th>
                                        <th>Type</th>
                                        <th>Amount</th>
                                        <th>Status</th>
                                        <th>Reference</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {ledger.map(row => (
                                        <tr key={row.id}>
                                            <td style={{ fontSize: '0.8rem' }}>{fmtDate(row.timestamp)}</td>
                                            <td><span className={`badge ${badgeClass(row.type)}`}>{row.type.replace('_', ' ')}</span></td>
                                            <td style={{ fontWeight: 600, color: row.type === 'DEPOSIT' || row.type === 'TRADE_SELL' ? 'var(--green)' : 'var(--red)' }}>
                                                {row.type === 'DEPOSIT' || row.type === 'TRADE_SELL' ? '+' : '-'}₹{fmt(row.amount)}
                                            </td>
                                            <td>
                                                <span className={`badge ${row.status === 'SUCCESS' ? 'badge-green' : 'badge-red'}`}>
                                                    {row.status}
                                                </span>
                                            </td>
                                            <td style={{ fontFamily: 'monospace', fontSize: '11px', color: 'var(--text-muted)' }}>
                                                {row.referenceId}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            </div>
        </Layout>
    );
}
