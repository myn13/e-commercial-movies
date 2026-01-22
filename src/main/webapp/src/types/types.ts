export type Star = {
    id: string;
    name: string;
    birth_year?: number | null;
    movies?: Movie[];
    total_movies?: number
};

export type GenreTag = {
    id: string;
    name: string
};

export type Movie = {
    vote_count?: number | null;
    id: string;
    title: string;
    year?: number | null;
    director?: string | null;
    rating?: number | null;
    stars?: Star[];
    genres?: GenreTag[];
    posterUrl?: string; // OMDB poster URL
};

export type LoginRequest = {
    username: string;
    password: string;
}

export type LoginResponse = {
    status: 'success' | 'fail';
    token?: string;
}

export type UserData = {
    username: string;
    loginTime: string;
}

export type TokenState = {
    token: string | null;
    currentUser: UserData | null;
    setToken: (data: { token: string }) => void;
    logout: () => void;
}

export type CartItem = {
    id: string;
    title: string;
    price: number;
    quantity: number;
}

export type CartResponse = {
    sessionId: string;
    lastAccessTime: string;
    cartItems: CartItem[];
    totalPrice: number;
    message?: string;
    error?: string;
}

export type CartInfo = {
    amount: number;
    total: number;
}

export type PaymentConfirmationResponse = {
    status: string;
    message: string;
    transactionId?: string;
}

export type NavLinkItem = { type: "link"; label: string; to: string };
export type NavDropdownItem = { type: "dropdown"; label: string; items: { label: string; to: string }[] };
export type NavNode = NavLinkItem | NavDropdownItem;

export interface MovieListState {
    searchParams: string;
    limit: number;
    offset: number;
    timestamp: number;
}