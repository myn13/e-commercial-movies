import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.google.gson.stream.JsonWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import static utils.JsonUtils.parseIntOrDefault;
import static utils.JsonUtils.parseNullableInt;
import static utils.SqlSortUtils.*;

import search.Page;
import search.SearchPlan;
import search.SqlTemplates;
import search.SearchCriteria;


@WebServlet(name = "MovieServlet", urlPatterns = {"/movies_page"})
public class MovieServlet extends HttpServlet {
    private DataSource dataSource;
    private final Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/my_data_source");
        } catch (NamingException e) {
            throw new ServletException("Failed to initialize DataSource", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        int requested = parseIntOrDefault(request.getParameter("limit"), 25, 1, 100);
        int limit = switch (requested) {
            case 10, 25, 50, 100 -> requested;
            default -> 25;
        };
        int offset = parseIntOrDefault(request.getParameter("offset"), 0, 0, 10_000_000);

        String orderKey = buildOrderKey(request); // "m.title ASC, r.rating DESC, m.id ASC"

        DataSource ds = this.dataSource;
        if (ds == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter out = response.getWriter()) {
                out.write(gson.toJson(new ErrorPayload("DATA_SOURCE_NOT_INITIALIZED", "DataSource has not been configured")));
            }
            return;
        }

        String sql = SqlTemplates.topPagedSql(orderKey);

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             JsonWriter jw = new JsonWriter(response.getWriter())) {

            int i = 1;
            ps.setInt(i++, limit);
            ps.setInt(i++, offset);

            try (ResultSet rs = ps.executeQuery()) {
                search.MovieJsonWriter.writeArray(jw, rs);
            }

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter out = response.getWriter()) {
                out.write(gson.toJson(new ErrorPayload(e.getClass().getName(), e.getMessage())));
            }
            e.printStackTrace();
        }
    }

    record ErrorPayload(String code, String message) {
    }
}
