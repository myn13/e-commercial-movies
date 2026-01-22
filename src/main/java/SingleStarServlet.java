import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import performance.PerformanceLog;

import static utils.JsonUtils.*;

@WebServlet(name = "SingleStarServlet", urlPatterns = {"/stars/*"})
public class SingleStarServlet extends HttpServlet {
    private DataSource dataSource;
    private final Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();

    private static final String STAR_INFO =
            """
                    WITH star_sel AS (
                      SELECT s.id, s.name AS star_name, s.birth_year
                      FROM stars s
                      WHERE s.id = ?
                    ),
                    movies_sorted AS (
                      SELECT m.id, m.title, m.year
                      FROM stars_in_movies sim
                      JOIN movies m ON m.id = sim.movie_id
                      JOIN star_sel ss ON ss.id = sim.star_id
                      ORDER BY m.year DESC, m.title ASC
                    )
                    SELECT
                      ss.id,
                      ss.star_name,
                      ss.birth_year,
                      (
                        SELECT JSON_ARRAYAGG(JSON_OBJECT('id', ms.id, 'title', ms.title, 'year', ms.year))
                        FROM movies_sorted ms
                      ) AS movies
                    FROM star_sel ss;
                    
                    """;

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

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/") || pathInfo.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Missing star id in URL (expected /stars/{id})\"}");
            // Log performance for early return (no database operations, so tj = 0)
            long tsEndTime = System.nanoTime();
            long ts = tsEndTime - tsStartTime;
            PerformanceLog.log(ts, 0);
            return;
        }

        String raw = pathInfo.substring(1); //"nm0094859"
        String starId = URLDecoder.decode(raw, StandardCharsets.UTF_8);

        DataSource localDataSource = this.dataSource;
        if (localDataSource == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter out = response.getWriter()) {
                out.write(gson.toJson(new ErrorPayload("DATA_SOURCE_NOT_INITIALIZED", "DataSource has not been configured")));
            }
            // Log performance for early return (no database operations, so tj = 0)
            long tsEndTime = System.nanoTime();
            long ts = tsEndTime - tsStartTime;
            PerformanceLog.log(ts, 0);
            return;
        }

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        JsonWriter jsonWriter = null;

        try {
            // 2. Measure Connection Time
            dbStart = System.nanoTime();
            connection = localDataSource.getConnection();
            dbEnd = System.nanoTime();
            tj += (dbEnd - dbStart);

            // 3. Measure Statement Preparation Time
            dbStart = System.nanoTime();
            preparedStatement = connection.prepareStatement(STAR_INFO);
            preparedStatement.setString(1, starId);
            dbEnd = System.nanoTime();
            tj += (dbEnd - dbStart);

            // 4. Measure Query Execution Time
            dbStart = System.nanoTime();
            resultSet = preparedStatement.executeQuery();
            dbEnd = System.nanoTime();
            tj += (dbEnd - dbStart);

            jsonWriter = new JsonWriter(response.getWriter());

            if (!resultSet.next()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                try (PrintWriter out = response.getWriter()) {
                    out.write(gson.toJson(new ErrorPayload("NOT_FOUND", "No star found with the given id")));
                }
                // Log performance for not found case
                long tsEndTime = System.nanoTime();
                long ts = tsEndTime - tsStartTime;
                System.out.println("PerformanceLog (not found): ts=" + ts + " ns, tj=" + tj + " ns");
                PerformanceLog.log(ts, tj);
                return;
            }

            jsonWriter.beginObject();
            writeIfNotNull(jsonWriter, "id", resultSet.getString("id"));
            writeIfNotNull(jsonWriter, "name", resultSet.getString("star_name"));
            writeIfNotNull(jsonWriter, "birth_year", resultSet.getObject("birth_year"));

            jsonWriter.name("movies");
            writeJsonArrayOrEmpty(jsonWriter, resultSet.getString("movies"));

            jsonWriter.endObject();

            System.out.println("SingleStarServlet: query succeeded for id=" + starId);

            // 5. END Ts and Log (success case)
            long tsEndTime = System.nanoTime();
            long ts = tsEndTime - tsStartTime;

            // Debug logging
            System.out.println("PerformanceLog: ts=" + ts + " ns, tj=" + tj + " ns");

            PerformanceLog.log(ts, tj);

        } catch (Exception e) {
            // If exception occurred during database operations, still measure tj up to the point of failure
            // (tj is already accumulated from previous operations)
            
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter out = response.getWriter()) {
                out.write(gson.toJson(new ErrorPayload(e.getClass().getSimpleName(), e.getMessage())));
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
            try { if (resultSet != null) resultSet.close(); } catch (SQLException e) { /* ignored */ }
            try { if (preparedStatement != null) preparedStatement.close(); } catch (SQLException e) { /* ignored */ }
            try { if (connection != null) connection.close(); } catch (SQLException e) { /* ignored */ }
            try { if (jsonWriter != null) jsonWriter.close(); } catch (IOException e) { /* ignored */ }
        }
    }

    record ErrorPayload(String code, String message) {
    }
}
