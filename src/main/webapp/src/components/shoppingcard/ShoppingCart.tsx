import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getCartItems, removeFromCart, updateCartQuantity } from '@/hooks/fetchers';
import type { CartResponse, CartItem } from '@/types/types';
import './ShoppingCart.css';

const ShoppingCart: React.FC = () => {
    const [cartData, setCartData] = useState<CartResponse | null>(null);
    const [removingItems, setRemovingItems] = useState<Set<string>>(new Set());
    const navigate = useNavigate();

    const loadCartItems = async () => {
        try {
            const data = await getCartItems();
            setCartData(data);
        } catch (err) {
            console.error('Error loading cart:', err);
        }
    };

    const handleQuantityChange = async (itemId: string, action: 'decrease' | 'increase' | 'delete') => {
        try {
            setRemovingItems(prev => new Set(prev).add(itemId));

            if (action === 'delete') {
                await removeFromCart(itemId);
            } else {
                await updateCartQuantity(itemId, action);
            }

            await loadCartItems(); // Reload cart after changes
        } catch (err) {
            console.error('Error updating item quantity:', err);
        } finally {
            setRemovingItems(prev => {
                const newSet = new Set(prev);
                newSet.delete(itemId);
                return newSet;
            });
        }
    };


    useEffect(() => {
        loadCartItems();

        // Listen for cart update events
        const handleCartUpdate = () => {
            loadCartItems();
        };

        window.addEventListener('cartUpdated', handleCartUpdate);

        // Cleanup event listener
        return () => {
            window.removeEventListener('cartUpdated', handleCartUpdate);
        };
    }, []);

    const cartItems = cartData?.cartItems || [];
    const totalPrice = cartData?.totalPrice || 0;

    return (
        <div className="shopping-cart">
            <div className="cart-header">
                <h2>Shopping Cart</h2>
            </div>

            <div className="cart-grid">
                {/* Left side - Movies */}
                <div className="cart-movies">
                    {cartItems.length === 0 ? (
                        <div className="empty-cart">
                            <p>Your cart is empty</p>
                            <p>Add some movies to get started!</p>
                        </div>
                    ) : (
                        <div className="movies-list">
                            {cartItems.map((item: CartItem) => (
                                <div key={item.id} className="movie-item">
                                    <div className="movie-details">
                                        <h3 className="movie-in-cart__title">{item.title}</h3>
                                        <div className="shopping-cart__price">
                                            <span>${item.price.toFixed(2)}</span>
                                        </div>
                                        <div className="shopping-cart__quantity">
                                            <span>Quantity: {item.quantity}</span>
                                        </div>
                                        <div className="shopping-cart__price">
                                            <span>Subtotal: ${(item.price * item.quantity).toFixed(2)}</span>
                                        </div>
                                    </div>
                                    <div className="quantity-controls">
                                        <button
                                            onClick={() => handleQuantityChange(item.id, 'decrease')}
                                            disabled={removingItems.has(item.id)}
                                            className="quantity-btn quantity-btn-decrease"
                                        >
                                            -1
                                        </button>
                                        <button
                                            onClick={() => handleQuantityChange(item.id, 'delete')}
                                            disabled={removingItems.has(item.id)}
                                            className="quantity-btn quantity-btn-delete"
                                        >
                                            0
                                        </button>
                                        <button
                                            onClick={() => handleQuantityChange(item.id, 'increase')}
                                            disabled={removingItems.has(item.id)}
                                            className="quantity-btn quantity-btn-increase"
                                        >
                                            +1
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                {/* Right side - Summary */}
                <div className="summary">
                    <div className="summary-content">
                        <h3>Order Summary</h3>
                        <div className="summary-item">
                            <span>Total Items:</span>
                            <span>{cartItems.reduce((sum, item) => sum + item.quantity, 0)}</span>
                        </div>
                        <div className="summary-item">
                            <span>Total Price:</span>
                            <span>${totalPrice.toFixed(2)}</span>
                        </div>
                        <div className="summary-divider"></div>
                        <div className="summary-total">
                            <span>Total:</span>
                            <span>${totalPrice.toFixed(2)}</span>
                        </div>
                        {cartItems.length > 0 && (
                            <button 
                                className="checkout-button"
                                onClick={() => navigate('/payment')}
                            >
                                Proceed to Checkout
                            </button>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ShoppingCart;
