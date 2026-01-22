package search;

public final class SqlTemplates {
    private SqlTemplates() {
    }

    private static final String AGG_FOR_PAGE = """
            , stars_page AS (
                SELECT sim.movie_id,
                       JSON_ARRAYAGG(JSON_OBJECT('id', s.id, 'name', s.name)) AS all_stars
                FROM stars_in_movies sim
                JOIN stars s ON s.id = sim.star_id
                WHERE sim.movie_id IN (SELECT id FROM page_ids)
                GROUP BY sim.movie_id
              )
            , genres_page AS (
                SELECT gim.movie_id,
                       JSON_ARRAYAGG(JSON_OBJECT('id', g.id, 'name', g.name)) AS all_genres
                FROM genres_in_movies gim
                JOIN genres g ON g.id = gim.genre_id
                WHERE gim.movie_id IN (SELECT id FROM page_ids)
                GROUP BY gim.movie_id
              )
            """;

    // final select orders by p.seq so the visible order matches the page slice
    private static final String FINAL_SELECT_NO_ORDER = """
            SELECT
              m.id,
              m.title        AS title_name,
              m.year,
              m.director,
              r.rating,
              r.vote_count,
              COALESCE(JSON_EXTRACT(sp.all_stars,  '$[0 to 2]'), JSON_ARRAY()) AS stars,
              COALESCE(JSON_EXTRACT(gp.all_genres, '$[0 to 2]'), JSON_ARRAY()) AS genres
            FROM movies m
            JOIN ratings r  ON r.movie_id = m.id
            JOIN page_ids p ON p.id       = m.id
            LEFT JOIN stars_page  sp ON sp.movie_id = m.id
            LEFT JOIN genres_page gp ON gp.movie_id = m.id
            ORDER BY p.seq
            """;

    // Search plan (multi-filter) with injected order key for page_ids
    private static final String MULTI_FILTER_PAGED_FMT = """
            WITH
              filtered AS (
                SELECT DISTINCT m.id
                FROM movies m
                LEFT JOIN stars_in_movies sim ON sim.movie_id = m.id
                LEFT JOIN stars s            ON s.id = sim.star_id
                LEFT JOIN genres_in_movies gim ON gim.movie_id = m.id
                LEFT JOIN genres g             ON g.id = gim.genre_id
                WHERE
                  (? IS NULL OR m.title LIKE ?) AND
                  (? IS NULL OR m.year = ?) AND
                  (? IS NULL OR m.director LIKE ?) AND
                  (? IS NULL OR s.name LIKE ?) AND
                  (? IS NULL OR UPPER(SUBSTRING(m.title,1,1)) = UPPER(?)) AND
                  (? IS NULL OR SUBSTRING(m.title,1,1) BETWEEN '0' AND '9') AND
                  (? IS NULL OR g.id = ?) AND
                  (? IS NULL OR g.name = ?)
              ),
              page_ids AS (
                SELECT
                  f.id,
                  ROW_NUMBER() OVER (ORDER BY %s) AS seq
                FROM filtered f
                JOIN ratings r ON r.movie_id = f.id
                JOIN movies  m ON m.id       = f.id
                ORDER BY %s
                LIMIT ? OFFSET ?
              )
            """ + AGG_FOR_PAGE + FINAL_SELECT_NO_ORDER;

    /**
     * Build final SQL for multi-filter paging with given order key
     */
    public static String multiFilterPagedSql(String orderKey) {
        return String.format(MULTI_FILTER_PAGED_FMT, orderKey, orderKey);
    }

    // Top/All movies (MovieServlet) with CTE-based paging, reuse same pattern
    private static final String TOP_PAGED_FMT = """
            WITH page_ids AS (
              SELECT
                m.id,
                ROW_NUMBER() OVER (ORDER BY %s) AS seq
              FROM movies m
              JOIN ratings r ON r.movie_id = m.id
              ORDER BY %s
              LIMIT ? OFFSET ?
            )
            """ + AGG_FOR_PAGE + FINAL_SELECT_NO_ORDER;

    public static String topPagedSql(String orderKey) {
        return String.format(TOP_PAGED_FMT, orderKey, orderKey);
    }
}
