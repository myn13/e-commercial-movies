import React, { useState, useEffect } from 'react';
import { getCartInfo, processPayment, getCartItems } from '@/hooks/fetchers';
import type { PaymentConfirmationResponse } from '@/types/types';
import CardBrandIcon from './CardBrandIcon';
import './PaymentCard.css';

interface PaymentFormProps {
    onPaymentSuccess: (result: PaymentConfirmationResponse) => void;
    onPaymentError: (error: string) => void;
}

const PaymentForm: React.FC<PaymentFormProps> = ({ onPaymentSuccess, onPaymentError }) => {
    const [isProcessing, setIsProcessing] = useState(false);
    const [cartInfo, setCartInfo] = useState<{ amount: number; total: number } | null>(null);
    const [cardBrand, setCardBrand] = useState<string>('');
    const [formData, setFormData] = useState({
        firstName: '',
        lastName: '',
        cardNumber: '',
        expiryDate: ''
    });

    useEffect(() => {
        loadCartInfo();
    }, []);

    const loadCartInfo = async () => {
        try {
            const info = await getCartInfo();
            setCartInfo(info);
        } catch (error) {
            console.error('Error loading cart info:', error);
        }
    };

    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
        
        if (name === 'cardNumber') {
            detectCardBrand(value);
        }
    };

    const detectCardBrand = (cardNumber: string) => {
        const cleanedNumber = cardNumber.replace(/\s/g, '');
        
        if (cleanedNumber.startsWith('4')) {
            setCardBrand('visa');
        } else if (cleanedNumber.startsWith('5') || cleanedNumber.startsWith('2')) {
            setCardBrand('mastercard');
        } else if (cleanedNumber.startsWith('3')) {
            if (cleanedNumber.startsWith('34') || cleanedNumber.startsWith('37')) {
                setCardBrand('amex');
            } else if (cleanedNumber.startsWith('30') || cleanedNumber.startsWith('36') || cleanedNumber.startsWith('38')) {
                setCardBrand('diners');
            }
        } else if (cleanedNumber.startsWith('6')) {
            setCardBrand('discover');
        } else if (cleanedNumber.startsWith('35')) {
            setCardBrand('jcb');
        } else {
            setCardBrand('');
        }
    };

    const formatCardNumber = (value: string) => {
        const cleaned = value.replace(/\D/g, '');
        return cleaned.replace(/(\d{4})(?=\d)/g, '$1 ');
    };

    const formatExpiryDate = (value: string) => {
        const cleaned = value.replace(/\D/g, '');
        if (cleaned.length >= 2) {
            return cleaned.substring(0, 2) + '/' + cleaned.substring(2, 4);
        }
        return cleaned;
    };

    const handleCardNumberChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const formatted = formatCardNumber(e.target.value);
        setFormData(prev => ({
            ...prev,
            cardNumber: formatted
        }));
        detectCardBrand(formatted);
    };

    const handleExpiryDateChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const formatted = formatExpiryDate(e.target.value);
        setFormData(prev => ({
            ...prev,
            expiryDate: formatted
        }));
    };

    const validateForm = () => {
        if (!formData.firstName.trim() || !formData.lastName.trim()) {
            return 'Please enter your first and last name';
        }
        
        if (!formData.cardNumber.trim()) {
            return 'Please enter your credit card number';
        }
        
        const cleanedCardNumber = formData.cardNumber.replace(/\s/g, '');
        if (cleanedCardNumber.length < 13 || cleanedCardNumber.length > 19) {
            return 'Please enter a valid credit card number';
        }
        
        if (!formData.expiryDate.trim()) {
            return 'Please enter expiry date';
        }
        
        // Validate MM/YY format
        const expiryRegex = /^(0[1-9]|1[0-2])\/(\d{2})$/;
        if (!expiryRegex.test(formData.expiryDate)) {
            return 'Please enter expiry date in MM/YY format';
        }
        
        const [monthStr, yearStr] = formData.expiryDate.split('/');
        const expiryMonth = parseInt(monthStr);
        const expiryYear = 2000 + parseInt(yearStr); // Convert YY to YYYY
        
        const currentYear = new Date().getFullYear();
        const currentMonth = new Date().getMonth() + 1;
        
        if (expiryYear < currentYear || (expiryYear === currentYear && expiryMonth < currentMonth)) {
            return 'Credit card has expired';
        }
        
        return null;
    };

    const handleSubmit = async (event: React.FormEvent) => {
        event.preventDefault();

        if (!cartInfo) {
            onPaymentError('Cart information not available');
            return;
        }

        // Validate form
        const validationError = validateForm();
        if (validationError) {
            onPaymentError(validationError);
            return;
        }

        setIsProcessing(true);

        try {
            // Get cart items to send actual purchase data
            const cartItemsResponse = await getCartItems();
            const cartItems = cartItemsResponse.cartItems;
            
            if (!cartItems || cartItems.length === 0) {
                onPaymentError('Your cart is empty');
                setIsProcessing(false);
                return;
            }

            // For now, use a default customer ID that exists in the database
            // In production, this should come from the user session
            const customerId = 1; // Use ID 1 as default (should exist)
            
            // Process each cart item
            for (const item of cartItems) {
                for (let i = 0; i < item.quantity; i++) {
                    const paymentData = {
                        customerId: customerId,
                        movieId: item.id,
                        saleDate: new Date().toISOString().split('T')[0]
                    };

                    const result = await processPayment(paymentData);
                    
                    if (result.status !== 'success') {
                        onPaymentError(result.message || 'Payment failed for some items');
                        setIsProcessing(false);
                        return;
                    }
                }
            }
            
            // If all items processed successfully
            const successResult = {
                status: 'succeeded',
                message: 'Payment successful!',
                transactionId: `TXN_${Date.now()}`
            };
            onPaymentSuccess(successResult);
            
        } catch (error) {
            onPaymentError(error instanceof Error ? error.message : 'Payment failed');
        } finally {
            setIsProcessing(false);
        }
    };

    if (!cartInfo) {
        return <div>Loading cart information...</div>;
    }

    return (
        <form onSubmit={handleSubmit} className="payment-form">
            <div className="payment-summary">
                <h3>Payment Summary</h3>
                <p>Items: {cartInfo.amount}</p>
                <p>Total: ${cartInfo.total.toFixed(2)}</p>
            </div>

            <div className="customer-info">
                <h3>Customer Information</h3>
                <div className="form-row">
                    <div className="form-group">
                        <label htmlFor="firstName">First Name *</label>
                        <input
                            type="text"
                            id="firstName"
                            name="firstName"
                            value={formData.firstName}
                            onChange={handleInputChange}
                            required
                            placeholder="Enter your first name"
                        />
                    </div>
                    <div className="form-group">
                        <label htmlFor="lastName">Last Name *</label>
                        <input
                            type="text"
                            id="lastName"
                            name="lastName"
                            value={formData.lastName}
                            onChange={handleInputChange}
                            required
                            placeholder="Enter your last name"
                        />
                    </div>
                </div>
            </div>

            <div className="card-element-container">
                <div className="form-row">
                    <div className="form-group">
                        <label htmlFor="expiryDate">Card Number *</label>
                        <input
                            type="text"
                            id="cardNumber"
                            name="cardNumber"
                            value={formData.cardNumber}
                            onChange={handleCardNumberChange}
                            placeholder="1234 5678 9012 3456"
                            maxLength={19}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label htmlFor="expiryDate">Expiry Date *</label>
                        <input
                            type="text"
                            id="expiryDate"
                            name="expiryDate"
                            value={formData.expiryDate}
                            onChange={handleExpiryDateChange}
                            placeholder="MM/YY"
                            maxLength={5}
                            required
                        />
                    </div>
                    <div className="card-brand-icons">
                        {cardBrand && (
                            <CardBrandIcon brand={cardBrand} size={32} />
                        )}
                    </div>
                </div>

                <div className="supported-cards">
                    <p className="supported-cards-label">We accept:</p>
                    <div className="supported-cards-icons">
                        <CardBrandIcon brand="visa" size={28} />
                        <CardBrandIcon brand="mastercard" size={28} />
                        <CardBrandIcon brand="amex" size={28} />
                        <CardBrandIcon brand="discover" size={28} />
                    </div>
                </div>
            </div>

            <button
                type="submit"
                disabled={isProcessing}
                className="payment-button"
            >
                {isProcessing ? 'Processing...' : 'Place Order'}
            </button>
        </form>
    );
};

const PaymentCard: React.FC = () => {
    const [paymentResult, setPaymentResult] = useState<PaymentConfirmationResponse | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [orderDetails, setOrderDetails] = useState<{ saleId: string; movies: Array<{ id: string; title: string; quantity: number; price: number }>; totalPrice: number } | null>(null);

    const handlePaymentSuccess = (result: PaymentConfirmationResponse) => {
        setPaymentResult(result);
        setError(null);
        
        // Get cart items for order details
        getCartInfo().then(cartInfo => {
            // Get the actual cart items
            fetch('/api/shopping_cart', {
                credentials: 'include'
            })
            .then(res => {
                console.log('fetch cart response status:', res.status);
                return res.text();
            })
            .then(text => {
                console.log('fetch cart raw response:', text);
                try {
                    const data = JSON.parse(text);
                    setOrderDetails({
                        saleId: result.transactionId || `ORDER_${Date.now()}`,
                        movies: data.cartItems || [],
                        totalPrice: cartInfo.total
                    });
                } catch (e) {
                    console.error('JSON parse error in fetch cart:', e, 'Response was:', text);
                    throw new Error(`Invalid JSON: ${text}`);
                }
            })
            .catch(err => {
                console.error('Error fetching cart items:', err);
                setOrderDetails({
                    saleId: result.transactionId || `ORDER_${Date.now()}`,
                    movies: [],
                    totalPrice: cartInfo.total
                });
            });
        })
        .catch(err => console.error('Error getting cart info:', err));
        
        // Dispatch cart updated event to clear cart
        window.dispatchEvent(new CustomEvent('cartUpdated'));
    };

    const handlePaymentError = (errorMessage: string) => {
        setError(errorMessage);
        setPaymentResult(null);
    };

    if (paymentResult) {
        return (
            <div className="payment-success">
                <h2>✓ Payment Successful!</h2>
                <p style={{ color: '#4ade80', fontSize: '1.1rem', marginBottom: '2rem' }}>{paymentResult.message}</p>
                
                {paymentResult.transactionId && (
                    <div className="order-id">
                        <p><strong>Order ID:</strong> {paymentResult.transactionId}</p>
                    </div>
                )}
                
                {orderDetails && (
                    <>
                        <div className="order-details">
                            <h3>Order Details</h3>
                            <div className="order-items">
                                {orderDetails.movies.map((movie, index) => (
                                    <div key={index} className="order-item">
                                        <p><strong>{movie.title}</strong></p>
                                        <p>Quantity: {movie.quantity} × ${movie.price.toFixed(2)}</p>
                                        <p>Subtotal: ${(movie.quantity * movie.price).toFixed(2)}</p>
                                    </div>
                                ))}
                            </div>
                        </div>
                        
                        <div className="order-total">
                            <p><strong>Total: ${orderDetails.totalPrice.toFixed(2)}</strong></p>
                        </div>
                    </>
                )}
                
                <button 
                    className="continue-shopping-btn"
                    onClick={() => {
                        setPaymentResult(null);
                        setOrderDetails(null);
                        window.location.href = '/';
                    }}
                >
                    Continue Shopping
                </button>
            </div>
        );
    }

    return (
        <div className="payment-card">
            <h2>Complete Your Purchase</h2>
            
            {error && (
                <div className="payment-error">
                    <p>Error: {error}</p>
                </div>
            )}

            <PaymentForm
                onPaymentSuccess={handlePaymentSuccess}
                onPaymentError={handlePaymentError}
            />
        </div>
    );
};

export default PaymentCard;