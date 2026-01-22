import React from "react";
import { useNavigate } from "react-router-dom";
import UserLogIn from "@/components/logincard/UserLogIn";
import { UserData } from "@/types/types";

const LoginPage: React.FC = () => {
    const navigate = useNavigate();

    const handleLoginSuccess = (user: UserData) => {
        console.log("Login successful for user:", user);
        // Get the redirect path from localStorage or default to movies
        const redirectPath = localStorage.getItem('redirectPath') || '/movies';
        localStorage.removeItem('redirectPath');
        navigate(redirectPath);
    };

    return <UserLogIn onLoginSuccess={handleLoginSuccess} />;
};

export default LoginPage;
