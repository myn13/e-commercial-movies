import {useParams, useNavigate} from "react-router-dom";
import {useEffect, useState} from "react";
import DataCard from "@/components/datacard/DataCard";
import {fetchStar} from "@/hooks/fetchers";
import {toStarViewModel} from "@/viewmodels/adapters";
import {getSearchParams} from "@/hooks/sessionStorage";

export default function SingleStarPage() {
    const {id} = useParams();
    const navigate = useNavigate();
    const [star, setStar] = useState<any>(null);
    const [error, setError] = useState<string | null>(null);

    console.log("star id:", id);

    useEffect(() => {
        if (!id) return;

        fetchStar(id)
            .then((data) => {
                console.log("single-star raw:", data);
                const viewmodel = toStarViewModel(data, {size: "large"});
                console.log("single-star vm:", viewmodel);
                setStar(viewmodel);
                setError(null);
            })
            .catch((e: unknown) =>
                setError(e instanceof Error ? e.message : String(e))
            );
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

            {error && (
                <div style={{padding: 16, color: "crimson"}}>Error: {error}</div>
            )}
            {!error && !star && <div style={{padding: 16}}>Loading…</div>}

            {!error && star && (
                <DataCard key={`${star.id}`} {...star} />
            )}
        </main>
    );
}