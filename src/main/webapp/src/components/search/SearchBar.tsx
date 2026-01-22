import {useEffect, useState} from "react";
import {useNavigate, createSearchParams} from "react-router-dom";
import {fetchGenres, fetchTitleInitials} from "@/hooks/fetchers";
import type {GenreTag} from "@/types/types";
import styles from "./SearchBar.module.css";

export default function MovieSearchBar() {
    const navigate = useNavigate();

    // form state
    const [title, setTitle] = useState("");
    const [year, setYear] = useState("");
    const [genreId, setGenreId] = useState("");   // ← now using ID
    const [director, setDirector] = useState("");
    const [star, setStar] = useState("");

    const [genres, setGenres] = useState<GenreTag[]>([]);

    useEffect(() => {
        fetchGenres().then(setGenres).catch(() => setGenres([]));
    }, []);


    function buildQuery(params: Record<string, string | number | null | undefined>) {
        const pairs = Object.entries(params).filter(
            ([, v]) => v !== null && v !== undefined && String(v).trim() !== ""
        );
        return createSearchParams(pairs as [string, string][]).toString();
    }

    function hasAnyFilter(p: {
        title?: string;
        year?: string;
        director?: string;
        star?: string;
        genreId?: string;
    }) {
        return Object.values(p).some(
            v => !!(
                v && String(v).trim()));
    }

    function onSubmit(e: React.FormEvent) {
        e.preventDefault();

        const filters = {
            title,
            year,
            director,
            star,
            genreId,
        };

        if (!hasAnyFilter(filters)) {
            navigate({pathname: "/movies"});
            return;
        }

        const qs = buildQuery({
            title: title || undefined,
            year: year || undefined,
            director: director || undefined,
            star: star || undefined,
            genreId: genreId || undefined,   // ← send ID, not name
            limit: "20",
            offset: "0",
        });

        navigate({pathname: "/movies", search: `?${qs}`});
    }

    function onReset() {
        setTitle("");
        setYear("");
        setGenreId("");
        setDirector("");
        setStar("");
        navigate({pathname: "/movies"});
    }


    return (
        <form className={styles["search-bar"]} onSubmit={onSubmit}>
            <div className={styles["search-fields"]}>
                <div className={styles["search-field"]}>
                    <label htmlFor="title" className={styles["search-label"]}>Title</label>
                    <input
                        id="title"
                        className="search-input"
                        value={title}
                        onChange={e => setTitle(e.target.value)}
                    />
                </div>

                <div className={styles["search-field"]}>
                    <label htmlFor="year" className={styles["search-label"]}>Year</label>
                    <input
                        id="year"
                        className="search-input"
                        value={year}
                        onChange={e => setYear(e.target.value.replace(/[^\d]/g, ""))}
                    />
                </div>

                <div className={styles["search-field"]}>
                    <label htmlFor="director" className={styles["search-label"]}>Director</label>
                    <input
                        id="director"
                        className="search-input"
                        value={director}
                        onChange={e => setDirector(e.target.value)}
                    />
                </div>

                <div className={styles["search-field"]}>
                    <label htmlFor="star" className={styles["search-label"]}>Star</label>
                    <input
                        id="star"
                        className="search-input"
                        value={star}
                        onChange={e => setStar(e.target.value)}
                    />
                </div>

                <div className={styles["search-field"]}>
                    <label htmlFor="genreId" className={styles["search-label"]}>Genre</label>
                    <select
                        id="genreId"
                        className="search-select"
                        value={genreId}
                        onChange={e => setGenreId(e.target.value)}
                    >
                        <option value="">Any</option>
                        {genres.map(g => (
                            <option key={g.id} value={String(g.id)}>{g.name}</option>
                        ))}
                    </select>
                </div>
            </div>

            <div className={styles["search-actions"]}>
                <button type="submit" className="search-btn search-btn--primary">Search</button>
                <button type="button" className="search-btn search-btn--reset" onClick={onReset}>
                    Reset
                </button>
            </div>
        </form>
    );
}
