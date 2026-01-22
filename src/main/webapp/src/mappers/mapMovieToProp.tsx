import type {Movie, Star} from "@/types/types";

export type MovieProps = {
    id: string;
    title: string;
    year: number | null;
    director: string | null;
    rating: number | null;
    vote_count: number | null;
    stars: Star[];
    genres: string[];
    onClick?: () => void;
};

export function mapMovieToProp(m: Movie): MovieProps {
    return {
        id: m.id,
        title: m.title,
        year: m.year ?? null,
        director: m.director ?? null,
        rating: m.rating ?? null,
        vote_count: m.vote_count ?? null,
        stars: m.stars ?? [],
        genres: (m.genres ?? []).map(g => g.name),
    };
}