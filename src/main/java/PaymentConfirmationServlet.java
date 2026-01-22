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
import java.sql.SQLException;
import java.time.LocalDate;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

@WebServlet("/payment/process-payment")
public class PaymentConfirmationServlet extends HttpServlet {

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
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String jsonString = "";

        try {
            StringBuilder jsonBuffer = new StringBuilder();
            String line;
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                jsonBuffer.append(line);
            }

            jsonString = jsonBuffer.toString();
            System.out.println("PaymentConfirmationServlet - Received payment request: " + jsonString);

            if (jsonString.trim().isEmpty()) {
                System.err.println("PaymentConfirmationServlet - Empty request body");
                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("error", "Empty request body");
                errorResponse.addProperty("errorCode", "EMPTY_REQUEST");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(errorResponse.toString());
                return;
            }

            JsonObject jsonRequest = JsonParser.parseString(jsonString).getAsJsonObject();

            // Get session
            HttpSession session = request.getSession();

            // Get cart items from session
            Object cartItemsObj = session.getAttribute("cartItems");

            if (cartItemsObj == null) {
                System.err.println("PaymentConfirmationServlet - Cart is empty");
                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("error", "Cart is empty");
                errorResponse.addProperty("errorCode", "EMPTY_CART");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(errorResponse.toString());
                return;
            }

            // Convert cartItems to JsonObject
            JsonObject cartItems;
            if (cartItemsObj instanceof JsonObject) {
                cartItems = (JsonObject) cartItemsObj;
            } else {
                // Convert HashMap to JsonObject (assuming the cart is stored as a Map)
                com.google.gson.Gson gson = new com.google.gson.Gson();
                String cartItemsJson = gson.toJson(cartItemsObj);
                cartItems = JsonParser.parseString(cartItemsJson).getAsJsonObject();
            }

            if (cartItems.size() == 0) { // Check size, as .isEmpty() is for Map/Collection, not JsonObject in a Gson version context
                System.err.println("PaymentConfirmationServlet - Cart is empty");
                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("error", "Cart is empty");
                errorResponse.addProperty("errorCode", "EMPTY_CART");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(errorResponse.toString());
                return;
            }

            System.out.println("PaymentConfirmationServlet - Found " + cartItems.size() + " items in cart");

            // Validate credit card
            String validationError = validateCreditCard(jsonRequest);
            if (validationError != null) {
                System.err.println("PaymentConfirmationServlet - Credit card validation failed: " + validationError);
                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("error", validationError);
                errorResponse.addProperty("errorCode", "INVALID_CARD");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(errorResponse.toString());
                return;
            }

            System.out.println("PaymentConfirmationServlet - Credit card validated successfully");

            // Payment is successful - record in database
            System.out.println("PaymentConfirmationServlet - Recording sales in database...");
            boolean salesRecorded = recordSalesInDatabase(jsonRequest, cartItems, session);

            if (!salesRecorded) {
                // Error response for database failure is already logged in the recording method
                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("error", "Payment successful but failed to record sale in database (Check server logs for SQLException)");
                errorResponse.addProperty("errorCode", "DATABASE_ERROR");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write(errorResponse.toString());
                return;
            }

            System.out.println("PaymentConfirmationServlet - Sales recorded successfully in database");

            // Clear cart after successful recording
            session.removeAttribute("cartItems");

            // Return success response
            String transactionId = "TXN_" + System.currentTimeMillis();
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "succeeded");
            responseJson.addProperty("message", "Payment successful and sale recorded!");
            responseJson.addProperty("transactionId", transactionId);

            response.getWriter().write(responseJson.toString());

        } catch (com.google.gson.JsonSyntaxException e) {
            System.err.println("PaymentConfirmationServlet - JSON parsing error: " + e.getMessage());
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Invalid JSON format in request");
            errorResponse.addProperty("errorCode", "INVALID_JSON");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(errorResponse.toString());
        } catch (Exception e) {
            System.err.println("PaymentConfirmationServlet error: " + e.getMessage());
            e.printStackTrace();
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Server error: " + e.getMessage());
            errorResponse.addProperty("errorCode", "SERVER_ERROR");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(errorResponse.toString());
        }
    }

    private boolean recordSalesInDatabase(JsonObject paymentData, JsonObject cartItems, HttpSession session) {
        System.out.println("PaymentConfirmationServlet - Starting database recording...");

        Connection conn = null;

        try {
            conn = dataSource.getConnection();
            System.out.println("PaymentConfirmationServlet - Database connection established");
            conn.setAutoCommit(false); // Start transaction

            // Get customer ID as String (from session)
            String customerIdStr = (String) session.getAttribute("username");
            if (customerIdStr == null || customerIdStr.isEmpty()) {
                customerIdStr = "490999"; // Default customer ID if no username in session
            }

            // --- Parse customer ID as INT (necessary if the column is INT) ---
            int customerIntId = -1;
            try {
                customerIntId = Integer.parseInt(customerIdStr);
            } catch (NumberFormatException e) {
                System.err.println("PaymentConfirmationServlet - CRITICAL: Customer ID '" + customerIdStr + "' is not a valid integer. Sales will likely fail.");
                // We'll proceed with -1, but this indicates a setup error.
            }

            System.out.println("PaymentConfirmationServlet - Using customer ID (String/Int): " + customerIdStr + "/" + customerIntId);

            // Credit card saving logic (runs outside the main sales transaction rollback scope)
            JsonObject paymentMethod = paymentData.getAsJsonObject("paymentMethod");
            if (paymentMethod != null) {
                try {
                    String cardNumber = paymentMethod.get("cardNumber").getAsString();
                    String cardHolderName = paymentMethod.get("cardHolderName").getAsString();
                    int expiryMonth = paymentMethod.get("expiryMonth").getAsInt();
                    int expiryYear = paymentMethod.get("expiryYear").getAsInt();
                    String cvv = paymentMethod.has("cvv") ? paymentMethod.get("cvv").getAsString() : "000";
                    saveCreditCard(cardNumber, cardHolderName, expiryMonth, expiryYear, cvv);
                } catch (Exception e) {
                    System.err.println("PaymentConfirmationServlet - Failed to save credit card, continuing sale: " + e.getMessage());
                }
            }

            // Updated schema with id column
            String insertSql = "INSERT INTO moviedb.sales (id, customer_id, movie_id, sale_date) VALUES (?, ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                LocalDate saleDate = LocalDate.now();
                int totalRecords = 0;
                int saleId = 1; // Starting ID, you might want to auto-increment or generate unique IDs

                System.out.println("PaymentConfirmationServlet - Preparing to insert sales records...");

                for (String movieIdStr : cartItems.keySet()) {
                    JsonObject movie = cartItems.getAsJsonObject(movieIdStr);
                    int quantity = movie.get("quantity").getAsInt();

                    // --- Parse movie ID as INT (necessary if the column is INT) ---
                    int movieIntId = -1;
                    try {
                        movieIntId = Integer.parseInt(movieIdStr);
                    } catch (NumberFormatException e) {
                        System.err.println("PaymentConfirmationServlet - SKIPPING: Movie ID key '" + movieIdStr + "' is not a valid integer. Check cart structure.");
                        continue;
                    }

                    System.out.println("Binding parameters: ID=" + saleId + ", CustomerID=" + customerIntId + ", MovieID=" + movieIntId + ", Quantity=" + quantity);

                    for (int i = 0; i < quantity; i++) {
                        stmt.setInt(1, saleId++);      // ID (auto-increment in database or generate unique)
                        stmt.setInt(2, customerIntId); // Customer ID
                        stmt.setInt(3, movieIntId);    // Movie ID
                        stmt.setDate(4, java.sql.Date.valueOf(saleDate)); // Sale date
                        stmt.addBatch();
                        totalRecords++;
                    }
                }

                if (totalRecords == 0) {
                    System.err.println("PaymentConfirmationServlet - No valid records were added to the batch. Cart keys may not be integers.");
                    conn.rollback();
                    return false;
                }

                System.out.println("PaymentConfirmationServlet - Executing batch insert for " + totalRecords + " records...");

                int[] results = stmt.executeBatch();
                System.out.println("PaymentConfirmationServlet - Successfully recorded " + results.length + " sales (expected " + totalRecords + ")");

                // Commit transaction
                conn.commit();
                System.out.println("PaymentConfirmationServlet - Transaction committed successfully");

                return true;
            }

        } catch (SQLException e) {
            System.err.println("PaymentConfirmationServlet - Database error recording sales (ROLLBACK): " + e.getMessage());
            System.err.println("PaymentConfirmationServlet - SQL State: " + e.getSQLState());
            System.err.println("PaymentConfirmationServlet - Error Code: " + e.getErrorCode());
            e.printStackTrace();

            // Rollback transaction on error
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackException) {
                    System.err.println("Failed to rollback: " + rollbackException.getMessage());
                }
            }

            return false;
        } catch (Exception e) {
            System.err.println("PaymentConfirmationServlet - Unexpected runtime error: " + e.getMessage());
            e.printStackTrace();

            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackException) {
                    System.err.println("Failed to rollback: " + rollbackException.getMessage());
                }
            }

            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Failed to close connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Validate credit card information
     * Returns error message if invalid, null if valid
     */
    private String validateCreditCard(JsonObject paymentData) {
        // Validation logic remains the same (omitted for brevity)
        // ... (original validation code) ...
        try {
            // Check payment method exists
            if (!paymentData.has("paymentMethod")) {
                return "Payment method information is required";
            }

            JsonObject paymentMethod = paymentData.getAsJsonObject("paymentMethod");

            // Validate card number
            if (!paymentMethod.has("cardNumber") || paymentMethod.get("cardNumber").getAsString().trim().isEmpty()) {
                return "Card number is required";
            }

            String cardNumber = paymentMethod.get("cardNumber").getAsString().replaceAll("\\s", "");
            if (cardNumber.length() < 13 || cardNumber.length() > 19) {
                return "Card number must be 13-19 digits";
            }

            // Validate Luhn algorithm
            if (!isValidLuhn(cardNumber)) {
                return "Invalid card number";
            }

            // Validate card holder name
            if (!paymentMethod.has("cardHolderName") || paymentMethod.get("cardHolderName").getAsString().trim().isEmpty()) {
                return "Card holder name is required";
            }

            String cardHolderName = paymentMethod.get("cardHolderName").getAsString().trim();
            String[] nameParts = cardHolderName.split("\\s+");
            if (nameParts.length < 2) {
                return "Please enter first and last name";
            }

            // Validate expiry date
            if (!paymentMethod.has("expiryMonth") || !paymentMethod.has("expiryYear")) {
                return "Expiry date is required";
            }

            int expiryMonth = paymentMethod.get("expiryMonth").getAsInt();
            int expiryYear = paymentMethod.get("expiryYear").getAsInt();

            if (expiryMonth < 1 || expiryMonth > 12) {
                return "Invalid expiry month";
            }

            // Check if card is expired
            int currentYear = java.time.Year.now().getValue();
            int currentMonth = java.time.MonthDay.now().getMonthValue();

            if (expiryYear < currentYear || (expiryYear == currentYear && expiryMonth < currentMonth)) {
                return "Credit card has expired";
            }

            // Validate CVV
            if (paymentMethod.has("cvv")) {
                String cvv = paymentMethod.get("cvv").getAsString();
                if (cvv != null && !cvv.isEmpty() && cvv.length() != 3 && cvv.length() != 4) {
                    return "CVV must be 3 or 4 digits";
                }
            }

            System.out.println("PaymentConfirmationServlet - Credit card validation passed");
            return null; // Valid

        } catch (Exception e) {
            System.err.println("Validation error: " + e.getMessage());
            return "Invalid payment information";
        }
    }

    /**
     * Luhn algorithm implementation for card number validation
     */
    private boolean isValidLuhn(String cardNumber) {
        try {
            int sum = 0;
            boolean alternate = false;

            for (int i = cardNumber.length() - 1; i >= 0; i--) {
                int digit = Character.getNumericValue(cardNumber.charAt(i));

                if (alternate) {
                    digit *= 2;
                    if (digit > 9) {
                        digit = (digit % 10) + 1;
                    }
                }

                sum += digit;
                alternate = !alternate;
            }

            return (sum % 10) == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Save credit card information to the credit_cards table
     */
    private void saveCreditCard(String cardNumber, String cardHolderName, int expiryMonth, int expiryYear, String cvv) {
        Connection conn = null;

        try {
            conn = dataSource.getConnection();

            // Check if card already exists
            String checkSql = "SELECT COUNT(*) FROM moviedb.credit_cards WHERE id = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, cardNumber);
                java.sql.ResultSet rs = checkStmt.executeQuery();

                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("PaymentConfirmationServlet - Credit card already exists in database");
                    return;
                }
            }

            // Insert new credit card with correct schema: (id, first_name, expiration)
            // The id column is used to store the card number
            String insertSql = "INSERT INTO moviedb.credit_cards (id, first_name, expiration) VALUES (?, ?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                String[] nameParts = cardHolderName.trim().split("\\s+", 2);
                String firstName = nameParts[0];
                
                // Format: MM/YY
                String expiration = String.format("%02d/%d", expiryMonth, expiryYear);

                insertStmt.setString(1, cardNumber);
                insertStmt.setString(2, firstName);
                insertStmt.setString(3, expiration);

                insertStmt.executeUpdate();
                System.out.println("PaymentConfirmationServlet - Credit card saved to database: " + cardNumber);
            }

        } catch (SQLException e) {
            // Log this error but do not re-throw, as the main sales transaction can proceed
            System.err.println("Failed to save credit card to database: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error saving credit card: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Failed to close connection: " + e.getMessage());
                }
            }
        }
    }
}
