import {useEffect, useState} from "react";
import {fetchGenres, fetchTitleInitials} from "@/hooks/fetchers";
import type {GenreTag} from "@/types/types";

export function useGenres() {
    const [genres, setGenres] = useState<GenreTag[]>([]);
    useEffect(() => {
        fetchGenres().then(setGenres).catch(() => setGenres([]));
    }, []);
    return genres;
}

export function useInitials() {
    const [initials, setInitials] = useState<string[]>([]);
    useEffect(() => {
        fetchTitleInitials().then(setInitials).catch(() => setInitials([]));
    }, []);
    return initials;
}