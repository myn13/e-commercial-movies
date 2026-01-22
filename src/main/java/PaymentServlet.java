import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

// this is a code freeze branch!!!
@WebServlet(name = "PaymentServlet", urlPatterns = "/payment")
public class PaymentServlet extends HttpServlet {
    private DataSource dataSource;

    @Override
    public void init() throws ServletException {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/my_data_source");
            System.out.println("PaymentConfirmationServlet initialized with DataSource");
        } catch (NamingException e) {
            // This error means the DataSource is not configured correctly in the server's context.xml or web.xml
            throw new ServletException("Failed to initialize DataSource. Check your JNDI setup.", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            String jsonBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            System.out.println("PaymentServlet received: " + jsonBody);

            if (jsonBody == null || jsonBody.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"status\": \"error\", \"message\": \"Empty request body\"}");
                return;
            }

            PaymentData paymentData = new Gson().fromJson(jsonBody, PaymentData.class);
            // Override customer ID with hardcoded value 490010
            int hardcodedCustomerId = 490010;
            paymentData.setCustomerId(hardcodedCustomerId);
            System.out.println("PaymentData: customerId=" + paymentData.getCustomerId() + ", movieId=" + paymentData.getMovieId() + ", saleDate=" + paymentData.getSaleDate());

            Connection connection = null;
            PreparedStatement preparedStatement = null;
            // Assuming ID is auto-generated, so we skip it and use NULL
            final String query = "INSERT INTO sales (customer_id, movie_id, sale_date) VALUES (?, ?, ?)";

            try {
                connection = dataSource.getConnection();
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setInt(1, paymentData.getCustomerId());
                preparedStatement.setString(2, paymentData.getMovieId());
                preparedStatement.setString(3, paymentData.getSaleDate());

                int rowsAffected = preparedStatement.executeUpdate();
                if (rowsAffected > 0) {
                    String successMessage = "{\"status\": \"success\", \"message\": \"Sale recorded successfully for customer ID: " + paymentData.getCustomerId() + "\"}";
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write(successMessage);
                    System.out.println("Sale recorded successfully");
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().write("{\"status\": \"error\", \"message\": \"Failed to record sale\"}");
                    System.err.println("Failed to insert sale into database");
                }
            } catch (SQLException e) {
                System.err.println("SQL Error during DB operation: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"status\": \"error\", \"message\": \"Database error: " + e.getMessage() + "\"}");
            } finally {
                // Close resources. Closing the connection here returns it to the pool.
                try {
                    if (preparedStatement != null) preparedStatement.close();
                } catch (SQLException e) { /* log and ignore */ }
                try {
                    if (connection != null) connection.close();
                } catch (SQLException e) { /* log and ignore */ }
            }
        } catch (Exception e) {
            System.err.println("Error in PaymentServlet: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"status\": \"error\", \"message\": \"Server error: " + e.getMessage() + "\"}");
        }
    }
}