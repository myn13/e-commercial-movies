import React, { useState, useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { UserData } from "@/types/types";
import useToken from "@/components/logincard/useToken";
import Navbar from "@/components/navbar/Navbar";
import Page from "@/components/navbar/Page";

interface AuthWrapperProps {
    children: React.ReactNode;
    pages: Page[];
}

const AuthWrapper: React.FC<AuthWrapperProps> = ({ children, pages }) => {
    const { token, currentUser, logout } = useToken();
    const [redirectPath, setRedirectPath] = useState<string>("/movies");
    const location = useLocation();
    const navigate = useNavigate();

    useEffect(() => {
        if (location.pathname !== "/login") {
            setRedirectPath(location.pathname);
            localStorage.setItem('redirectPath', location.pathname);
        }
    }, [location.pathname]);

    const handleLoginSuccess = (user: UserData) => {
        console.log("Login successful for user:", user);
        navigate(redirectPath);
    };

    const handleLogout = () => {
        logout();
        navigate("/movies");
    };

    return (
        <>
            <Navbar
                pages={pages}
                isLoggedIn={!!token}
                currentUser={currentUser}
                onLogout={handleLogout}
            />
            <main className="page-body">
                {children}
            </main>
        </>
    );
};

export default AuthWrapper;
