// src/pages/Home.jsx
import React, { useState, useEffect } from "react";
import Sidebar from "./homeSidebar";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";

export default function Home() {
    // 存后端推过来的 { catName, topic1: {...}, topic2: {...}, ... }
    const [data, setData] = useState({});
    const [client, setClient] = useState(null);

    // 建WebSocket+STOMP连接，订阅 /topic/catData
    useEffect(() => {
        const sock = new SockJS("http://localhost:2800/ws");
        const stomp = new Client({
            webSocketFactory: () => sock,
            reconnectDelay: 5000,
        });
        stomp.onConnect = () => {
            stomp.subscribe("/topic/catData", (msg) => {
                try {
                    setData(JSON.parse(msg.body));
                } catch (e) {
                    console.error("Invalid JSON", e);
                }
            });
        };
        stomp.activate();
        setClient(stomp);

        return () => {
            if (client) client.deactivate();
        };
    }, []);

    // 过滤出主题键，不展示 catName 自身
    const topics = Object.keys(data).filter((k) => k !== "catName");

    return (
        <div className="bg-[#FCE287] h-screen flex">
            <Sidebar />

            <div className="w-5/6 p-8 overflow-auto">
                {/* Cat name */}
                {data.catName && (
                    <h1 className="text-4xl font-bold text-center mb-6">
                        {data.catName}
                    </h1>
                )}

                <div className="flex flex-wrap justify-around items-start gap-6">
                    {topics.map((topic) => (
                        <div
                            key={topic}
                            className="bg-white rounded-lg shadow-md w-1/4 h-96 flex flex-col items-start p-4"
                        >
                            {/* Name with topic */}
                            <h2 className="text-2xl font-semibold mb-4 capitalize">
                                {topic}
                            </h2>
                            {/* All payload key */}
                            {data[topic] && Object.entries(data[topic]).map(([key, val]) => (
                                <div
                                    key={key}
                                    className="w-full flex justify-between py-1 border-b last:border-0"
                                >
                                    <span className="font-medium">{key}</span>
                                    <span>{String(val)}</span>
                                </div>
                            ))}
                        </div>
                    ))}
                    {topics.length === 0 && (
                        <p className="text-gray-600">Waiting for data</p>
                    )}
                </div>
            </div>
        </div>
    );
}
