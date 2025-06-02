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
    const [userId, setUserId] = useState(null);
    const [catList, setCatList] = useState([]);
    const [selectedCat, setSelectedCat] = useState(null)

    const colorMap = {
        water: "text-blue-500",
        food:  "text-green-500",
        poop:  "text-yellow-700",
    };
    useEffect(() => {
        const params = new URLSearchParams();
        params.append('info', 'userId');
        fetch(`http://localhost:2800/userInfo?${params.toString()}`, {
            method: 'GET',
            credentials: 'include',
        })
            .then(async res => {
                if (!res.ok) {
                    const response = await res.json();
                    alert(response.message);
                    if (response.code === 3) {
                        navigate('/login');
                        return;
                    }
                    throw new Error(response.message);
                }
                return res.json();
            })
            .then(data => {
                setUserId(data.userId);
            })
            .catch(err => {
                console.error('Failed to get user information!', err);
            });
    }, []);


    useEffect(() => {
        if (!userId) return;
        const sock = new SockJS("http://localhost:2800/ws");
        const stomp = new Client({
            webSocketFactory: () => sock,
            reconnectDelay: 2000,
        });
        stomp.onConnect = () => {
            setClient(stomp);

            stomp.subscribe(`/topic/${userId}/catList`, (msg) =>{
                const CatList = JSON.parse(msg.body);
                setCatList(CatList);
            })

            console.log("request Cat list")
            stomp.publish({
                destination: "/app/requestCatList",
                body: JSON.stringify({ userId }),
            });
        };

        stomp.activate();

        return () => {
            if (client) client.deactivate();
        };
    }, [userId]);


    useEffect(()=>{
        if (!client || catList.length === 0) return;

        if(selectedCat === null) {
            const cat = catList[0];
            console.log("cat:",cat);
            setSelectedCat(cat);
        }

        console.log(catList);
    }, [catList, client, selectedCat]);


    useEffect(() => {
        if (!client || !userId || !selectedCat) return;

        const topic = `/topic/${userId}/${selectedCat}`;
        console.log("Subscribe topic:", topic);

        const subscription = client.subscribe(topic, (msg) => {
            const parsed = JSON.parse(msg.body);
            console.log("New Data：", parsed);
            setData(parsed);
        });

        console.log("request message");
        client.publish({
            destination: "/app/requestLatest",
            body: JSON.stringify({ userId, catName: selectedCat }),
        });

        return () => subscription.unsubscribe();
    }, [client, userId, selectedCat]);



    const topics = Object.keys(data).filter((k) => k !== "catName");

    const rows = [];
    for (let i = 0; i < topics.length; i += 2) {
        rows.push(topics.slice(i, i + 2));
    }

    const handleExport = (topic) => {
        const url =
            `http://localhost:2800/export/topic-data` +
            `?userId=${userId}` +
            `&topicName=${encodeURIComponent(topic)}` +
            `&catName=${encodeURIComponent(selectedCat)}`;
        window.location.href = url;
    };

    return (
        <div className="bg-[#FCE287] h-screen flex">
            <Sidebar />

            <div className="w-5/6 p-8 overflow-auto">

                {catList.length > 0 && (
                    <div className="flex justify-center gap-4 mb-6">
                        {catList.map((cat) => (
                            <button
                                key={cat}
                                className={`px-4 py-2 rounded-full border font-semibold transition
                                    ${selectedCat === cat
                                    ? 'bg-yellow-600 text-white'
                                    : 'bg-white text-gray-800 hover:bg-yellow-100 border-gray-400'}
                                `}
                                onClick={() => setSelectedCat(cat)}
                            >
                                {cat}
                            </button>
                        ))}
                    </div>
                )}

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

                                        <button
                                            className="mt-4 px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
                                            onClick={() => handleExport(topic)}
                                        >
                                            Export
                                        </button>
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