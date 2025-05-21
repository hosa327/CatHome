import React, {useContext, useEffect, useState} from "react";
import { useNavigate } from "react-router-dom";

function LoginPage() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [isEmailValid, setIsEmailValid] = useState(false);
    const navigate = useNavigate();

    const handleEmailChange = (e) => {
        const newEmail = e.target.value;
        setEmail(newEmail);
        const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        setIsEmailValid(emailPattern.test(newEmail));
    };

    const handleLogin = () => {
        fetch('http://localhost:2800/login', {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password }),
        })
            .then(res => {
                if (!res.ok) {
                    return res.json().then(err => { throw new Error(err.msg || 'Login failed'); });
                }
                return res.json();
            })
            .then(data => {
                const userId = data.id;
                setError('Login successful');
                // navigate("/home", {state:{userId}});
                navigate("/home");
            })
            .catch(err => setError(err.message));
    };

    const handleRegister = () =>{
        navigate("/register_avatar")
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-100">
            <div className="w-full max-w-md bg-white p-8 rounded-lg shadow-md">
                <h2 className="text-2xl font-bold text-center mb-6">Wellcome to CatHome</h2>
                {error && <div className={`mb-4 px-4 py-2 rounded text-sm ${error === 'Login successful' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>{error}</div>}
                <div className="space-y-4">
                    <input type="email" value={email} onChange={handleEmailChange} className="w-full border px-3 py-2 rounded" placeholder="Email" />
                    {!isEmailValid && email && <p className="text-xs text-red-500">Invalid email format</p>}
                    <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} className="w-full border px-3 py-2 rounded" placeholder="Password" />
                    <button onClick={handleLogin} className="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700">Login</button>
                    <button onClick={handleRegister} className="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700">Register</button>
                </div>
            </div>
        </div>
    );
}





export default LoginPage;
