package search;

public record SearchCriteria(
        String title,
        String director,
        String star,
        Integer year,
        String initial,
        Integer genreId,
        String genre
) {

    public boolean hasTitle() {
        return title != null && !title.isBlank();
    }

    public String effectiveTitle() {
        return hasInitial() ? null : title;
    }

    public boolean hasYear() {
        return year != null && year > 0;
    }

    public boolean hasDirector() {
        return director != null && !director.isBlank();
    }

    public boolean hasStar() {
        return star != null && !star.isBlank();
    }

    public boolean hasAny() {
        return hasTitle()
                || hasGenre()
                || hasDirector()
                || hasStar()
                || hasYear()
                || hasInitial()
                || hasGenreId();
    }

    public boolean hasInitial() {
        return initial != null && !initial.isBlank();
    }

    public boolean hasGenreId() {
        return genreId != null;
    }

    public boolean hasGenre() {
        return genre != null && !genre.isBlank();
    }
}
