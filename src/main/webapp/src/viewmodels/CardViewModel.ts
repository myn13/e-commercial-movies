export type Size = "small" | "large";

export type CardChip = {
    key: string;
    label: string | React.ReactNode;
    type?: "movie" | "star" | "genre";
    to?: string;
    href?: string;
    onClick?: React.MouseEventHandler;
    className?: string;
    style?: React.CSSProperties;
};

export type CardViewModel = {
    id: string;
    size: Size;
    className?: string;
    onClick?: React.MouseEventHandler<HTMLElement>;

    headerMain: string;
    headerYear?: number | null;

    metaLabel: string;
    metaValue: React.ReactNode;

    genres?: CardChip[];
    chips: CardChip[];
    rating?: { score: number; votes: number };
};