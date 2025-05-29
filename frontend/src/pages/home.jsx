import React, {useState, useEffect, useRef} from "react";
import Sidebar from "./homeSidebar";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import { useLocation, useNavigate } from "react-router-dom";

export default function Home() {
    const [data, setData] = useState({});
    const [client, setClient] = useState(null);
    const navigate = useNavigate();
    const location = useLocation();
    const { userId } = location.state || {};

    const colorMap = {
        water: "text-blue-500",
        food:  "text-green-500",
        poop:  "text-yellow-700",
    };

    useEffect(() => {
        const sock = new SockJS("http://localhost:2800/ws");
        const stomp = new Client({
            webSocketFactory: () => sock,
            reconnectDelay: 2000,
        });
        stomp.onConnect = () => {
            stomp.subscribe("/topic/catData", (msg) => {
                try {
                    setData(JSON.parse(msg.body));
                } catch (e) {
                    console.error("Invalid JSON", e);
                }
            });
            // const catName = "Mimi";
            // stomp.publish({
            //     destination: "/app/requestLatest",
            //     body: JSON.stringify({userId, catName})
            // });
        };

        stomp.activate();
        setClient(stomp);

        return () => {
            if (client) client.deactivate();
        };
    }, []);


    const topics = Object.keys(data).filter((k) => k !== "catName");

    const rows = [];
    for (let i = 0; i < topics.length; i += 2) {
        rows.push(topics.slice(i, i + 2));
    }

    return (
        <div className="bg-[#FCE287] h-screen flex">
            <Sidebar />

            <div className="w-5/6 p-8 overflow-auto">
                {data.catName && (
                    <h1 className="text-4xl font-bold text-center mb-6">
                        {data.catName}
                    </h1>
                )}

                <div className="flex flex-col gap-6">
                    {rows.length > 0 ? (
                        rows.map((row, rowIndex) => (
                            <div
                                key={rowIndex}
                                className="flex justify-around items-start gap-6"
                            >
                                {row.map((topic) => (
                                    <div
                                        key={topic}
                                        className="bg-white rounded-lg shadow-md w-1/4 h-96 flex flex-col items-center justify-start p-4"
                                    >
                                        <h2
                                            className={`${colorMap[topic] || "text-gray-700"} text-2xl font-semibold mb-4 capitalize`}
                                        >
                                            {topic}
                                        </h2>
                                        {data[topic] &&
                                            Object.entries(data[topic]).map(([key, val]) => (
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
                                {row.length === 1 && <div className="w-1/4" />}
                            </div>
                        ))
                    ) : (
                        <p className="text-gray-600 w-full text-center">Waiting for data</p>
                    )}
                </div>
            </div>
        </div>
    );
}
