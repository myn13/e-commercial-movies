import type {Movie} from '@/types/types.ts'
import {useEffect, useState, useRef} from "react"
import {useNavigate, useSearchParams} from "react-router-dom"
import DataCard from "@/components/datacard/DataCard"
import {fetchSearchOrTop} from "@/hooks/fetchers";
import {toMovieViewModel} from "@/viewmodels/adapters";
import {CardViewModel} from "@/viewmodels/CardViewModel";
import SearchBar from "@/components/search/SearchBar";
import {saveSearchParams, getSearchParams} from "@/hooks/sessionStorage";
import './MoviesPage.css';


const DEFAULT_LIMIT = 25;
const ALLOWED_LIMITS = [10, 25, 50, 100] as const;

const DEFAULT_ORDER = {
    order1: 'rating',
    dir1: 'desc',
    order2: 'title',
    dir2: 'asc',
} as const;

export default function MoviesPage() {
    const [cards, setCards] = useState<CardViewModel[]>([]);
    const [error, setError] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const navigate = useNavigate();
    const [searchParams, setSearchParams] = useSearchParams();
    const isInitialMount = useRef(true);

    useEffect(() => {
        if (!isInitialMount.current) return;

        const saved = getSearchParams();
        if (saved.toString()) {
            // honor previously saved state
            setSearchParams(saved);
            isInitialMount.current = false;
            return;
        }

        const next = new URLSearchParams(searchParams);
        if (!next.get('limit')) next.set('limit', String(DEFAULT_LIMIT));
        if (!next.get('offset')) next.set('offset', '0');

        // sorting defaults (only if missing)
        if (!next.get('order1')) next.set('order1', DEFAULT_ORDER.order1);
        if (!next.get('dir1')) next.set('dir1', DEFAULT_ORDER.dir1);
        if (!next.get('order2')) next.set('order2', DEFAULT_ORDER.order2);
        if (!next.get('dir2')) next.set('dir2', DEFAULT_ORDER.dir2);

        if (next.toString() !== searchParams.toString()) {
            setSearchParams(next);
        }
        isInitialMount.current = false;
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const limitRaw = parseInt(searchParams.get('limit') || String(DEFAULT_LIMIT), 10);
    const moviesPerPage = Number.isFinite(limitRaw) ? limitRaw : DEFAULT_LIMIT;
    const currentOffset = parseInt(searchParams.get('offset') || '0', 10) || 0;
    const currentPage = Math.floor(currentOffset / moviesPerPage) + 1;

    const order1 = (searchParams.get('order1') ?? DEFAULT_ORDER.order1);
    const dir1 = (searchParams.get('dir1') ?? DEFAULT_ORDER.dir1);
    const order2 = (searchParams.get('order2') ?? DEFAULT_ORDER.order2);
    const dir2 = (searchParams.get('dir2') ?? DEFAULT_ORDER.dir2);

    useEffect(() => {
        if (!isInitialMount.current) {
            saveSearchParams(searchParams);
        }
    }, [searchParams]);

    function setParamAndResetOffset(key: string, value: string) {
        setSearchParams(prev => {
            const next = new URLSearchParams(prev);
            next.set(key, value);
            next.set('offset', '0');
            return next;
        });
    }

    const handleLimitChange = (newLimit: number) => {
        const safe = (ALLOWED_LIMITS as readonly number[]).includes(newLimit) ? newLimit : DEFAULT_LIMIT;
        setSearchParams(prev => {
            const next = new URLSearchParams(prev);
            next.set('limit', String(safe));
            next.set('offset', '0');
            return next;
        });
    };

    const handleNextPage = () => {
        const newOffset = currentOffset + moviesPerPage;
        setSearchParams(prev => {
            const next = new URLSearchParams(prev);
            next.set('offset', String(newOffset));
            return next;
        });
    };

    const handlePrevPage = () => {
        const newOffset = Math.max(0, currentOffset - moviesPerPage);
        setSearchParams(prev => {
            const next = new URLSearchParams(prev);
            next.set('offset', String(newOffset));
            return next;
        });
    };

    // ----- fetch whenever URL params change -----
    useEffect(() => {
        setIsLoading(true);
        fetchSearchOrTop(searchParams)
            .then((data: Movie[]) => {
                const mapped = data.map(m => ({
                    ...toMovieViewModel(m, {size: "small"}),
                    onClick: () => {
                        saveSearchParams(searchParams);
                        navigate(`/movies/${m.id}`);
                    },
                }));
                setCards(mapped);
                setError(null);
                setIsLoading(false);
            })
            .catch(e => {
                setError(e.message);
                setIsLoading(false);
            });
    }, [navigate, searchParams]);

    return (
        <>
            <SearchBar/>

            <div className="pagination-controls">
                <div className="pagination-limit">
                    <label htmlFor="movies-per-page">Movies per page:</label>
                    <select
                        id="movies-per-page"
                        value={moviesPerPage}
                        onChange={(e) => handleLimitChange(parseInt(e.target.value, 10))}
                    >
                        <option value="10">10</option>
                        <option value="25">25</option>
                        <option value="50">50</option>
                        <option value="100">100</option>
                    </select>
                </div>

                <div className="pagination-sorting">
                    <label className="sort-label">Sort:</label>

                    <select
                        className="sort-select"
                        value={order1}
                        onChange={(e) => setParamAndResetOffset('order1', e.target.value)}
                        aria-label="Primary sort field"
                    >
                        <option value="rating">Rating</option>
                        <option value="title">Title</option>
                    </select>

                    <select
                        className="sort-select"
                        value={dir1}
                        onChange={(e) => setParamAndResetOffset('dir1', e.target.value)}
                        aria-label="Primary sort direction"
                    >
                        <option value="desc">Desc</option>
                        <option value="asc">Asc</option>
                    </select>

                    <span className="sort-then">then</span>

                    <select
                        className="sort-select"
                        value={order2}
                        onChange={(e) => setParamAndResetOffset('order2', e.target.value)}
                        aria-label="Secondary sort field"
                    >
                        <option value="rating">Rating</option>
                        <option value="title">Title</option>
                    </select>

                    <select
                        className="sort-select"
                        value={dir2}
                        onChange={(e) => setParamAndResetOffset('dir2', e.target.value)}
                        aria-label="Secondary sort direction"
                    >
                        <option value="asc">Asc</option>
                        <option value="desc">Desc</option>
                    </select>
                </div>

                <div className="pagination-buttons">
                    <button onClick={handlePrevPage} disabled={currentOffset === 0 || isLoading}>
                        Previous
                    </button>
                    <span className="page-info">Page {currentPage}</span>
                    <button
                        onClick={handleNextPage}
                        disabled={cards.length < moviesPerPage || isLoading}
                    >
                        Next
                    </button>
                </div>
            </div>

            {isLoading && <div className="loading">Loading movies...</div>}
            {error && <div className="error">Error: {error}</div>}

            {!isLoading && !error && cards.map(movie => (
                <DataCard key={`${movie.id}`} {...movie} />
            ))}

            {!isLoading && !error && cards.length === 0 && (
                <div className="no-movies">No movies found</div>
            )}
        </>
    );
}
