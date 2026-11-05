import { useEffect, useCallback, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const getSocketUrl = () => {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
    // Remove trailing slash if present
    const cleanBaseUrl = baseUrl.replace(/\/$/, '');
    return `${cleanBaseUrl}/api/v1/market/ws`;
};
export const useWebSocket = (userId) => {
    const [marketData, setMarketData] = useState({});
    const [orderUpdates, setOrderUpdates] = useState(null);
    const clientRef = useRef(null);

    useEffect(() => {
        const client = new Client({
            webSocketFactory: () => new SockJS(getSocketUrl()),
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        client.onConnect = () => {
            console.log('Connected to WebSocket Hub');
            
            // 1. Subscribe to Live Market Data
            client.subscribe('/topic/market-data', (message) => {
                const data = JSON.parse(message.body);
                setMarketData(data);
            });

            // 2. Subscribe to Personal Order Updates
            if (userId) {
                client.subscribe(`/topic/orders/${userId}`, (message) => {
                    const update = JSON.parse(message.body);
                    setOrderUpdates(update);
                });
            }
        };

        client.onStompError = (frame) => {
            console.error('Broker reported error: ' + frame.headers['message']);
            console.error('Additional details: ' + frame.body);
        };

        client.activate();
        clientRef.current = client;

        return () => {
            if (clientRef.current) {
                clientRef.current.deactivate();
            }
        };
    }, [userId]);

    return { marketData, orderUpdates };
};
