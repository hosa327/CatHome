import React, { useState } from "react";
import Sidebar from "./homeSidebar";
import { useLocation, useNavigate } from 'react-router-dom';

export default function Home() {
    const [showMqttModal, setShowMqttModal] = useState(false);

    const navigate = useNavigate();
    const location = useLocation();
    const { userId } = location.state || {};

    return (
        <div className="bg-[#FCE287] h-screen flex">
            <Sidebar />

            <div className="w-5/6 flex justify-around items-center p-8">
                {/* Water Card */}
                <div className="bg-white rounded-lg shadow-md w-1/4 h-96 flex flex-col items-center justify-start p-4">
                    <h2 className="text-blue-500 text-2xl font-semibold mb-4">Water</h2>
                </div>
                {/* Food Card */}
                <div className="bg-white rounded-lg shadow-md w-1/4 h-96 flex flex-col items-center justify-start p-4">
                    <h2 className="text-green-500 text-2xl font-semibold mb-4">Food</h2>
                </div>
                {/* Poop Card */}
                <div className="bg-white rounded-lg shadow-md w-1/4 h-96 flex flex-col items-center justify-start p-4">
                    <h2 className="text-yellow-700 text-2xl font-semibold mb-4">Poop</h2>
                </div>
            </div>
        </div>
    );
}