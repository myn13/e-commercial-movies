import type { MovieListState } from '@/types/types';

const STATE_KEY = 'fabflix_movie_list_state';

export function saveMovieListState(state: MovieListState): void {
    try {
        sessionStorage.setItem(STATE_KEY, JSON.stringify(state));
    } catch (error) {
        console.error('Failed to save movie list state:', error);
    }
}

export function getMovieListState(): MovieListState | null {
    try {
        const stored = sessionStorage.getItem(STATE_KEY);
        if (!stored) return null;
        
        const state = JSON.parse(stored) as MovieListState;
    
        const maxAge = 3600000; // 1 hour in milliseconds
        const age = Date.now() - state.timestamp;
        
        if (age > maxAge) {
            sessionStorage.removeItem(STATE_KEY);
            return null;
        }
        
        return state;
    } catch (error) {
        console.error('Failed to restore movie list state:', error);
        return null;
    }
}

export function clearMovieListState(): void {
    try {
        sessionStorage.removeItem(STATE_KEY);
    } catch (error) {
        console.error('Failed to clear movie list state:', error);
    }
}

export function saveSearchParams(searchParams: URLSearchParams): void {
    const state: MovieListState = {
        searchParams: searchParams.toString(),
        limit: parseInt(searchParams.get('limit') || '20', 10),
        offset: parseInt(searchParams.get('offset') || '0', 10),
        timestamp: Date.now()
    };
    saveMovieListState(state);
}


export function getSearchParams(): URLSearchParams {
    const state = getMovieListState();
    if (!state) {
        return new URLSearchParams();
    }
    return new URLSearchParams(state.searchParams);
}

