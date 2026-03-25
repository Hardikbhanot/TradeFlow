import React from 'react';

export default function MarketWatch() {
    const alerts = [
        { id: 1, type: 'warning', msg: 'SEBI Alert: New margin rules for F&O effective from April 1st.', time: '2h ago' },
        { id: 2, type: 'info', msg: 'RBI Policy: Interest rate decision expected at 10:00 AM tomorrow.', time: '4h ago' },
    ];

    return (
        <div className="market-watch">
            {alerts.map(a => (
                <div key={a.id} className={`alert-item alert-${a.type}`}>
                    <div className="alert-content">
                        <span className="alert-msg">{a.msg}</span>
                        <span className="alert-time">{a.time}</span>
                    </div>
                </div>
            ))}
        </div>
    );
}
