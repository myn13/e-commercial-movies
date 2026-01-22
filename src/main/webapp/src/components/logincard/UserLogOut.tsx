import React from 'react';
import useToken from './useToken';

interface UserLogOutProps {
    onLogoutSuccess?: () => void;
}

const UserLogOut: React.FC<UserLogOutProps> = ({ onLogoutSuccess }) => {
    const { logout } = useToken();

    const handleLogout = () => {
        try {
            console.log('UserLogOut: Starting logout process');
            logout(); // This handles localStorage cleanup
            console.log('UserLogOut: Logout successful');
            
            if (onLogoutSuccess) {
                onLogoutSuccess();
            }
        } catch (error) {
            console.error('UserLogOut: Logout error:', error);
        }
    };

    return (
        <button 
            onClick={handleLogout}
            className="logout-button"
        >
            Logout
        </button>
    );
};

export default UserLogOut;