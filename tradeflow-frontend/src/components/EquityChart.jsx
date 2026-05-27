import React from 'react';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

const CustomTooltip = ({ active, payload }) => {
    if (active && payload && payload.length) {
        return (
            <div style={{ background: 'rgba(10, 14, 18, 0.9)', border: '1px solid var(--primary)', padding: '10px', borderRadius: '8px', backdropFilter: 'blur(10px)' }}>
                <p style={{ margin: 0, color: 'var(--text-muted)', fontSize: '0.7rem' }}>{payload[0].payload.name}</p>
                <p style={{ margin: 0, color: 'var(--primary)', fontWeight: 800, fontSize: '1.1rem' }}>₹{payload[0].value.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</p>
            </div>
        );
    }
    return null;
};

export default function EquityChart({ currentValue = 0 }) {
    // Generate dynamic chart data points ending at the exact currentValue at 15:30
    const data = React.useMemo(() => {
        if (currentValue <= 0) {
            return [
                { name: '09:30', value: 0 },
                { name: '10:30', value: 0 },
                { name: '11:30', value: 0 },
                { name: '12:30', value: 0 },
                { name: '13:30', value: 0 },
                { name: '14:30', value: 0 },
                { name: '15:30', value: 0 },
            ];
        }

        // Simulate a natural market trend starting slightly lower and ending at the exact currentValue
        const times = ['09:30', '10:30', '11:30', '12:30', '13:30', '14:30', '15:30'];
        const multipliers = [0.978, 0.985, 0.982, 0.991, 0.987, 0.994, 1.000];
        
        return times.map((t, idx) => ({
            name: t,
            value: parseFloat((currentValue * multipliers[idx]).toFixed(2))
        }));
    }, [currentValue]);

    const firstVal = data[0]?.value ?? 0;
    const lastVal = data[data.length - 1]?.value ?? 0;
    const isDown = lastVal < firstVal;
    const color = isDown ? 'var(--red)' : 'var(--primary)';

    const domain = currentValue > 0
        ? [parseFloat((currentValue * 0.96).toFixed(2)), parseFloat((currentValue * 1.04).toFixed(2))]
        : [0, 100];

    return (
        <div style={{ width: '100%', height: 320, marginTop: '1rem' }}>
            <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={data}>
                    <defs>
                        <linearGradient id="colorValue" x1="0" y1="0" x2="0" y2="1">
                            <stop offset="5%" stopColor={color} stopOpacity={0.3}/>
                            <stop offset="95%" stopColor={color} stopOpacity={0}/>
                        </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="rgba(255,255,255,0.05)" />
                    <XAxis 
                        dataKey="name" 
                        axisLine={false} 
                        tickLine={false} 
                        tick={{ fill: 'var(--text-muted)', fontSize: 10 }} 
                        dy={10}
                    />
                    <YAxis 
                        hide={true} 
                        domain={domain} 
                    />
                    <Tooltip content={<CustomTooltip />} cursor={{ stroke: color, strokeWidth: 1, strokeDasharray: '5 5' }} />
                    <Area 
                        type="monotone" 
                        dataKey="value" 
                        stroke={color} 
                        strokeWidth={3} 
                        fillOpacity={1} 
                        fill="url(#colorValue)" 
                        animationDuration={1500}
                    />
                </AreaChart>
            </ResponsiveContainer>
        </div>
    );
}
