export async function fetchWithSession(url, options = {}, navigate) {
    try {
        const res = await fetch(url, { credentials: 'include', ...options });
        const data = await res.json();

        if (!res.ok) {
            if (data.code === 3) {
                // Session invalid or expired
                alert(data.message || 'Session expired, please login again.');
                navigate('/login');
                throw new Error('Session expired');
            } else {
                throw new Error(data.message || 'Unknown error');
            }
        }

        return data;
    } catch (error) {
        console.error('Request error:', error);
        throw error;
    }
}
