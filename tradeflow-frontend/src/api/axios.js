import axios from 'axios';

export const SESSION_EXPIRED_EVENT = 'tradeflow:session-expired';

function expireSession() {
    localStorage.removeItem('tf_token');
    window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
    if (window.location.pathname !== '/login') {
        window.location.replace('/login');
    }
}

function hasExpired(token) {
    try {
        const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
        return typeof payload.exp === 'number' && payload.exp * 1000 <= Date.now();
    } catch {
        return true;
    }
}

const api = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/',
});


api.interceptors.request.use((config) => {
    const token = localStorage.getItem('tf_token');
    if (token) {
        if (hasExpired(token)) {
            expireSession();
            return Promise.reject(new axios.Cancel('Session expired'));
        }
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});


api.interceptors.response.use(
    (res) => res,
    (err) => {
        if (err.response?.status === 401) {
                expireSession();
        }
        return Promise.reject(err);
    }
);

export default api;
