import './Navbar.css';
import type Page from './Page'
import {Link} from "react-router-dom";
import {NavNode, UserData} from "@/types/types";
import UserLogOut from "@/components/logincard/UserLogOut";
import {useState, useEffect, useMemo} from 'react';
import {getCartInfo} from '@/hooks/fetchers';
import {useGenres, useInitials} from "@/hooks/navData";
import NavDropdown from "@/components/navbar/NavDropdown";

interface NavbarProps {
    pages?: Page[];
    isLoggedIn?: boolean;
    currentUser?: UserData | null;
    onLogout?: () => void;
}

const Navbar: React.FC<NavbarProps> = ({pages = [], isLoggedIn = false, currentUser, onLogout}) => {
    const [cartItemCount, setCartItemCount] = useState<number>(0);
    const genres = useGenres();
    const initials = useInitials();

    const genreItems = genres.map(g => ({
        label: g.name,
        to: `/movies?genreId=${g.id}`,
    }));

    const initialItems = initials.map(ch => ({
        label: ch,
        to: `/movies?initial=${encodeURIComponent(ch)}`,
    }));

    const navItems: NavNode[] = useMemo(() => {
        const flatLinks: NavNode[] = pages
            .filter(p => p.name !== "Login")
            .map(p => ({type: "link", label: p.name, to: p.href}));

        return [
            {type: "dropdown", label: "Genres", items: genreItems},
            {type: "dropdown", label: "Browse Movies", items: initialItems},
            ...flatLinks,
        ];
    }, [pages, genreItems, initialItems]);

    useEffect(() => {
        const loadCartCount = async () => {
            try {
                const cartInfo = await getCartInfo();
                setCartItemCount(cartInfo.amount);
            } catch (err) {
                console.error('Error loading cart count:', err);
                setCartItemCount(0);
            }
        };

        if (isLoggedIn) {
            loadCartCount();
        } else {
            setCartItemCount(0);
        }
        const handleCartUpdate = () => {
            if (isLoggedIn) {
                loadCartCount();
            }
        };

        window.addEventListener('cartUpdated', handleCartUpdate);

        return () => {
            window.removeEventListener('cartUpdated', handleCartUpdate);
        };
    }, [isLoggedIn]);

    useEffect(() => {
        const el = document.querySelector('.navbar') as HTMLElement | null;
        const setH = () => {
            if (el) document.documentElement.style.setProperty('--nav-height', `${el.offsetHeight}px`);
        };
        setH();
        window.addEventListener('resize', setH);
        return () => window.removeEventListener('resize', setH);
    }, []);


    return (
        <nav className="navbar">
            <div className="nav-content">
                <h2 className="logo">
                    <Link to="/" className="logo-link">
                        Fabflix
                    </Link>
                </h2>

                <ul className="nav-links">
                    {navItems.map((node) => {
                        if (node.type === "link") {
                            return (
                                <li key={`${node.label}-${node.to}`} className="nav-item">
                                    <Link to={node.to} className="nav-link">{node.label}</Link>
                                </li>
                            );
                        }

                        return (
                            <li key={`dd-${node.label}`} className="nav-item nav-dropdown">
                                <Link
                                    to="#"
                                    className="nav-link nav-dropdown__trigger"
                                    aria-haspopup="menu"
                                    aria-expanded="false"
                                    onClick={(e) => e.preventDefault()}
                                >
                                    {node.label}
                                </Link>


                                <div className="nav-dropdown__menu" role="menu" aria-label={node.label}>
                                    <ul className="nav-dropdown__list">
                                        {node.items.map(it => (
                                            <li key={it.to} className="nav-dropdown__item" role="none">
                                                <Link to={it.to} className="nav-dropdown__link" role="menuitem">
                                                    {it.label}
                                                </Link>
                                            </li>
                                        ))}
                                    </ul>
                                </div>
                            </li>
                        );
                    })}
                </ul>

                <div className="nav-right">
                    {isLoggedIn ? (
                        <div className="user-info">
                            <span className="welcome-text">
                                Welcome, {currentUser?.username}!
                            </span>
                            <UserLogOut onLogoutSuccess={onLogout}/>
                        </div>
                    ) : (
                        <Link to="/login" className="login-link">
                            Login
                        </Link>
                    )}

                    {isLoggedIn && (
                        <Link to="/shopping-cart" className="cart-button">
                            Cart
                            {cartItemCount > 0 && (
                                <span className="cart-badge">
                                    {cartItemCount > 99 ? '99+' : cartItemCount}
                                </span>
                            )}
                        </Link>
                    )}
                </div>
            </div>
        </nav>
    );
}

export default Navbar;