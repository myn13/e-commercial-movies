import {useState, useCallback, useEffect, useMemo} from 'react';
import {TokenState, UserData} from "@/types/types";

export const TOKEN_KEY = 'auth_token';
export const USER_KEY = 'currentUser';

export default function useToken() : TokenState {
    const [token, setTokenState] = useState<string|null>(() =>
                localStorage.getItem(TOKEN_KEY));
    const [currentUser, setCurrentUserState] = useState<UserData | null > (() => {
        const storedUser = localStorage.getItem(USER_KEY);
        try {
            return storedUser ? JSON.parse(storedUser) : null ;
        } catch (e){
            console.error("Could not parse current user from localStorage", e);
            return null;
        }
    });

    const setToken: TokenState['setToken'] = useCallback((data) => {
        localStorage.setItem(TOKEN_KEY, data.token);
        setTokenState(data.token);
        const storedUser = localStorage.getItem(USER_KEY);
        if (storedUser) {
            try {
                setCurrentUserState(JSON.parse(storedUser));
            } catch (e) {
                console.error("Could not parse current user from localStorage", e);
            }
        }
    }, []);

    const logout: TokenState['logout'] = useCallback(() => {
        console.log("Logout: Clearing local storage");
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
        setTokenState(null);
        setCurrentUserState(null);

        window.dispatchEvent(new CustomEvent('authUpdate'));
    }, []);

    useEffect(() => {
        const handleStorageChange = () => {
            const tokenValue = localStorage.getItem(TOKEN_KEY);
            const userValue = localStorage.getItem(USER_KEY);
            
            setTokenState(tokenValue);
            try {
                setCurrentUserState(userValue ? JSON.parse(userValue) : null);
            } catch (e) {
                setCurrentUserState(null);
            }
        };

        window.addEventListener('storage', handleStorageChange);
        window.addEventListener('authUpdate', handleStorageChange);
        
        return () => {
            window.removeEventListener('storage', handleStorageChange);
            window.removeEventListener('authUpdate', handleStorageChange);
        };
    }, []);

    const saveToken = (userToken: { token: any; }) => {
        localStorage.setItem('token', JSON.stringify(userToken));
        setToken(userToken.token);
    };

    return useMemo(() => ({
        token,
        currentUser,
        setToken,
        logout
    }), [token, currentUser, setToken, logout]);
}