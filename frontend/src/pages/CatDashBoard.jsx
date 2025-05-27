// src/pages/CatDashboard.jsx
import React, { useState, useEffect, useRef } from 'react';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

export default function CatDashboard() {
    const [data, setData] = useState({
        temperature: null,
        weight: null,
        waterNeeded: null,
        time: null
    });
    const clientRef = useRef(null);

    useEffect(() => {
        // 1. 创建 STOMP 客户端
        const stompClient = new Client({
            // 告诉它如何创建底层 WebSocket
            webSocketFactory: () => new SockJS('http://localhost:2800/ws/catData'),
            // 连接成功后的回调
            onConnect: () => {
                console.log('WebSocket connected');
                // 2. 订阅后端广播的 /topic/catData
                stompClient.subscribe('/topic/catData', message => {
                    const payload = JSON.parse(message.body);
                    setData(payload);
                });
            },
            // 出错重连（可选）
            reconnectDelay: 5000,
            // 错误回调
            onStompError: frame => {
                console.error('Broker reported error: ' + frame.headers['message']);
                console.error('Detail: ' + frame.body);
            }
        });

        clientRef.current = stompClient;
        // 激活连接
        stompClient.activate();

        return () => {
            // 组件卸载时断开
            stompClient.deactivate();
        };
    }, []);

    return (
        <div style={{ padding: 20, fontFamily: 'sans-serif' }}>
            <h2>猫咪实时 Dashboard</h2>
            <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
                <Card title="体温 (℃)" value={data.temperature != null ? data.temperature.toFixed(1) : '--'} />
                <Card title="体重 (kg)" value={data.weight != null ? data.weight.toFixed(2) : '--'} />
                <Card title="需补水" value={data.waterNeeded != null ? (data.waterNeeded ? '是' : '否') : '--'} />
                <Card title="最后更新时间" value={data.time || '--'} wide />
            </div>
        </div>
    );
}

// 下面是 Card 子组件，你也可以直接写成 div
function Card({ title, value, wide }) {
    return (
        <div
            style={{
                border: '1px solid #ddd',
                borderRadius: 4,
                padding: 12,
                width: wide ? 300 : 150,
                boxSizing: 'border-box'
            }}
        >
            <div style={{ fontSize: 14, color: '#666' }}>{title}</div>
            <div style={{ fontSize: 24, marginTop: 4 }}>{value}</div>
        </div>
    );
}
