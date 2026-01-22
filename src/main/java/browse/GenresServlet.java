package browse;

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

import static utils.JsonUtils.*;


@WebServlet(name = "GenresServlet", urlPatterns = {"/genres"})
public class GenresServlet extends HttpServlet {
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
        response.setContentType("application/json;charset=UTF-8");
        DataSource localDataSource = this.dataSource;

        if (localDataSource == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            ErrorPayload err = new ErrorPayload("DATA_SOURCE_NOT_INITIALIZED", "DataSource has not been configured");
            try (PrintWriter out = response.getWriter()) {
                out.write(gson.toJson(err));
            }
            return;
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(
                     "SELECT id, name FROM genres ORDER BY name ASC"
             );
             ResultSet resultSet = preparedStatement.executeQuery();
             JsonWriter jsonWriter = new JsonWriter(response.getWriter())) {

            jsonWriter.beginArray();
            while (resultSet.next()) {
                jsonWriter.beginObject();
                writeIfNotNull(jsonWriter, "id", resultSet.getObject("id"));
                writeIfNotNull(jsonWriter, "name", resultSet.getString("name"));
                jsonWriter.endObject();
            }
            jsonWriter.endArray();

        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            ErrorPayload err = new ErrorPayload(e.getClass().getName(), e.getMessage());
            try (PrintWriter out = response.getWriter()) {
                out.write(gson.toJson(err));
            }
            e.printStackTrace();
        }

    }

    record ErrorPayload(String code, String message) {
    }

}