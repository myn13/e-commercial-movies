import React from 'react';

interface CardBrandIconProps {
    brand: string;
    size?: number;
}

const CardBrandIcon: React.FC<CardBrandIconProps> = ({ brand, size = 24 }) => {
    const iconStyle = {
        width: size,
        height: size,
        display: 'inline-block'
    };

    const getBrandIcon = () => {
        switch (brand.toLowerCase()) {
            case 'visa':
                return (
                    <svg viewBox="0 0 24 16" style={iconStyle} fill="currentColor">
                        <rect width="24" height="16" rx="2" fill="#1A1F71"/>
                        <text x="12" y="11" textAnchor="middle" fontSize="8" fill="white" fontWeight="bold">VISA</text>
                    </svg>
                );
            case 'mastercard':
                return (
                    <svg viewBox="0 0 24 16" style={iconStyle} fill="currentColor">
                        <rect width="24" height="16" rx="2" fill="#000"/>
                        <circle cx="9" cy="8" r="6" fill="#EB001B"/>
                        <circle cx="15" cy="8" r="6" fill="#F79E1B"/>
                        <path d="M12 2c1.5 1.5 2.5 3.5 2.5 6s-1 4.5-2.5 6c-1.5-1.5-2.5-3.5-2.5-6s1-4.5 2.5-6z" fill="#FF5F00"/>
                    </svg>
                );
            case 'amex':
                return (
                    <svg viewBox="0 0 24 16" style={iconStyle} fill="currentColor">
                        <rect width="24" height="16" rx="2" fill="#006FCF"/>
                        <text x="12" y="10" textAnchor="middle" fontSize="6" fill="white" fontWeight="bold">AMEX</text>
                    </svg>
                );
            case 'discover':
                return (
                    <svg viewBox="0 0 24 16" style={iconStyle} fill="currentColor">
                        <rect width="24" height="16" rx="2" fill="#FF6000"/>
                        <circle cx="6" cy="8" r="2" fill="white"/>
                        <text x="12" y="10" textAnchor="middle" fontSize="5" fill="white" fontWeight="bold">DISCOVER</text>
                    </svg>
                );
            case 'diners':
                return (
                    <svg viewBox="0 0 24 16" style={iconStyle} fill="currentColor">
                        <rect width="24" height="16" rx="2" fill="#0079BE"/>
                        <text x="12" y="10" textAnchor="middle" fontSize="5" fill="white" fontWeight="bold">DINERS</text>
                    </svg>
                );
            case 'jcb':
                return (
                    <svg viewBox="0 0 24 16" style={iconStyle} fill="currentColor">
                        <rect width="24" height="16" rx="2" fill="#003A70"/>
                        <text x="12" y="10" textAnchor="middle" fontSize="6" fill="white" fontWeight="bold">JCB</text>
                    </svg>
                );
            default:
                return (
                    <svg viewBox="0 0 24 16" style={iconStyle} fill="currentColor">
                        <rect width="24" height="16" rx="2" fill="#666"/>
                        <text x="12" y="10" textAnchor="middle" fontSize="6" fill="white">💳</text>
                    </svg>
                );
        }
    };

    return getBrandIcon();
};

export default CardBrandIcon;
