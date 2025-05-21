import React, { useState } from 'react';
import { useNavigate } from "react-router-dom";

function RegisterForm() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [ConfirmPassword, setConfirmPassword] = useState('');
    const [username, setName] = useState('');
    const [error, setError] = useState('');
    const [isEmailValid, setIsEmailValid] = useState(false);
    const navigate = useNavigate();

    const validateEmail = (email) => {
        const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return re.test(email);
    };

    const handleEmailChange = (e) => {
        const value = e.target.value;
        setEmail(value);
        setIsEmailValid(validateEmail(value));
    };

    const validatePassword = (password) =>{
        const regex = /^(?=.*[a-z])(?=.*[A-Z]).{8,}$/;
        return regex.test(password);
    };

    const repeatPassword = (password,ConfirmPassword) =>{
        return password == ConfirmPassword;
    };


    const handleRegister = () => {
        if (!username || !email || !password) {
            setError('Please fill out all fields');
        } else if (!isEmailValid) {
            setError('Invalid email format');
        } else if (!validatePassword(password)){
            setError('Please enter a password that includes at least one uppercase letter, one lowercase letter, and is at least 8 characters in length')
        } else if (!repeatPassword(password,ConfirmPassword)){
            setError('Please make sure the passwords match')
        } else {
            fetch('http://localhost:2800/register', {
                method: 'POST',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({username, email, password }),
            })
                .then(res => {
                    if (!res.ok) {
                        return res.json().then(err => { throw new Error(err.msg || 'Register failed'); });
                    }
                    return res.json();
                })
                .then(data => {
                    setError('Register successful');
                    navigate("/home");
                })
                .catch(err => setError(err.message));
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-100">
            <div className="w-full max-w-md bg-white p-8 rounded-lg shadow-md">
                <h2 className="text-2xl font-bold text-center mb-6">Register at CatHome</h2>
                {error && (
                    <div className={`mb-4 px-4 py-2 rounded text-sm ${error === 'Register successful' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                        {error}
                    </div>
                )}
                <div className="space-y-4">
                    <input type="text" value={username} onChange={(e) => setName(e.target.value)} className="w-full border px-3 py-2 rounded" placeholder="Name" />

                    <input type="email" value={email} onChange={handleEmailChange} className="w-full border px-3 py-2 rounded" placeholder="Email" />
                    {!isEmailValid && email && <p className="text-xs text-red-500">Invalid email format</p>}

                    <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} className="w-full border px-3 py-2 rounded" placeholder="Password" />
                    <input type="password" value={ConfirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} className="w-full border px-3 py-2 rounded" placeholder="Confirm Password" />
                    <div className="text-sm mt-1 space-y-1">
                        <p className={/[A-Z]/.test(password) ? "text-green-600" : "text-red-500"}>
                            • At least one uppercase letter
                        </p>
                        <p className={/[a-z]/.test(password) ? "text-green-600" : "text-red-500"}>
                            • At least one lowercase letter
                        </p>
                        <p className={password.length >= 8 ? "text-green-600" : "text-red-500"}>
                            • Minimum 8 characters
                        </p>
                    </div>
                    <button onClick={handleRegister} className="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700">Register</button>
                </div>
            </div>
        </div>
    );
}

export default RegisterForm;
