import React from 'react';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

const data = [
    { name: '09:30', value: 2450000 },
    { name: '10:30', value: 2465000 },
    { name: '11:30', value: 2458000 },
    { name: '12:30', value: 2482000 },
    { name: '13:30', value: 2475000 },
    { name: '14:30', value: 2498000 },
    { name: '15:30', value: 2512450 },
];

const CustomTooltip = ({ active, payload }) => {
    if (active && payload && payload.length) {
        return (
            <div style={{ background: 'rgba(10, 14, 18, 0.9)', border: '1px solid var(--primary)', padding: '10px', borderRadius: '8px', backdropFilter: 'blur(10px)' }}>
                <p style={{ margin: 0, color: 'var(--text-muted)', fontSize: '0.7rem' }}>{payload[0].payload.name}</p>
                <p style={{ margin: 0, color: 'var(--primary)', fontWeight: 800, fontSize: '1.1rem' }}>₹{payload[0].value.toLocaleString('en-IN')}</p>
            </div>
        );
    }
    return null;
};

export default function EquityChart() {
    const firstVal = data[0]?.value ?? 0;
    const lastVal = data[data.length - 1]?.value ?? 0;
    const isDown = lastVal < firstVal;
    const color = isDown ? 'var(--red)' : 'var(--primary)';

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
                        domain={['dataMin - 10000', 'dataMax + 10000']} 
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
