// src/pages/MQTTConfigPage.jsx
import React, { useState } from 'react';
import Sidebar from './homeSidebar';
import { useNavigate } from 'react-router-dom';

export default function MQTTConfigPage() {
    const [endPoint, setEndPoint] = useState('');
    const [clientId, setClientId] = useState('');
    const [clientCert, setClientCert] = useState(null);
    const [clientKey, setClientKey] = useState(null);
    const [caCert, setCaCert] = useState(null);
    const [uploading, setUploading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setSuccess('');

        if (!endPoint || !clientId || !clientCert || !clientKey || !caCert) {
            setError('Please provide endpoint, client ID, and upload certificates/key.');
            return;
        }

        const formData = new FormData();
        formData.append('endPoint', endPoint);
        formData.append('clientId', clientId);
        formData.append('clientCert', clientCert);
        formData.append('clientKey', clientKey);
        formData.append('caCert', caCert);

        setUploading(true);
        try {
            const res = await fetch('http://localhost:2800/mqtt/config/upload', {
                method: 'POST',
                credentials: 'include',
                body: formData,
            });
            if (!res.ok) {
                const resp = await res.json();
                if (resp.code === 3) {
                    navigate('/home');
                    return;
                }
                throw new Error(resp.message);
            }
            setSuccess('Connection Successful!');
            navigate('/home');
        } catch (err) {
            console.error(err);
            setError(err.message || 'Connection failed, please try again!');
        } finally {
            setUploading(false);
        }
    };

    return (
        <div className="bg-[#FCE287] h-screen flex">
            <Sidebar />
            <div className="w-5/6 flex justify-center items-center p-8">
                <div className="bg-white rounded-lg shadow-md w-full max-w-md p-6">
                    <h3 className="text-xl font-semibold mb-4 text-center">MQTT Configuration Upload</h3>
                    <form onSubmit={handleSubmit} className="flex flex-col space-y-4">
                        <label className="flex flex-col text-sm">
                            Endpoint
                            <input
                                type="text"
                                value={endPoint}
                                onChange={(e) => setEndPoint(e.target.value)}
                                className="mt-1 p-2 border rounded focus:outline-none"
                                required
                            />
                        </label>
                        <label className="flex flex-col text-sm">
                            Client ID
                            <input
                                type="text"
                                value={clientId}
                                onChange={(e) => setClientId(e.target.value)}
                                className="mt-1 p-2 border rounded focus:outline-none"
                                required
                            />
                        </label>
                        <label className="flex flex-col text-sm">
                            Device Certification (PEM)
                            <input
                                type="file"
                                accept=".pem,.crt"
                                onChange={(e) => setClientCert(e.target.files[0])}
                                required
                            />
                        </label>
                        <label className="flex flex-col text-sm">
                            Private Key (PEM)
                            <input
                                type="file"
                                accept=".pem,.key"
                                onChange={(e) => setClientKey(e.target.files[0])}
                                required
                            />
                        </label>
                        <label className="flex flex-col text-sm">
                            CA Certification
                            <input
                                type="file"
                                accept=".pem,.crt"
                                onChange={(e) => setCaCert(e.target.files[0])}
                                required
                            />
                        </label>

                        {error && <p className="text-red-500 text-sm text-center">{error}</p>}
                        {success && <p className="text-green-600 text-sm text-center">{success}</p>}

                        <button
                            type="submit"
                            disabled={uploading}
                            className={`py-2 rounded ${uploading ? 'bg-gray-300' : 'bg-blue-500 hover:bg-blue-600 text-white'} transition`}
                        >
                            {uploading ? 'Uploading…' : 'Upload'}
                        </button>
                        <button
                            type="button"
                            onClick={() => navigate(-1)}
                            className="mt-2 py-2 rounded bg-gray-200 hover:bg-gray-300 text-gray-700"
                        >
                            Cancel
                        </button>
                    </form>
                </div>
            </div>
        </div>
    );
}
