import React, { useState, useMemo, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'

/**
 * Step1: Basic Information with inline validations and availability check
 */
const Step1 = React.memo(function Step1({
                                            form,
                                            setForm,
                                            confirmPassword,
                                            setConfirmPassword,
                                            onNext,
                                            error,
                                            isPasswordValidations,
                                            isEmailValid,
                                        }) {
    return (
        <div className="space-y-4">
            <h2 className="text-2xl font-bold text-center mb-6">
                Step 1: Basic Information
            </h2>
            {error && <div className="text-red-500">{error}</div>}

            <input
                className="w-full border px-3 py-2 rounded"
                placeholder="Username"
                value={form.username}
                onChange={e => setForm(f => ({ ...f, username: e.target.value }))}
            />
            {form.username && /\s/.test(form.username) && (
                <p className="text-xs text-red-500">Username cannot contain spaces.</p>
            )}

            <input
                className="w-full border px-3 py-2 rounded"
                placeholder="Email"
                value={form.email}
                onChange={e => setForm(f => ({ ...f, email: e.target.value }))}
            />
            {form.email && !isEmailValid && (
                <p className="text-xs text-red-500">Invalid email format</p>
            )}

            <input
                type="password"
                className="w-full border px-3 py-2 rounded"
                placeholder="Password"
                value={form.password}
                onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
            />
            <div className="text-sm mt-1 space-y-1">
                <p
                    className={
                        isPasswordValidations.hasUpper ? 'text-green-600' : 'text-red-500'
                    }
                >
                    • At least one uppercase letter
                </p>
                <p
                    className={
                        isPasswordValidations.hasLower ? 'text-green-600' : 'text-red-500'
                    }
                >
                    • At least one lowercase letter
                </p>
                <p
                    className={
                        isPasswordValidations.minLength ? 'text-green-600' : 'text-red-500'
                    }
                >
                    • Minimum 8 characters
                </p>
            </div>

            <input
                type="password"
                className="w-full border px-3 py-2 rounded"
                placeholder="Confirm Password"
                value={confirmPassword}
                onChange={e => setConfirmPassword(e.target.value)}
            />
            {confirmPassword && form.password !== confirmPassword && (
                <p className="text-xs text-red-500">Passwords do not match</p>
            )}

            <button
                onClick={onNext}
                className="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700"
            >
                Next: Upload Avatar
            </button>
        </div>
    )
})

/**
 * Step2: Avatar upload
 */
const Step2 = React.memo(function Step2({
                                            form,
                                            file,
                                            preview,
                                            handleFileChange,
                                            onComplete,
                                            error,
                                            navigate,
                                        }) {
    if (!form.username) {
        navigate('/register')
        return null
    }

    return (
        <div className="space-y-4">
            <h2 className="text-2xl font-bold text-center mb-6">
                Step 2: Upload Avatar
            </h2>
            {error && <div className="text-red-500">{error}</div>}

            <input
                type="file"
                accept="image/*"
                onChange={handleFileChange}
                className="mb-4"
            />

            {preview && (
                <img
                    src={preview}
                    alt="Avatar preview"
                    className="w-32 h-32 rounded-full object-cover mb-4"
                />
            )}

            <button
                onClick={onComplete}
                className="w-full bg-green-600 text-white py-2 rounded hover:bg-green-700"
            >
                Complete Registration
            </button>
        </div>
    )
})

export default function RegisterWizard() {
    const [step, setStep] = useState(1)
    const [form, setForm] = useState({
        username: '',
        email: '',
        password: '',
    })
    const [confirmPassword, setConfirmPassword] = useState('')
    const [file, setFile] = useState(null)
    const [preview, setPreview] = useState('')
    const [error, setError] = useState('')
    const navigate = useNavigate()

    const validateEmail = useCallback(
        s => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(s),
        []
    )

    const validatePassword = useCallback(
        s => /^(?=.*[a-z])(?=.*[A-Z]).{8,}$/.test(s),
        []
    )

    // derive inline validation states
    const isPasswordValidations = useMemo(
        () => ({
            hasUpper: /[A-Z]/.test(form.password),
            hasLower: /[a-z]/.test(form.password),
            minLength: form.password.length >= 8,
        }),
        [form.password]
    )

    const isEmailValid = useMemo(
        () => validateEmail(form.email),
        [form.email, validateEmail]
    )

    // Async onNext with availability check
    const onNext = useCallback(async () => {
        const { username, email, password } = form

        // client-side validations
        if (!username || !email || !password) {
            setError('Please fill out all fields.')
            return
        }
        if (/\s/.test(username)) {
            setError('Username cannot contain spaces.')
            return
        }
        if (!isEmailValid) {
            setError('Invalid email format.')
            return
        }
        if (!validatePassword(password)) {
            setError(
                'Password must include at least one uppercase letter, one lowercase letter, and be at least 8 characters long.'
            )
            return
        }
        if (password !== confirmPassword) {
            setError('Passwords do not match.')
            return
        }

        // server-side availability check
        try {
            const res = await fetch('http://localhost:2800/check-availability', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ username, email }),
            })
            if (!res.ok) {
                const err = await res.json()
                throw new Error(err.msg || 'Error checking availability.')
            }
            const { usernameExists, emailExists } = await res.json()
            if (usernameExists) {
                setError('Username is already taken.')
                return
            }
            if (emailExists) {
                setError('Email is already registered.')
                return
            }
        } catch (e) {
            setError(e.message)
            return
        }

        // all checks passed
        setError('')
        setStep(2)
    }, [form, confirmPassword, isEmailValid, validatePassword])

    const handleFileChange = useCallback(e => {
        const f = e.target.files[0]
        if (f && f.type.startsWith('image/') && f.size <= 2_000_000) {
            setFile(f)
            setPreview(URL.createObjectURL(f))
            setError('')
        } else {
            setError('Please select an image no larger than 2MB.')
        }
    }, [])

    const onComplete = useCallback(async () => {
        if (!file) {
            setError('Please select an avatar.')
            return
        }

        try {
            const regRes = await fetch('http://localhost:2800/register', {
                method: 'POST',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(form),
            })
            if (!regRes.ok) {
                const err = await regRes.json()
                throw new Error(err.msg || 'Registration failed.')
            }
            const { id } = await regRes.json()

            const fd = new FormData()
            fd.append('file', file)
            const upRes = await fetch(
                `http://localhost:2800/users/${id}/avatar`,
                {
                    method: 'POST',
                    body: fd,
                }
            )
            if (!upRes.ok) {
                const err = await upRes.json()
                throw new Error(err.msg || 'Avatar upload failed.')
            }

            navigate('/home', { state: { userId: id } })
        } catch (e) {
            setError(e.message)
        }
    }, [form, file, navigate])

    const step1Props = useMemo(
        () => ({
            form,
            setForm,
            confirmPassword,
            setConfirmPassword,
            onNext,
            error,
            isPasswordValidations,
            isEmailValid,
        }),
        [form, confirmPassword, onNext, error, isPasswordValidations, isEmailValid]
    )

    const step2Props = useMemo(
        () => ({
            form,
            file,
            preview,
            handleFileChange,
            onComplete,
            error,
            navigate,
        }),
        [form, file, preview, handleFileChange, onComplete, error, navigate]
    )

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-100">
            <div className="w-full max-w-md bg-white p-8 rounded-lg shadow-md">
                {step === 1 ? <Step1 {...step1Props} /> : <Step2 {...step2Props} />}
            </div>
        </div>
    )
}
