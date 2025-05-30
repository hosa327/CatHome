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

    // Avatar states
    const [avatarModalOpen, setAvatarModalOpen] = useState(false);
    const [avatarFile, setAvatarFile] = useState(null);

    const handleSubmit = (e) => {
        e.preventDefault();
        const payload = { name, catName, catBreed, email };
        console.log('Submitting profile:', payload);
        alert('Profile saved!');
    };

    const passwordValidation = useMemo(() => ({
        hasUpper: /[A-Z]/.test(newPassword),
        hasLower: /[a-z]/.test(newPassword),
        minLength: newPassword.length >= 8,
    }), [newPassword]);

    const handleChangePassword = async (e) => {
        e.preventDefault();
        if (!name && !catName && !catBreed && !email) {
            alert('Please fill at least one profile field before saving.');
            return;
        }

        setPasswordError('');
        setPasswordSuccess('');
        if (!currentPassword || !newPassword) {
            setPasswordError('Please fill in both fields.');
            return;
        }
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
            if (!res.ok) throw new Error(data.message || 'Password change failed.');
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

    const handleAvatarUpload = async () => {
        if (!avatarFile) {
            alert('Please select an image file.');
            return;
        }
        const formData = new FormData();
        formData.append('file', avatarFile);

        try {
            const res = await fetch('http://localhost:2800/users/avatar', {
                method: 'POST',
                credentials: 'include',
                body: formData,
            });
            const data = await res.json();
            if (!res.ok) throw new Error(data.message || 'Upload failed.');
            alert('Avatar uploaded successfully!');
            setAvatarModalOpen(false);
            setAvatarFile(null);
        } catch (err) {
            alert(err.message);
        }
    };

    return (
        <div className="bg-[#FCE287] h-screen flex">
            <Sidebar />
            <div className="w-5/6 flex justify-center items-center p-8">
                <div className="bg-white rounded-lg shadow-md w-full max-w-2xl p-6">
                    <h2 className="text-2xl font-semibold text-yellow-600 mb-6">Edit Profile</h2>
                    <form onSubmit={handleSubmit} className="space-y-5 text-gray-700">
                        <input type="text" value={name} onChange={e => setName(e.target.value)} placeholder="Name" className="w-full p-2 border rounded"/>
                        <input type="text" value={catName} onChange={e => setCatName(e.target.value)} placeholder="Cat Name" className="w-full p-2 border rounded"/>
                        <input type="text" value={catBreed} onChange={e => setCatBreed(e.target.value)} placeholder="Cat Breed" className="w-full p-2 border rounded"/>
                        <input type="email" value={email} onChange={e => setEmail(e.target.value)} placeholder="Email" className="w-full p-2 border rounded"/>

                        <div className="flex justify-between pt-6">
                            <button type="button" onClick={() => setPasswordModalOpen(true)} className="text-yellow-600 underline hover:text-yellow-800">Change Password</button>
                            <button type="submit" className="bg-yellow-500 text-white px-6 py-2 rounded-lg hover:bg-yellow-600">Save</button>
                        </div>
                    </form>

                    <button onClick={() => setAvatarModalOpen(true)} className="mt-4 bg-gray-200 px-4 py-2 rounded hover:bg-gray-300">Change Avatar</button>

                    {passwordModalOpen && (
                        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center">
                            <div className="bg-white p-6 rounded-lg shadow-lg w-96">
                                <h3 className="text-lg font-semibold mb-4">Change Password</h3>
                                <input type="password" placeholder="Current Password" value={currentPassword} onChange={e => setCurrentPassword(e.target.value)} className="w-full p-2 border rounded mb-3"/>
                                <input type="password" placeholder="New Password" value={newPassword} onChange={e => setNewPassword(e.target.value)} className="w-full p-2 border rounded mb-3"/>
                                <div className="flex justify-end space-x-3 mt-4">
                                    <button onClick={() => setPasswordModalOpen(false)} className="px-4 py-2 rounded border">Cancel</button>
                                    <button onClick={handleChangePassword} disabled={passwordLoading} className="px-4 py-2 bg-yellow-500 text-white rounded hover:bg-yellow-600">
                                        {passwordLoading ? 'Updating...' : 'Submit'}
                                    </button>
                                </div>
                            </div>
                        </div>
                    )}

                    {avatarModalOpen && (
                        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center">
                            <div className="bg-white p-6 rounded-lg shadow-lg w-96">
                                <h3 className="text-lg font-semibold mb-4">Upload Avatar</h3>
                                <input type="file" accept="image/*" onChange={e => setAvatarFile(e.target.files[0])} className="w-full mb-4"/>
                                <div className="flex justify-end space-x-3">
                                    <button onClick={() => { setAvatarModalOpen(false); setAvatarFile(null); }} className="px-4 py-2 rounded border">Cancel</button>
                                    <button onClick={handleAvatarUpload} className="px-4 py-2 bg-yellow-500 text-white rounded hover:bg-yellow-600">Save</button>
                                </div>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
