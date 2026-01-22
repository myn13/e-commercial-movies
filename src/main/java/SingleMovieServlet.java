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
import search.MovieJsonWriter;
import performance.PerformanceLog;

import static utils.JsonUtils.*;

@WebServlet(name = "SingleMovieServlet", urlPatterns = {"/api/movies/*"})
public class SingleMovieServlet extends HttpServlet {
    private DataSource dataSource;
    private final Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();

    private static final String MOVIE_INFO =
            """
                                      WITH m_sel AS (
                                          SELECT m.id, m.title, m.year, m.director, r.rating, r.vote_count
                                          FROM movies m
                                                   JOIN ratings r ON r.movie_id = m.id
                                          WHERE m.id = ?
                                      ),
                                           stars_json AS (
                    SELECT JSON_ARRAYAGG(JSON_OBJECT('id', id, 'name', name)) AS stars
                    FROM (
                      SELECT s.id, s.name
                      FROM stars_in_movies sim
                      JOIN stars s ON s.id = sim.star_id
                      JOIN m_sel ms ON ms.id = sim.movie_id
                      LEFT JOIN (
                        SELECT star_id, COUNT(*) AS count
                        FROM stars_in_movies
                        GROUP BY star_id
                      ) sc ON sc.star_id = s.id
                      ORDER BY COALESCE(sc.count, 0) DESC, s.name ASC   -- use count ONLY to order
                    ) AS star_sorted
                    
                                           ),
                                           genres_json AS (
                                               SELECT JSON_ARRAYAGG(JSON_OBJECT('id', g_sorted.id, 'name', g_sorted.name)) AS genres
                                               FROM (
                                                        SELECT g.id, g.name
                                                        FROM genres_in_movies gim
                                                                 JOIN genres g ON g.id = gim.genre_id
                                                                 JOIN m_sel ms ON ms.id = gim.movie_id
                                                        ORDER BY g.name
                                                    ) AS g_sorted
                                           )
                                      SELECT
                                          ms.id,
                                          ms.title        AS title_name,
                                          ms.year,
                                          ms.director,
                                          ms.rating,
                                          ms.vote_count,
                                          COALESCE(a.stars,  JSON_ARRAY()) AS stars,
                                          COALESCE(g.genres, JSON_ARRAY()) AS genres
                                      FROM m_sel ms
                                               LEFT JOIN stars_json  a ON TRUE
                                               LEFT JOIN genres_json g ON TRUE;   
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
            response.getWriter().write("{\"error\":\"Missing movie in URL (expected /movies/{id})\"}");
            // Log performance for early return (no database operations, so tj = 0)
            long tsEndTime = System.nanoTime();
            long ts = tsEndTime - tsStartTime;
            PerformanceLog.log(ts, 0);
            return;
        }

        String raw = pathInfo.substring(1); // "tt0094859"
        String movieId = URLDecoder.decode(raw, StandardCharsets.UTF_8);

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
            preparedStatement = connection.prepareStatement(MOVIE_INFO);
            preparedStatement.setString(1, movieId);
            dbEnd = System.nanoTime();
            tj += (dbEnd - dbStart);

            // 4. Measure Query Execution Time
            dbStart = System.nanoTime();
            resultSet = preparedStatement.executeQuery();
            dbEnd = System.nanoTime();
            tj += (dbEnd - dbStart);

            jsonWriter = new JsonWriter(response.getWriter());

            boolean found = MovieJsonWriter.writeSingleIfPresent(jsonWriter, resultSet);
            if (!found) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                // Log performance for not found case
                long tsEndTime = System.nanoTime();
                long ts = tsEndTime - tsStartTime;
                System.out.println("PerformanceLog (not found): ts=" + ts + " ns, tj=" + tj + " ns");
                PerformanceLog.log(ts, tj);
                return;
            }

            System.out.println("SingleMovieServlet: query succeeded for id=" + movieId);

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
            MovieServlet.ErrorPayload err = new MovieServlet.ErrorPayload(e.getClass().getName(), e.getMessage());
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
            try { if (resultSet != null) resultSet.close(); } catch (SQLException e) { /* ignored */ }
            try { if (preparedStatement != null) preparedStatement.close(); } catch (SQLException e) { /* ignored */ }
            try { if (connection != null) connection.close(); } catch (SQLException e) { /* ignored */ }
            try { if (jsonWriter != null) jsonWriter.close(); } catch (IOException e) { /* ignored */ }
        }
    }

    record ErrorPayload(String code, String message) {
    }
}