import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import performance.PerformanceLog;
import search.Page;
import search.SearchCriteria;
import search.SearchPlan;

import static utils.JsonUtils.*;


@WebServlet(name = "SearchServlet", urlPatterns = {"/api/search"})
public class SearchServlet extends HttpServlet {
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
        // 1. START Ts (Total Servlet Time)
        long tsStartTime = System.nanoTime();

        // Initialize Tj (Total JDBC Time) accumulator
        long tj = 0;
        long dbStart, dbEnd;

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        int limit = parseIntOrDefault(request.getParameter("limit"), 20, 1, 100);
        int offset = parseIntOrDefault(request.getParameter("offset"), 0, 0, 10_000);
        String title = request.getParameter("title");
        String director = request.getParameter("director");
        String star = request.getParameter("star");
        String initial = request.getParameter("initial");
        Integer year = parseNullableInt(request.getParameter("year"));
        Integer genreId = parseNullableInt(request.getParameter("genreId"));
        String genreName = request.getParameter("genre");

        String orderKey = utils.SqlSortUtils.buildOrderKey(request);

        SearchCriteria criteria = new SearchCriteria(title, director, star, year, initial, genreId, genreName);
        Page page = new Page(limit, offset);

        DataSource localDataSource = this.dataSource;

        if (localDataSource == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            ErrorPayload err = new ErrorPayload(
                    "DATA_SOURCE_NOT_INITIALIZED",
                    "DataSource has not been configured"
            );
            try (PrintWriter out = response.getWriter()) {
                out.write(gson.toJson(err));
            }
            // Log performance for early return (no database operations, so tj = 0)
            long tsEndTime = System.nanoTime();
            long ts = tsEndTime - tsStartTime;
            PerformanceLog.log(ts, 0);
            return;
        }

        SearchPlan plan;
        try {
            plan = SearchPlan.choose(criteria);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json;charset=UTF-8");
            try (PrintWriter out = response.getWriter()) {
                out.write(gson.toJson(new ErrorPayload("INVALID_QUERY", e.getMessage())));
            }
            // Log performance for early return (no database operations, so tj = 0)
            long tsEndTime = System.nanoTime();
            long ts = tsEndTime - tsStartTime;
            PerformanceLog.log(ts, 0);
            return;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        JsonWriter jsonWriter = null;

        try {
            dbStart = System.nanoTime();
            conn = dataSource.getConnection();
            dbEnd = System.nanoTime();
            tj += (dbEnd - dbStart);

            // 3. Measure Statement Preparation Time
            // (Assuming plan.prepare calls conn.prepareStatement internaly)
            dbStart = System.nanoTime();
            ps = plan.prepare(conn, criteria, page, orderKey);
            dbEnd = System.nanoTime();
            tj += (dbEnd - dbStart);

            // 4. Measure Query Execution Time
            dbStart = System.nanoTime();
            rs = ps.executeQuery();
            dbEnd = System.nanoTime();
            tj += (dbEnd - dbStart);

            jsonWriter = new JsonWriter(response.getWriter());
            search.MovieJsonWriter.writeArray(jsonWriter, rs);
            
            // 7. END Ts and Log (success case)
            long tsEndTime = System.nanoTime();
            long ts = tsEndTime - tsStartTime;

            // Debug logging
            System.out.println("PerformanceLog: ts=" + ts + " ns, tj=" + tj + " ns");

            PerformanceLog.log(ts, tj);
        } catch (Exception e) {
            // If exception occurred during database operations, still measure tj up to the point of failure
            // (tj is already accumulated from previous operations)
            
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            ErrorPayload err = new ErrorPayload(e.getClass().getName(), e.getMessage());
            try (PrintWriter out = response.getWriter()) {
                out.write(gson.toJson(err));
            }
            e.printStackTrace();
            
            // Log performance immediately after error operation
            long tsEndTime = System.nanoTime();
            long ts = tsEndTime - tsStartTime;

            // Debug logging
            System.out.println("PerformanceLog (error): ts=" + ts + " ns, tj=" + tj + " ns");

            // Log immediately after error operation
            PerformanceLog.log(ts, tj);
        } finally {
            // 6. Close Resources Manually
            try { if (rs != null) rs.close(); } catch (SQLException e) { /* ignored */ }
            try { if (ps != null) ps.close(); } catch (SQLException e) { /* ignored */ }
            try { if (conn != null) conn.close(); } catch (SQLException e) { /* ignored */ }
            try { if (jsonWriter != null) jsonWriter.close(); } catch (IOException e) { /* ignored */ }
        }
    }

    record ErrorPayload(String code, String message) {
    }

}