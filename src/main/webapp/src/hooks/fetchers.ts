import {ROUTES} from "@/routes/routes";

import type {Movie, Star, LoginRequest, LoginResponse, CartResponse, CartInfo, GenreTag} from "@/types/types";

export async function fetchMovies(limit = 20, offset = 0): Promise<Movie[]> {
    const res = await fetch(ROUTES.MOVIES_PAGE(limit, offset), {
        credentials: 'include'
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
}

export async function fetchMovie(id: string): Promise<Movie> {
    const res = await fetch(ROUTES.MOVIE_DETAIL_API(id), {
        credentials: 'include'
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
}

export async function fetchStar(id: string): Promise<Star> {
    const res = await fetch(ROUTES.STAR_DETAIL_API(id), {
        credentials: 'include'
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
}

export async function loginUser(credentials: LoginRequest): Promise<LoginResponse> {
    const params = new URLSearchParams(credentials as Record<string, string>);

    console.log("Attempting login", params);
    const res = await fetch(ROUTES.LOGIN_API, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: params.toString(),
        credentials: 'include'
    });


    console.log("Res", res);
    if (!res.ok) {
        // Throw an error with the HTTP status and, if available, the server's text response
        const errorText = await res.text();
        throw new Error(`Login failed (HTTP ${res.status}): ${errorText || 'No response body'}`);
    }
    return res.json();
}

export async function addToCart(movie: { id: string; title: string; quantity?: number }): Promise<CartResponse> {
    const movieData = {
        id: movie.id,
        title: movie.title,
        quantity: movie.quantity || 1
    };

    const res = await fetch(ROUTES.SHOPPING_CART_API, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(movieData),
        credentials: 'include'
    });

    if (!res.ok) {
        const errorText = await res.text();
        throw new Error(`Add to cart failed (HTTP ${res.status}): ${errorText || 'No response body'}`);
    }

    const result = await res.json();

    // Dispatch custom event to notify all components of cart update
    window.dispatchEvent(new CustomEvent('cartUpdated'));

    return result;
}

export async function removeFromCart(movieId: string): Promise<CartResponse> {
    const res = await fetch(`${ROUTES.SHOPPING_CART_API}?movieId=${encodeURIComponent(movieId)}`, {
        method: 'DELETE',
        credentials: 'include'
    });

    if (!res.ok) {
        const errorText = await res.text();
        throw new Error(`Remove from cart failed (HTTP ${res.status}): ${errorText || 'No response body'}`);
    }

    const result = await res.json();

    // Dispatch custom event to notify all components of cart update
    window.dispatchEvent(new CustomEvent('cartUpdated'));

    return result;
}

export async function updateCartQuantity(movieId: string, action: 'increase' | 'decrease'): Promise<CartResponse> {
    const res = await fetch(ROUTES.SHOPPING_CART_API, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            movieId: movieId,
            action: action
        }),
        credentials: 'include'
    });

    if (!res.ok) {
        const errorText = await res.text();
        throw new Error(`Update cart quantity failed (HTTP ${res.status}): ${errorText || 'No response body'}`);
    }

    const result = await res.json();

    // Dispatch custom event to notify all components of cart update
    window.dispatchEvent(new CustomEvent('cartUpdated'));

    return result;
}


export async function getCartItems(): Promise<CartResponse> {
    const res = await fetch(ROUTES.SHOPPING_CART_API, {
        method: 'GET',
        credentials: 'include'
    });

    const responseText = await res.text();
    console.log('getCartItems raw response:', responseText);

    if (!res.ok) {
        throw new Error(`Get cart items failed (HTTP ${res.status}): ${responseText || 'No response body'}`);
    }

    try {
        return JSON.parse(responseText);
    } catch (e) {
        console.error('getCartItems JSON parse error:', e);
        throw new Error(`Invalid JSON response: ${responseText}`);
    }
}

export async function getCartInfo(): Promise<CartInfo> {
    const cartResponse = await getCartItems();
    const totalQuantity = cartResponse.cartItems.reduce((sum, item) => sum + item.quantity, 0);

    return {
        amount: totalQuantity,
        total: cartResponse.totalPrice
    };
}

// Payment functions
export async function processPayment(paymentData: { customerId: number; movieId: string; saleDate: string }): Promise<{
    status: string;
    message: string
}> {
    console.log('processPayment called with:', paymentData);

    const res = await fetch(ROUTES.PAYMENT_API, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify(paymentData)
    });

    console.log('processPayment response status:', res.status, res.statusText);
    console.log('processPayment response headers:', Object.fromEntries(res.headers.entries()));

    const responseText = await res.text();
    console.log('processPayment raw response:', responseText);

    if (!res.ok) {
        try {
            const errorData = JSON.parse(responseText);
            throw new Error(errorData.message || `Payment failed (HTTP ${res.status})`);
        } catch (e) {
            throw new Error(`Payment failed (HTTP ${res.status}): ${responseText || 'No response body'}`);
        }
    }

    try {
        return JSON.parse(responseText);
    } catch (e) {
        console.error('JSON parse error:', e);
        throw new Error(`Invalid JSON response: ${responseText}`);
    }

    return res.json();
}

export async function fetchGenres(): Promise<GenreTag[]> {
    const res = await fetch(ROUTES.GENRES, {
        credentials: 'include'
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
}

export async function fetchTitleInitials(): Promise<string[]> {
    const res = await fetch(ROUTES.TITLE_INITIALS, {
        credentials: "include",
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
}

export async function fetchSearchOrTop(params: URLSearchParams): Promise<Movie[]> {
    const res = await fetch(ROUTES.SEARCH_API(params.toString()), {credentials: 'include'});
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
}

/**
 * Fetch movie poster URL from OMDB API
 * @param movieId - The movie ID (e.g., "tt0388951")
 * @param title - Optional movie title for fallback search
 * @param year - Optional movie year for better matching
 * @returns Poster URL or null if not found
 */
export async function fetchMoviePoster(movieId: string, title?: string, year?: number): Promise<string | null> {
    const apiKey = import.meta.env.VITE_OMDB_API_KEY;

    if (!apiKey) {
        console.warn('[OMDB] API key not configured. Set VITE_OMDB_API_KEY in .env file.');
        return null;
    }

    try {
        // Use HTTPS for OMDB API
        const isImdbId = movieId.startsWith('tt');
        const url = isImdbId
            ? `https://www.omdbapi.com/?apikey=${apiKey}&i=${movieId}&plot=short`
            : `https://www.omdbapi.com/?apikey=${apiKey}&t=${encodeURIComponent(movieId)}&y=${year || ''}&plot=short`;

        console.log(`[OMDB] Fetching poster for: ${movieId || title}`);

        const res = await fetch(url);
        if (!res.ok) {
            console.error(`[OMDB] HTTP error ${res.status}: ${res.statusText}`);
            return null;
        }

        const data = await res.json();

        if (data.Response === 'False') {
            console.warn(`[OMDB] Movie not found: ${data.Error || 'Unknown error'}`);
            return null;
        }

        if (data.Response === 'True' && data.Poster && data.Poster !== 'N/A') {
            console.log(`[OMDB] Found poster for ${data.Title}`);
            return data.Poster;
        }

        console.warn(`[OMDB] No poster available for ${data.Title || 'unknown'}`);
        return null;
    } catch (error) {
        console.error('[OMDB] Error fetching poster:', error);
        return null;
    }
}


