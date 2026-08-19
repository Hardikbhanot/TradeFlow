/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState, useMemo, useCallback, useEffect } from 'react';
import { jwtDecode } from 'jwt-decode';
import api, { SESSION_EXPIRED_EVENT } from '../api/axios';

const AuthContext = createContext(null);

function parseUserFromToken(token) {
    const decoded = jwtDecode(token);
    return {
        userId: decoded.userId || decoded.sub,
        username: decoded.username || decoded.name || 'Trader',
        email: decoded.email || '',
    };
}

export function AuthProvider({ children }) {
    const [token, setToken] = useState(() => {
        const stored = localStorage.getItem('tf_token');
        if (!stored) return null;

        try {
            parseUserFromToken(stored);
            return stored;
        } catch {
            localStorage.removeItem('tf_token');
            return null;
        }
    });

    const user = useMemo(() => {
        if (!token) return null;
        try {
            return parseUserFromToken(token);
        } catch {
            return null;
        }
    }, [token]);

    useEffect(() => {
        const handleSessionExpired = () => setToken(null);
        window.addEventListener(SESSION_EXPIRED_EVENT, handleSessionExpired);
        return () => window.removeEventListener(SESSION_EXPIRED_EVENT, handleSessionExpired);
    }, []);

    const login = useCallback((jwt) => {
        localStorage.setItem('tf_token', jwt);
        setToken(jwt);
    }, []);

    const logout = useCallback(async () => {
        try {
            await api.post('/auth/logout');
        } catch {
            // ignore network/auth errors to ensure user gets logged out locally
        } finally {
            localStorage.removeItem('tf_token');
            setToken(null);
        }
    }, []);

    return (
        <AuthContext.Provider value={{ user, token, login, logout, isAuthenticated: !!token && !!user }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    return useContext(AuthContext);
}
