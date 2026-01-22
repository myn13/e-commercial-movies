import React, { useState } from 'react';
import { useForm } from "react-hook-form";
import {UserData, LoginRequest } from '@/types/types';
import './LoginCard.modules.css';
import { loginUser } from '@/hooks/fetchers';
import useToken, { TOKEN_KEY, USER_KEY } from './useToken';
function UserLogIn( {onLoginSuccess}: {onLoginSuccess: (user: UserData)=>void }) {
    const { register, handleSubmit } = useForm<LoginRequest>();
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const { setToken } = useToken();

    console.log("UserLogIn component is rendering");
    const onSubmit = async (data: LoginRequest) => {
        setIsLoading(true);
        setError(null);

        try {
            console.log("Attempting login with:", data);
            const result = await loginUser(data);
            console.log("Login response received:", result);

            if (result.status === 'success') {
                const userData: UserData = {
                    username: data.username,
                    loginTime: new Date().toISOString()
                };

                // Store token and user data using useToken hook
                setToken({ token: 'authenticated' });
                localStorage.setItem(USER_KEY, JSON.stringify(userData));
                
                // Dispatch custom event to update auth state across components
                window.dispatchEvent(new CustomEvent('authUpdate'));
                
                onLoginSuccess(userData);
            } else {
                setError("Username or password is incorrect");
            }
        } catch (err) {
            setError("Login failed. Please try again.");
            console.error("Login error:", err);
        } finally {
            setIsLoading(false);
        }
    };
    return (
        <div className="window">
            <div className="login-card">
                <h2>SIGN IN</h2>
                
                {error && (
                    <div className="login-error">
                        <p>{error}</p>
                    </div>
                )}
                
                <form onSubmit={handleSubmit(onSubmit)} className="login-form">
                    <div className="form-group">
                        <label htmlFor="username">Username *</label>
                        <input
                            id="username"
                            type="text"
                            {...register("username", { required: true })}
                            disabled={isLoading}
                            placeholder="Enter your username"
                        />
                    </div>
                    
                    <div className="form-group">
                        <label htmlFor="password">Password *</label>
                        <input
                            id="password"
                            type="password"
                            {...register("password", { required: true })}
                            disabled={isLoading}
                            placeholder="Enter your password"
                        />
                    </div>
                    
                    <button
                        type="submit"
                        disabled={isLoading}
                        className="login-button"
                    >
                        {isLoading ? "Logging in..." : "Sign In"}
                    </button>
                </form>
            </div>
        </div>
    );
}

export default UserLogIn;
