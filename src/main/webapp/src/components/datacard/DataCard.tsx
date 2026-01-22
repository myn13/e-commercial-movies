import styles from "./DataCard.module.css";
import type {CardViewModel} from "@/viewmodels/CardViewModel";
import {Link, useSearchParams} from "react-router-dom";
import useToken from '../logincard/useToken';
import {addToCart, fetchMoviePoster} from '@/hooks/fetchers';
import {saveSearchParams} from '@/hooks/sessionStorage';
import React, {useState, useEffect} from "react";

const DataCard: React.FC<CardViewModel> = (viewmodel) => {
    const {token} = useToken();
    const [isAddingToCart, setIsAddingToCart] = useState(false);
    const [cartMessage, setCartMessage] = useState<string | null>(null);
    const [posterUrl, setPosterUrl] = useState<string | null>(null);
    const [searchParams] = useSearchParams();

    const isClickable = typeof viewmodel.onClick === "function";
    const isLoggedIn = !!token;

    // Fetch movie poster on mount
    useEffect(() => {
        const loadPoster = async () => {
            const url = await fetchMoviePoster(
                viewmodel.id,
                viewmodel.headerMain,
                viewmodel.headerYear ?? undefined
            );
            setPosterUrl(url);
        };
        loadPoster();
    }, [viewmodel.id, viewmodel.headerMain, viewmodel.headerYear]);

    const handleAddToCart = async (e: React.MouseEvent) => {
        e.stopPropagation(); // Prevent card click
        if (isAddingToCart) return;

        setIsAddingToCart(true);
        setCartMessage(null);

        try {
            await addToCart({
                id: viewmodel.id,
                title: viewmodel.headerMain
            });
            setCartMessage("Added to cart!");
            setTimeout(() => setCartMessage(null), 2000);
        } catch (error) {
            console.error('Error adding to cart:', error);
            setCartMessage("Failed to add to cart");
            setTimeout(() => setCartMessage(null), 2000);
        } finally {
            setIsAddingToCart(false);
        }
    };

    const handleStarClick = (e: React.MouseEvent) => {
        if (searchParams.toString()) {
            saveSearchParams(searchParams);
        }
    };

    const rootClass = [
        styles["movie-card"],
        styles[`movie-card--${viewmodel.size}`],
        isClickable ? styles["is-clickable"] : "",
        viewmodel.className,
    ].filter(Boolean).join(" ");

    return (
        <article
            className={rootClass}
            onClick={viewmodel.onClick}
            role={isClickable ? "button" : undefined}
            tabIndex={isClickable ? 0 : undefined}
        >
            <div className={styles["movie-card__content"]}>
                {/* Movie Poster - Left Side */}
                {posterUrl && (
                    <div className={styles["movie-card__poster"]}>
                        <img
                            src={posterUrl}
                            alt={`${viewmodel.headerMain} poster`}
                            loading="lazy"
                            crossOrigin="anonymous"
                            onError={(e) => {
                                console.warn('Failed to load poster image:', posterUrl);
                                (e.target as HTMLImageElement).style.display = 'none';
                            }}
                        />
                    </div>
                )}

                <div className={styles["movie-card__info"]}>
                    <header className={styles["movie-card__header"] ?? ""}>
                        <h5 className={styles["movie-card__title"]}>
                            {viewmodel.headerMain}
                            {viewmodel.headerYear !== undefined && (
                                <span className={styles["movie-card__year"]}> ({viewmodel.headerYear})</span>)
                            }
                        </h5>
                    </header>
                    {viewmodel.rating && (
                        <span className={styles["movie-card__rating"]} title="Rating">
                        &#11088;{viewmodel.rating.score.toFixed(1)}
                            <span className={styles["movie-card__vote_count"]}> ({viewmodel.rating.votes})</span>
                        </span>
                    )}

                    <div className={styles["movie-card__meta"]}>
                        <span className={styles["movie-card__director"]}>
                        <span style={{fontWeight: "bold"}}>{viewmodel.metaLabel}</span> {viewmodel.metaValue}
                        </span>

                        {viewmodel.genres?.length ? (
                            <div className={styles["movie-card__genres"]}>
                                {viewmodel.genres.map((chip) => (
                                    <Link
                                        key={chip.key}
                                        to={chip.to!}
                                        className={[styles["movie-card__chip"], styles["movie-card__chip--genre"]].join(" ")}
                                        onClick={(e) => e.stopPropagation()}
                                    >
                                        {chip.label}
                                    </Link>
                                ))}
                            </div>
                        ) : null}

                        <div className={styles["movie-card__actors"]}>
                            {viewmodel.chips.map((chip) => (
                                <Link
                                    key={chip.key}
                                    to={chip.to!}
                                    className={styles["movie-card__chip"]}
                                    onClick={(e) => {
                                        e.stopPropagation();
                                        handleStarClick(e);
                                    }}
                                >
                                    {chip.label}
                                </Link>
                            ))}
                        </div>

                        {/* Add to Cart Button - only show when logged in */}
                        {isLoggedIn && (
                            <div className={styles["movie-card__cart-section"]}>
                                <button
                                    className={styles["add-to-cart-button"]}
                                    onClick={handleAddToCart}
                                    disabled={isAddingToCart}
                                >
                                    {isAddingToCart ? "Adding..." : "Add to Cart"}
                                </button>
                                {cartMessage && (
                                    <div className={styles["cart-message"]}>
                                        {cartMessage}
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </article>
    );
};

export default DataCard;
