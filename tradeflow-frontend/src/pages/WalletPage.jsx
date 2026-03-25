import { useState, useEffect, useCallback } from 'react';
import Layout from '../components/Layout';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';

export default function WalletPage() {
    const { user } = useAuth();
    const [wallet, setWallet] = useState(null);
    const [ledger, setLedger] = useState([]);
    const [loading, setLoading] = useState(true);

    // Modal & Payment logic
    const [showPayModal, setShowPayModal] = useState(false);
    const [amount, setAmount] = useState('');
    const [processing, setProcessing] = useState(false);
    const [payMethod, setPayMethod] = useState('UPI');

    const fetchWalletData = useCallback(async () => {
        if (!user?.userId) return;
        try {
            const [wRes, lRes] = await Promise.all([
                api.get(`/api/v1/wallets/user/${user.userId}`),
                api.get('/api/v1/wallets/ledger'),
            ]);
            setWallet(wRes.data);
            setLedger(lRes.data);
        } catch { /* silent */ } finally {
            setLoading(false);
        }
    }, [user?.userId]);

    useEffect(() => {
        fetchWalletData();
    }, [fetchWalletData]);

    async function simulatePayment() {
        if (!amount || Number(amount) <= 0) return;
        setProcessing(true);
        try {
            // Real API call to add wallet balance
            await api.post(`/api/v1/wallets/add?amount=${amount}`);
            await fetchWalletData();
            setShowPayModal(false);
            setAmount('');
        } catch {
            alert('Payment failed. Try again.');
        } finally {
            setProcessing(false);
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

    const fmt = n => Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2 });
    const fmtDate = d => new Date(d).toLocaleString('en-IN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });

    return (
        <Layout title="Wallet & Ledger">
            <div className="page-header">
                <h1>Wallet</h1>
                <p>Manage your funds and transaction history in KINETIC style.</p>
            </div>

            <div className="dashboard-grid" style={{ gridTemplateColumns: 'minmax(300px, 1fr) 2fr', alignItems: 'start' }}>
                {/* Left: Balance & Quick Action */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
                    <div className="wallet-balance-card" style={{ padding: '2.5rem' }}>
                        <div className="wlabel" style={{ fontSize: '0.9rem', color: 'rgba(255,255,255,0.6)' }}>Available Balance</div>
                        <div className="wamount" style={{ fontSize: '2.5rem', marginTop: '0.5rem' }}>₹{wallet ? fmt(wallet.balance) : '0.00'}</div>
                        <button 
                            className="btn btn-primary" 
                            style={{ marginTop: '2rem', width: '100%', height: '50px', fontSize: '1rem' }}
                            onClick={() => setShowPayModal(true)}
                        >
                            Add Funds
                        </button>
                    </div>

                    <div className="card">
                        <div style={{ fontWeight: 600, marginBottom: '0.5rem' }}>Wallet Statistics</div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.5rem 0', borderBottom: '1px solid var(--border)' }}>
                            <span style={{ color: 'var(--text-muted)' }}>Net Credits</span>
                            <span style={{ color: 'var(--green)' }}>+ ₹{fmt(ledger.filter(l => l.type === 'DEPOSIT').reduce((acc, l) => acc + l.amount, 0))}</span>
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.5rem 0' }}>
                            <span style={{ color: 'var(--text-muted)' }}>Net Trades</span>
                            <span style={{ color: 'var(--red)' }}>- ₹{fmt(ledger.filter(l => l.type === 'TRADE_BUY').reduce((acc, l) => acc + l.amount, 0))}</span>
                        </div>
                    </div>
                </div>

                {/* Right: Compact Ledger */}
                <div className="card" style={{ padding: 0 }}>
                    <div style={{ padding: '1.25rem 1.5rem', borderBottom: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span style={{ fontWeight: 600 }}>Activity Ledger</span>
                        <div className="tab-row" style={{ margin: 0 }}>
                            <button className="tab-item active">REAL-TIME</button>
                            <button className="tab-item">REPORTS</button>
                        </div>
                    </div>

                    {loading ? (
                        <div className="centered-spinner"><div className="spinner" /></div>
                    ) : (
                        <div style={{ overflowY: 'auto', maxHeight: '500px' }}>
                            <table className="tf-table">
                                <thead style={{ position: 'sticky', top: 0, backgroundColor: 'var(--surface)', zIndex: 10 }}>
                                    <tr>
                                        <th>Date</th>
                                        <th>Type</th>
                                        <th>Amount</th>
                                        <th>Status</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {ledger.map(row => (
                                        <tr key={row.id}>
                                            <td style={{ fontSize: '0.75rem' }}>{fmtDate(row.timestamp)}</td>
                                            <td><span className={`badge ${badgeClass(row.type)}`}>{row.type.replace('_', ' ')}</span></td>
                                            <td style={{ fontWeight: 600, color: row.type === 'DEPOSIT' || row.type === 'TRADE_SELL' ? 'var(--green)' : 'var(--red)' }}>
                                                {row.type === 'DEPOSIT' || row.type === 'TRADE_SELL' ? '+' : '-'}₹{fmt(row.amount)}
                                            </td>
                                            <td><span className={`badge ${row.status === 'SUCCESS' ? 'badge-green' : 'badge-red'}`}>{row.status}</span></td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            </div>

            {/* Razorpay Mockup Modal */}
            {showPayModal && (
                <div className="modal-overlay">
                    <div className="razorpay-modal">
                        <div style={{ background: '#2B3440', padding: '1.5rem', color: '#fff' }}>
                            <div style={{ fontSize: '0.8rem', opacity: 0.8 }}>TradeFlow Technologies</div>
                            <div style={{ fontSize: '1.25rem', fontWeight: 700, marginTop: '4px' }}>₹{amount || '0.00'}</div>
                        </div>
                        
                        <div style={{ padding: '1.5rem' }}>
                            <div style={{ marginBottom: '1.5rem' }}>
                                <label style={{ fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase', color: '#666' }}>Amount to Add</label>
                                <input 
                                    type="number" 
                                    style={{ width: '100%', border: 'none', borderBottom: '2px solid #3B82F6', fontSize: '1.5rem', fontWeight: 600, padding: '8px 0', outline: 'none' }} 
                                    autoFocus
                                    value={amount}
                                    onChange={e => setAmount(e.target.value)}
                                    placeholder="0"
                                />
                            </div>

                            <div style={{ marginBottom: '1.5rem' }}>
                                <label style={{ fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase', color: '#666' }}>Payment Method</label>
                                <div style={{ display: 'flex', gap: '10px', marginTop: '8px' }}>
                                    <button 
                                        onClick={() => setPayMethod('UPI')}
                                        style={{ flex: 1, padding: '10px', borderRadius: '6px', border: payMethod === 'UPI' ? '1px solid #3B82F6' : '1px solid #ddd', background: payMethod === 'UPI' ? '#eff6ff' : '#fff', cursor: 'pointer' }}
                                    >
                                        UPI / QR
                                    </button>
                                    <button 
                                        onClick={() => setPayMethod('CARD')}
                                        style={{ flex: 1, padding: '10px', borderRadius: '6px', border: payMethod === 'CARD' ? '1px solid #3B82F6' : '1px solid #ddd', background: payMethod === 'CARD' ? '#eff6ff' : '#fff', cursor: 'pointer' }}
                                    >
                                        Card
                                    </button>
                                </div>
                            </div>

                            <button 
                                onClick={simulatePayment}
                                disabled={processing || !amount}
                                style={{ width: '100%', background: '#3B82F6', color: '#fff', border: 'none', padding: '1rem', borderRadius: '6px', fontWeight: 600, fontSize: '1rem', cursor: 'pointer' }}
                            >
                                {processing ? 'Connecting...' : `Pay ₹${amount || '0.00'}`}
                            </button>

                            <button 
                                onClick={() => setShowPayModal(false)}
                                style={{ width: '100%', background: 'transparent', color: '#666', border: 'none', marginTop: '1rem', fontSize: '0.85rem', cursor: 'pointer' }}
                            >
                                Cancel
                            </button>
                        </div>
                        <div style={{ textAlign: 'center', padding: '0.5rem', background: '#f8f9fa', fontSize: '0.7rem', color: '#999' }}>
                            SECURED BY 🏷️ <b>Razorpay</b>
                        </div>
                    </div>
                </div>
            )}
        </Layout>
    );
}
