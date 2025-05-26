import React, {useEffect, useState} from 'react';
import Sidebar from './homeSidebar';
import { useNavigate } from 'react-router-dom';

export default function SubscribeTopics() {
    const [topics, setTopics] = useState(['']);
    const navigate = useNavigate();

    const handleChange = (index, value) => {
        const newTopics = [...topics];
        newTopics[index] = value;
        setTopics(newTopics);
    };

    const addTopic = () => setTopics([...topics, '']);

    const removeTopic = (index) => {
        const newTopics = topics.filter((_, i) => i !== index);
        setTopics(newTopics.length > 0 ? newTopics : ['']);
    };

    useEffect(() => {
        async function fetchSubscriptions(){
            try {
                const res = await fetch('http://localhost:2800/mqtt/subscriptions', {
                    method: 'POST',
                    credentials: 'include',
                });
                const result = await res.json();

                if (!res.ok || result.code !== 1) {
                    if (result.code === 3) {
                        alert(result.message);
                        navigate('/home');
                        return;
                    }
                    throw new Error(result.message || 'Failed to load subscriptions');
                }

                const existing = Array.isArray(result.data) && result.data.length
                    ? result.data
                    : [''];
                setTopics(existing);

            } catch (err) {
                console.error('fetchSubscriptions error', err);
                setTopics(['']);
            }
        }
        fetchSubscriptions();
    }, [navigate]);

    const handleSubmit = async () => {
        try {
            const res = await fetch('http://localhost:2800/mqtt/subscribeTopic', {
                method: 'POST',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(topics),
            });
            if (!res.ok) {
                const response = await res.json();
                alert(`Subscribe failed: ${response.message}`);
                if (response.code === 3) {
                    navigate('/home');
                    return;
                }
                throw new Error(response.message);
            }
            const data = await res.json();
            alert(`Subscribe success: ${data.message || 'OK'}`);
            navigate('/home')
            return;
        } catch (err) {
            console.error('Exception:', err);
        }
    };

    return (
        <div className="bg-[#FCE287] h-screen flex">
            <Sidebar />
            {/* Main Content Area */}
            <div className="w-5/6 flex justify-center items-center p-8">
                <div className="bg-white rounded-lg shadow-md w-full max-w-md p-6 relative">
                    <h2 className="text-2xl font-semibold text-indigo-600 mb-6 text-center">
                        Subscribe Topics
                    </h2>

                    <div className="space-y-4 mb-4">
                        {topics.map((topic, idx) => (
                            <div key={idx} className="flex items-center">
                                <input
                                    type="text"
                                    value={topic}
                                    onChange={(e) => handleChange(idx, e.target.value)}
                                    placeholder={`Topic ${idx + 1}`}
                                    className="flex-1 px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-400"
                                />
                                <button
                                    onClick={() => removeTopic(idx)}
                                    className="ml-2 p-2 rounded-full bg-red-500 hover:bg-red-600 text-white focus:outline-none"
                                    aria-label="Delete Topic"
                                >
                                    &times;
                                </button>
                                {idx === topics.length - 1 && (
                                    <button
                                        onClick={addTopic}
                                        className="ml-2 p-2 bg-indigo-500 text-white rounded-full hover:bg-indigo-600 focus:outline-none"
                                        aria-label="Add Topic"
                                    >
                                        +
                                    </button>
                                )}
                            </div>
                        ))}
                    </div>

                    <button
                        onClick={handleSubmit}
                        className="w-full py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 focus:outline-none"
                    >
                        Submit
                    </button>
                </div>
            </div>
        </div>
    );
}
