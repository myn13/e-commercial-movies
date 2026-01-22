export default interface Page {
    name: string;
    href: string;
    children?: {
        label: string;
        to: string;
    }[];
};
