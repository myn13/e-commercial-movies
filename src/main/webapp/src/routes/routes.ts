export const ROUTES = {
    MOVIES: "/movies",
    MOVIE_DETAIL: (id: string) => `/movies/${id}`,
    STARS: "/stars",
    STAR_DETAIL: (id: string) => `/stars/${id}`,
    SHOPPING_CART: "/shopping-cart",

    MOVIES_PAGE: (limit = 20, offset = 0) =>
        `${import.meta.env.VITE_API_URL}/movies_page?limit=${limit}&offset=${offset}`,
    MOVIE_DETAIL_API: (id: string) =>
        `${import.meta.env.VITE_API_URL}/movies/${encodeURIComponent(id)}`,
    STAR_DETAIL_API: (id: string) =>
        `${import.meta.env.VITE_API_URL}/stars/${encodeURIComponent(id)}`,
    LOGIN_API: `${import.meta.env.VITE_API_URL}/login`,
    GENRES: `${import.meta.env.VITE_API_URL}/genres`,
    TITLE_INITIALS: `${import.meta.env.VITE_API_URL}/browse/title-initials`,
    SEARCH_API: (parameters: string) =>
        `${import.meta.env.VITE_API_URL}/search?${parameters}`,
    SHOPPING_CART_API: `${import.meta.env.VITE_API_URL}/shopping_cart`,
    PAYMENT_API: `${import.meta.env.VITE_API_URL}/payment`
};

