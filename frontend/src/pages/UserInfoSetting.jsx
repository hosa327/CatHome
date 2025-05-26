import React, { useState, useMemo } from 'react';
import Sidebar from './homeSidebar';

export default function UserInfoSetting() {
    const [name, setName] = useState('');
    const [catName, setCatName] = useState('');
    const [catBreed, setCatBreed] = useState('');
    const [email, setEmail] = useState('');
    const [passwordModalOpen, setPasswordModalOpen] = useState(false);
    const [currentPassword, setCurrentPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [passwordError, setPasswordError] = useState('');
    const [passwordSuccess, setPasswordSuccess] = useState('');
    const [passwordLoading, setPasswordLoading] = useState(false);

    const handleSubmit = (e) => {
        e.preventDefault();
        // TODO: Integrate with your API
        const payload = { name, catName, catBreed, email };
        console.log('Submitting profile:', payload);
        alert('Profile saved!');
    };

    // Password validation rules
    const passwordValidation = useMemo(() => ({
        hasUpper: /[A-Z]/.test(newPassword),
        hasLower: /[a-z]/.test(newPassword),
        minLength: newPassword.length >= 8,
    }), [newPassword]);

    const handleChangePassword = async (e) => {
        e.preventDefault();
        setPasswordError('');
        setPasswordSuccess('');
        if (!currentPassword || !newPassword) {
            setPasswordError('Please fill in both fields.');
            return;
        }
        // Inline validations
        if (!passwordValidation.hasUpper || !passwordValidation.hasLower || !passwordValidation.minLength) {
            setPasswordError('Password must include uppercase, lowercase letters, and be at least 8 characters.');
            return;
        }
        setPasswordLoading(true);
        try {
            const res = await fetch('http://localhost:2800/user/changePassword', {
                method: 'POST',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ currentPassword, newPassword }),
            });
            const data = await res.json();
            if (!res.ok) {
                throw new Error(data.message || 'Password change failed.');
            }
            setPasswordSuccess('Password updated successfully!');
            setTimeout(() => {
                setPasswordModalOpen(false);
                setCurrentPassword('');
                setNewPassword('');
                setPasswordSuccess('');
            }, 1500);
        } catch (err) {
            setPasswordError(err.message);
        } finally {
            setPasswordLoading(false);
        }
    };

    return (
        <div className="bg-[#FCE287] h-screen flex">
            <Sidebar />
            <div className="w-5/6 flex justify-center items-center p-8">
                <div className="bg-white rounded-lg shadow-md w-full max-w-2xl p-6">
                    <h2 className="text-2xl font-semibold text-yellow-600 mb-6">Edit Profile</h2>
                    <form onSubmit={handleSubmit} className="space-y-5 text-gray-700">
                        <div>
                            <label className="block mb-1 font-medium">Name</label>
                            <input
                                type="text"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                placeholder="Enter your name"
                                className="w-full p-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-yellow-300"
                            />
                        </div>
                        <div>
                            <label className="block mb-1 font-medium">Cat Name</label>
                            <input
                                type="text"
                                value={catName}
                                onChange={(e) => setCatName(e.target.value)}
                                placeholder="Enter your cat's name"
                                className="w-full p-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-yellow-300"
                            />
                        </div>
                        <div>
                            <label className="block mb-1 font-medium">Cat Breed</label>
                            <input
                                type="text"
                                value={catBreed}
                                onChange={(e) => setCatBreed(e.target.value)}
                                placeholder="Enter your cat's breed"
                                className="w-full p-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-yellow-300"
                            />
                        </div>
                        <div>
                            <label className="block mb-1 font-medium">Email</label>
                            <input
                                type="email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                placeholder="Enter your email address"
                                className="w-full p-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-yellow-300"
                            />
                        </div>
                        <div className="flex justify-between pt-6">
                            <button
                                type="button"
                                onClick={() => setPasswordModalOpen(true)}
                                className="text-yellow-600 underline hover:text-yellow-800"
                            >
                                Change Password
                            </button>
                            <button
                                type="submit"
                                className="bg-yellow-500 text-white px-6 py-2 rounded-lg hover:bg-yellow-600 transition"
                            >
                                Save
                            </button>
                        </div>
                    </form>

                    {passwordModalOpen && (
                        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center">
                            <div className="bg-white p-6 rounded-lg shadow-lg w-96">
                                <h3 className="text-lg font-semibold mb-4">Change Password</h3>
                                <form onSubmit={handleChangePassword} className="space-y-4">
                                    <div>
                                        <label className="block text-sm mb-1">Current Password</label>
                                        <input
                                            type="password"
                                            value={currentPassword}
                                            onChange={(e) => setCurrentPassword(e.target.value)}
                                            placeholder="Enter current password"
                                            className="w-full p-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-yellow-300"
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-sm mb-1">New Password</label>
                                        <input
                                            type="password"
                                            value={newPassword}
                                            onChange={(e) => setNewPassword(e.target.value)}
                                            placeholder="Enter new password"
                                            className="w-full p-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-yellow-300"
                                        />
                                    </div>
                                    <div className="text-sm mt-1 space-y-1">
                                        <p className={passwordValidation.hasUpper ? 'text-green-600' : 'text-red-500'}>
                                            • At least one uppercase letter
                                        </p>
                                        <p className={passwordValidation.hasLower ? 'text-green-600' : 'text-red-500'}>
                                            • At least one lowercase letter
                                        </p>
                                        <p className={passwordValidation.minLength ? 'text-green-600' : 'text-red-500'}>
                                            • Minimum 8 characters
                                        </p>
                                    </div>
                                    {passwordError && <p className="text-red-500 text-sm">{passwordError}</p>}
                                    {passwordSuccess && <p className="text-green-600 text-sm">{passwordSuccess}</p>}
                                    <div className="flex justify-end space-x-3 mt-4">
                                        <button
                                            type="button"
                                            onClick={() => setPasswordModalOpen(false)}
                                            className="px-4 py-2 rounded border"
                                        >
                                            Cancel
                                        </button>
                                        <button
                                            type="submit"
                                            disabled={passwordLoading}
                                            className={`px-4 py-2 bg-yellow-500 text-white rounded hover:bg-yellow-600 ${passwordLoading ? 'opacity-50 cursor-not-allowed' : ''}`}
                                        >
                                            {passwordLoading ? 'Updating...' : 'Submit'}
                                        </button>
                                    </div>
                                </form>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}