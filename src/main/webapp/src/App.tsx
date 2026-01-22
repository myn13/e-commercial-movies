import './App.css'
import {BrowserRouter as Router, Navigate, Route, Routes} from "react-router-dom";
import MoviesPage from "@/pages/MoviesPage";
import SingleMoviePage from "@/pages/SingleMoviePage"
import LoginPage from "@/pages/LoginPage"
import React from "react";
import SingleStarPage from "@/pages/SingleStarPage";
import AuthWrapper from "@/components/logincard/AuthWrapper";
import ShoppingCart from "@/components/shoppingcard/ShoppingCart";
import PaymentCard from "@/components/paymentcard/PaymentCard";
import useToken from "@/components/logincard/useToken";

// Disabled: Login protection is currently disabled
const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    // Always allow access (login disabled)
    return <>{children}</>;
};
export const App: React.FC = () => {
    const pages = [
        {name: "Home", href: "/"},
        {name: "Movies", href: "/movies"},
        {name: "Stars", href: "/stars"},
    ];

    return (
        <Router>
            <AuthWrapper pages={pages}>
                <Routes>
                    <Route path="/" element={<Navigate to="/movies" replace/>}/>
                    <Route path="/login" element={<LoginPage/>}/>
                    <Route path="/movies" element={<ProtectedRoute><MoviesPage/></ProtectedRoute>}/>
                    <Route path="/movies/:id" element={<ProtectedRoute><SingleMoviePage/></ProtectedRoute>}/>
                    <Route path="/stars/:id" element={<ProtectedRoute><SingleStarPage/></ProtectedRoute>}/>
                    <Route path="/shopping-cart" element={<ProtectedRoute><ShoppingCart/></ProtectedRoute>}/>
                    <Route path="/payment" element={<ProtectedRoute><PaymentCard/></ProtectedRoute>}/>
                    <Route path="/genres" element={<ProtectedRoute><h1>Genres</h1></ProtectedRoute>}/>
                    <Route path="/stars" element={<ProtectedRoute><h1>Stars</h1></ProtectedRoute>}/>
                    <Route path="*" element={<ProtectedRoute><h1>Not Found</h1></ProtectedRoute>}/>
                </Routes>
            </AuthWrapper>
        </Router>
    );
}

export default App;

