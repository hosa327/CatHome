import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

export default function Sidebar() {
    const navigate = useNavigate();

    const [avatarUrl, setAvatarUrl] = useState('http://localhost:2800/avatars/defaultAvatar.png');
    const [userId, setUserId] = useState(null);
    const [userName, setUserName] = useState('');
    const [isAwsConnected, setIsAwsConnected] = useState(false);

    const handleMqttClick = () => {
        navigate('/mqtt-config');
    };

    const handleEditProfile = () => {
        navigate('/userInfo')
    }
    const handleHome = () => {
        navigate('/home')
    }

    const handleAwsConnect = async () => {
        try {
            const res = await fetch('http://localhost:2800/mqtt/awsConnect', {
                method: 'POST',
                credentials: 'include',
            });
            if (!res.ok) {
                const response = await res.json();
                alert(`Connection failed: ${response.message}`);
                if (response.code === 3) {
                    navigate('/home');
                    return;
                }
                throw new Error(response.message);
            }
            const response = await res.json();
            alert(`Connection successful: ${response.message || 'OK'}`)
            setIsAwsConnected(true);
            return response;
        } catch (err) {
            console.error('Exception:', err);
        }
    };


    const handleSetTopic = () => {
        navigate('/subscribe_topic');
    };

    useEffect(() => {
        const params = new URLSearchParams();
        params.append('info', 'avatar');
        params.append('info', 'userId');
        params.append('info', 'userName');

        fetch(`http://localhost:2800/userInfo?${params.toString()}`, {
            method: 'GET',
            credentials: 'include',
        })
            .then(async res => {
                if (!res.ok) {
                    const response = await res.json();
                    alert(response.message);
                    if (response.code === 3) {
                        navigate('/home');
                        return;
                    }
                    throw new Error(response.message);
                }
                return res.json();
            })
            .then(data => {
                setUserId(data.userId);
                setAvatarUrl(data.avatarURL);
                setUserName(data.userName);
            })
            .catch(err => {
                console.error('Failed to get user information!', err);
            });
    }, []);

    const btnClass = 'w-56 px-4 py-2 bg-[#A9D18F] hover:bg-[#8EB37A] text-white transition font-bold rounded-lg border border-[#A9D18F]';
    const roundedFont = { fontFamily: 'Arial Rounded MT Bold, Helvetica Rounded, sans-serif' };

    return (
        <div className="relative w-1/6 h-full bg-[#F9C657] p-4 flex flex-col items-center">
            <img
                src={avatarUrl}
                alt="Profile Photo"
                className="w-20 h-20 rounded-full mt-4 object-cover"
                onError={e => {
                    e.currentTarget.onerror = null;
                    e.currentTarget.src = 'http://localhost:2800/avatars/defaultAvatar.png';
                }}
            />
            <p className="mt-2 text-gray-700 font-medium">{userName}</p>
            <p className="mt-2 text-gray-700 font-medium">User ID: {userId}</p>

            <div className="mt-2 flex items-center space-x-2">
                <span className="text-gray-700 font-medium">AWS Status:</span>
                <span
                    className={`inline-block w-3 h-3 rounded-full ${
                        isAwsConnected ? 'bg-green-500' : 'bg-red-500'
                    }`}
                ></span>
            </div>

            <div className="absolute top-[40%] left-1/2 transform -translate-x-1/2 flex flex-col items-center space-y-4 w-full">
                <button
                    className={btnClass}
                    style={roundedFont}
                    onClick={handleHome}
                >
                    Home
                </button>
                <button
                    className={btnClass}
                    style={roundedFont}
                    onClick={handleEditProfile}
                >
                    Edit Profile
                </button>
                <button
                    className={btnClass}
                    style={roundedFont}
                    onClick={handleMqttClick}
                >
                    Set MQTT Configuration
                </button>
                <button
                    className={btnClass}
                    style={roundedFont}
                    onClick={handleAwsConnect}
                >
                    Connect AWS Service
                </button>
                <button
                    className={btnClass}
                    style={roundedFont}
                    onClick={handleSetTopic}
                >
                    Set Topic
                </button>
            </div>
        </div>
    );
}