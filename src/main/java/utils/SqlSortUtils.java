// utils/SqlSortUtils.java
package utils;

import jakarta.servlet.http.HttpServletRequest;

public final class SqlSortUtils {
    private SqlSortUtils() {
    }

    public static String defaultOrderKey() {
        // Classic "top movies" default
        return "r.rating DESC, m.title ASC, m.id ASC";
    }

    private static String normField(String f, String def) {
        if ("rating".equalsIgnoreCase(f)) return "rating";
        if ("title".equalsIgnoreCase(f)) return "title";
        return def; // use provided default
    }

    private static String normDir(String d, String def) {
        if ("desc".equalsIgnoreCase(d)) return "DESC";
        if ("asc".equalsIgnoreCase(d)) return "ASC";
        return def;
    }

    private static String colFor(String field) {
        return switch (field) {
            case "rating" -> "r.rating";
            case "title" -> "m.title";
            default -> "m.title";
        };
    }

    /**
     * Returns only the key: "r.rating DESC, m.title ASC, m.id ASC"
     */
    public static String buildOrderKey(HttpServletRequest req) {
        // If NO sort params at all, use default
        boolean hasAny =
                req.getParameter("order1") != null ||
                        req.getParameter("dir1") != null ||
                        req.getParameter("order2") != null ||
                        req.getParameter("dir2") != null;
        if (!hasAny) return defaultOrderKey();

        // Otherwise normalize with sensible fallbacks that still bias to the default
        String o1 = normField(req.getParameter("order1"), "rating");
        String d1 = normDir(req.getParameter("dir1"), "DESC");
        String o2 = normField(req.getParameter("order2"), "title");
        String d2 = normDir(req.getParameter("dir2"), "ASC");

        if (o1.equals(o2)) {
            o2 = "title".equals(o1) ? "rating" : "title";
            // keep d2
        }

        String c1 = colFor(o1);
        String c2 = colFor(o2);
        return c1 + " " + d1 + ", " + c2 + " " + d2 + ", m.id ASC";
    }

    public static String buildOrderBy(HttpServletRequest req) {
        return "ORDER BY " + buildOrderKey(req);
    }
}
