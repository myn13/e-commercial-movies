package search;

import java.sql.*;

import static search.SqlTemplates.multiFilterPagedSql;
import static search.SqlTemplates.topPagedSql;

public enum SearchPlan {
    MULTI_FILTER {
        @Override
        public PreparedStatement prepare(Connection c, SearchCriteria s, Page p, String orderKey) throws SQLException {
            String sql = multiFilterPagedSql(orderKey);
            PreparedStatement ps = c.prepareStatement(sql);
            int i = 1;

            String title = s.effectiveTitle();
            String likeTitle = (title != null && !title.isBlank()) ? "%" + title + "%" : null;
            ps.setObject(i++, likeTitle);
            ps.setString(i++, likeTitle);

            Integer year = s.hasYear() ? s.year() : null;
            ps.setObject(i++, year);
            if (year == null) ps.setNull(i++, Types.INTEGER);
            else ps.setInt(i++, year);

            String likeDir = s.hasDirector() ? "%" + s.director() + "%" : null;
            ps.setObject(i++, likeDir);
            ps.setString(i++, likeDir);

            String likeStar = s.hasStar() ? "%" + s.star() + "%" : null;
            ps.setObject(i++, likeStar);
            ps.setString(i++, likeStar);

            String letter = (s.hasInitial() && !"0-9".equalsIgnoreCase(s.initial())) ? s.initial() : null;
            ps.setObject(i++, letter);
            ps.setString(i++, letter);

            String digits = (s.hasInitial() && "0-9".equalsIgnoreCase(s.initial())) ? "0-9" : null;
            ps.setObject(i++, digits);

            Integer gid = s.hasGenreId() ? s.genreId() : null;
            ps.setObject(i++, gid);
            if (gid == null) ps.setNull(i++, Types.INTEGER);
            else ps.setInt(i++, gid);

            String gname = s.hasGenre() ? s.genre() : null;
            ps.setObject(i++, gname);
            ps.setString(i++, gname);

            ps.setInt(i++, p.limit());
            ps.setInt(i++, p.offset());
            return ps;
        }
    },

    // NEW: handle "no filters" – reuse the same paging/sorting pipeline
    TOP {
        @Override
        public PreparedStatement prepare(Connection c, SearchCriteria s, Page p, String orderKey) throws SQLException {
            String sql = topPagedSql(orderKey);
            PreparedStatement ps = c.prepareStatement(sql);
            int i = 1;
            ps.setInt(i++, p.limit());
            ps.setInt(i++, p.offset());
            return ps;
        }
    };

    public abstract PreparedStatement prepare(Connection c, SearchCriteria s, Page p, String orderKey) throws SQLException;

    // Back-compat 3-arg
    public PreparedStatement prepare(Connection c, SearchCriteria s, Page p) throws SQLException {
        return prepare(c, s, p, "r.rating DESC, m.title ASC, m.id ASC");
    }

    public static SearchPlan choose(SearchCriteria s) {
        if (!s.hasAny()) return TOP;
        return MULTI_FILTER;
    }
}
