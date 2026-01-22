import {Link} from "react-router-dom";

type Item = { label: string; to: string };

type Props = {
    label: string;
    items: Item[];
    className?: string;
    triggerClassName?: string;
    menuClassName?: string;
    listClassName?: string;
    itemClassName?: string;
    linkClassName?: string;
};

export default function NavDropdown({
                                        label,
                                        items,
                                        className = "nav-dropdown",
                                        triggerClassName = "nav-dropdown__trigger",
                                        menuClassName = "nav-dropdown__menu",
                                        listClassName = "nav-dropdown__list",
                                        itemClassName = "nav-dropdown__item",
                                        linkClassName = "nav-dropdown__link",
                                    }: Props) {
    return (
        <li className={`nav-item ${className}`}>
            <button
                type="button"
                className={`nav-link ${triggerClassName}`}
                aria-haspopup="menu"
                aria-expanded="false"
            >
                {label}
            </button>
            
            <div className={menuClassName} role="menu">
                <ul className={listClassName}>
                    {items.map((it) => (
                        <li key={it.to} className={itemClassName} role="none">
                            <Link to={it.to} className={linkClassName} role="menuitem">
                                {it.label}
                            </Link>
                        </li>
                    ))}
                </ul>
            </div>
        </li>
    );
}
