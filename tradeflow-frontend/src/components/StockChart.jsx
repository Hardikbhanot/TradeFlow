import { useEffect, useRef } from 'react';
import { createChart, ColorType, LineSeries } from 'lightweight-charts';

export default function StockChart({ data, symbol }) {
    const chartContainerRef = useRef();

    useEffect(() => {
        if (!data || data.length === 0 || !chartContainerRef.current) return;

        const chart = createChart(chartContainerRef.current, {
            layout: {
                background: { type: ColorType.Solid, color: 'transparent' },
                textColor: '#888',
                fontSize: 10,
            },
            grid: {
                vertLines: { visible: false },
                horzLines: { visible: false },
            },
            width: chartContainerRef.current.clientWidth,
            height: 100,
            timeScale: { visible: false },
            rightPriceScale: { visible: false },
            handleScroll: false,
            handleScale: false,
        });

        const series = chart.addSeries(LineSeries, {
            color: '#2962FF',
            lineWidth: 2,
            priceLineVisible: false,
            lastValueVisible: false,
        });

        const formattedData = data.map((p, index) => ({
            time: index,
            value: typeof p.price === 'number' ? p.price : parseFloat(p.price),
        })).filter(p => !isNaN(p.value));

        if (formattedData.length > 0) {
            series.setData(formattedData);

            const isUp = formattedData[formattedData.length - 1].value >= formattedData[0].value;
            series.applyOptions({
                color: isUp ? '#26a69a' : '#ef5350',
            });
        }

        const handleResize = () => {
            if (chartContainerRef.current) {
                chart.applyOptions({ width: chartContainerRef.current.clientWidth });
            }
        };

        window.addEventListener('resize', handleResize);

        return () => {
            window.removeEventListener('resize', handleResize);
            chart.remove();
        };
    }, [data, symbol]);

    return (
        <div 
            ref={chartContainerRef} 
            style={{ width: '100%', height: '100px', pointerEvents: 'none' }} 
        />
    );
}
