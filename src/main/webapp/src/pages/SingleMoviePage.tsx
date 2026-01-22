import {useParams, useNavigate} from "react-router-dom"
import {useEffect, useState} from "react"
import DataCard from "@/components/datacard/DataCard";
import {fetchMovie} from "@/hooks/fetchers";
import {toMovieViewModel} from "@/viewmodels/adapters";
import {getSearchParams} from "@/hooks/sessionStorage";

export default function SingleMoviePage() {
    const {id} = useParams();
    const navigate = useNavigate();
    const [movie, setMovie] = useState<any>(null);
    const [error, setError] = useState<string | null>(null);

    console.log(id);

    useEffect(() => {
        if (!id) return;

        fetchMovie(id)
            .then((data) => {
                console.log("single-movie raw:", data);
                const props = toMovieViewModel(data, {size: "large"});
                console.log("single-movie props:", props);
                setMovie(props);
                setError(null);
            })
            .catch((e) => setError(e.message));
    }, [id]);

    const handleBack = () => {
        const savedParams = getSearchParams();
        const paramsString = savedParams.toString();
        navigate(paramsString ? `/movies?${paramsString}` : '/movies');
    };

    return (
        <main style={{overflow: "hidden"}}>
            <div>
                <button onClick={handleBack} className="back-link" style={{background: 'none', border: 'none', cursor: 'pointer', fontSize: 'inherit', color: 'inherit'}}>
                    ← Back to Movies
                </button>
            </div>

            {error && <div style={{padding: 16, color: "crimson"}}>Error: {error}</div>}
            {!error && !movie && <div style={{padding: 16}}>Loading…</div>}

            {!error && movie && (
                <DataCard
                    key={`${movie.id}`} {...movie}
                />
            )}
        </main>
    );

}
