import type {CardViewModel, Size} from "./CardViewModel";
import type {GenreTag, Movie, Star} from "@/types/types";

const normalizeString = (s?: string | null) =>
    s && s.trim() !== "" ? s : null;

function genresToChips(genres: GenreTag[] = []): CardViewModel["chips"] {
    return genres.map(g => ({
        key: `g-${g.id}`,
        label: g.name,
        to: `/movies?genreId=${encodeURIComponent(g.id)}`,
        className: "chip chip--genre",
        type: "genre",
    }));
}

function starsToChips(stars: Star[] = []): CardViewModel["chips"] {
    return stars.map(s => ({
        key: `s-${s.id}`,
        label: `${s.name}${typeof s.total_movies === "number" ? ` (${s.total_movies})` : ""}`,
        to: `/stars/${encodeURIComponent(s.id)}`,
        className: "chip chip--star",
        type: "star",
    }));
}

export function toMovieViewModel(
    movie: Movie,
    opts?: { size?: Size; className?: string; onClick?: React.MouseEventHandler<HTMLElement> }
): CardViewModel {
    const size = opts?.size ?? "small";
    return {
        id: movie.id,
        size,
        className: opts?.className,
        onClick: opts?.onClick,

        headerMain: movie.title,
        headerYear: movie.year,

        metaLabel: "Director:",
        metaValue: normalizeString((movie as any).director) ?? "—",
        genres: genresToChips(movie.genres),
        chips: starsToChips(movie.stars),

        rating:
            typeof (movie as any).rating === "number" &&
            typeof (movie as any).vote_count === "number"
                ? {score: (movie as any).rating, votes: (movie as any).vote_count}
                : undefined,
    };
}

export function toStarViewModel(
    star: Star,
    opts?: { size?: Size; className?: string; onClick?: React.MouseEventHandler<HTMLElement> }
): CardViewModel {
    const size = opts?.size ?? "small";
    const movies = star.movies ?? [];

    return {
        id: star.id,
        size,
        className: opts?.className,
        onClick: opts?.onClick,

        headerMain: star.name,

        metaLabel: "Birth year:",
        metaValue: (star as any).birth_year ?? "—",

        chips: movies.map(m => ({
            key: `m-${m.id}`,
            label: `${m.title}${m.year ? ` (${m.year})` : ""}`,
            to: `/movies/${encodeURIComponent(m.id)}`,
            className: "chip chip--movie",
            type: "movie",
        })),
    };
}
