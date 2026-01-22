import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "CartServlet", urlPatterns = "/shopping_cart")
public class CartServlet extends HttpServlet {
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try {
            HttpSession session = request.getSession();
            String sessionId = session.getId();
            long lastAccessTime = session.getLastAccessedTime();
            
            @SuppressWarnings("unchecked")
            Map<String, JsonObject> cartItems = (Map<String, JsonObject>) session.getAttribute("cartItems");
            if (cartItems == null) {
                cartItems = new HashMap<>();
            }
            
            JsonObject responseJsonObject = new JsonObject();
            responseJsonObject.addProperty("sessionId", sessionId);
            responseJsonObject.addProperty("lastAccessTime", new Date(lastAccessTime).toString());
            
            JsonArray cartItemsJsonArray = new JsonArray();
            for (JsonObject movieItem : cartItems.values()) {
                cartItemsJsonArray.add(movieItem);
            }
            responseJsonObject.add("cartItems", cartItemsJsonArray);

            double totalPrice = 0.0;
            for (JsonObject movieItem : cartItems.values()) {
                totalPrice += movieItem.get("price").getAsDouble() * movieItem.get("quantity").getAsInt();
            }
            responseJsonObject.addProperty("totalPrice", totalPrice);
            
            System.out.println("Getting " + cartItems.size() + " cart items");
            response.getWriter().write(responseJsonObject.toString());
            
        } catch (Exception e) {
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Failed to get cart items: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(errorResponse.toString());
        }
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try {
            // Read JSON request body
            StringBuilder jsonBuffer = new StringBuilder();
            String line;
            while ((line = request.getReader().readLine()) != null) {
                jsonBuffer.append(line);
            }
            
            JsonObject requestJson = JsonParser.parseString(jsonBuffer.toString()).getAsJsonObject();
            String movieId = requestJson.get("id").getAsString();
            String movieTitle = requestJson.get("title").getAsString();
            int quantity = requestJson.get("quantity").getAsInt();
            
            if (movieId == null || movieId.trim().isEmpty()) {
                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("error", "Movie ID is required");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(errorResponse.toString());
                return;
            }
            
            HttpSession session = request.getSession();
            
            @SuppressWarnings("unchecked")
            Map<String, JsonObject> cartItems = (Map<String, JsonObject>) session.getAttribute("cartItems");
            if (cartItems == null) {
                cartItems = new HashMap<>();
            }
            
            JsonObject responseJsonObject = new JsonObject();

            if (cartItems.containsKey(movieId)) {
                JsonObject existingItem = cartItems.get(movieId);
                int currentQuantity = existingItem.get("quantity").getAsInt();
                int newQuantity = currentQuantity + quantity;
                existingItem.addProperty("quantity", newQuantity);
                responseJsonObject.addProperty("message", "Movie quantity updated in cart successfully");
                System.out.println("Updating quantity for movie: " + movieId + " from " + currentQuantity + " to " + newQuantity);
            } else {
                JsonObject movieItem = new JsonObject();
                movieItem.addProperty("id", movieId);
                movieItem.addProperty("title", movieTitle);
                movieItem.addProperty("price", 10.0); // Fixed price
                movieItem.addProperty("quantity", quantity);
                cartItems.put(movieId, movieItem);
                responseJsonObject.addProperty("message", "Movie added to cart successfully");
                System.out.println("Adding movie to cart: " + movieTitle + " (ID: " + movieId + ", Price: $10.0, Qty: " + quantity + ")");
            }
            
            // Save the updated cart items back to the session
            session.setAttribute("cartItems", cartItems);
            
            JsonArray cartItemsJsonArray = new JsonArray();
            for (JsonObject movieItem : cartItems.values()) {
                cartItemsJsonArray.add(movieItem);
            }
            responseJsonObject.add("cartItems", cartItemsJsonArray);
            
            // Calculate total price
            double totalPrice = 0.0;
            for (JsonObject movieItem : cartItems.values()) {
                totalPrice += movieItem.get("price").getAsDouble() * movieItem.get("quantity").getAsInt();
            }
            responseJsonObject.addProperty("totalPrice", totalPrice);
            
            response.getWriter().write(responseJsonObject.toString());
            
        } catch (Exception e) {
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Failed to add item to cart: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(errorResponse.toString());
        }
    }
    
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try {
            // Read JSON request body
            StringBuilder jsonBuffer = new StringBuilder();
            String line;
            while ((line = request.getReader().readLine()) != null) {
                jsonBuffer.append(line);
            }
            
            JsonObject requestJson = JsonParser.parseString(jsonBuffer.toString()).getAsJsonObject();
            String movieId = requestJson.get("movieId").getAsString();
            String action = requestJson.get("action").getAsString();
            
            if (movieId == null || movieId.trim().isEmpty()) {
                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("error", "Movie ID is required");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(errorResponse.toString());
                return;
            }
            
            HttpSession session = request.getSession();
            
            @SuppressWarnings("unchecked")
            Map<String, JsonObject> cartItems = (Map<String, JsonObject>) session.getAttribute("cartItems");
            if (cartItems == null) {
                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("error", "Cart is empty");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(errorResponse.toString());
                return;
            }
            
            JsonObject responseJsonObject = new JsonObject();
            boolean itemExists = cartItems.containsKey(movieId);
            
            if (!itemExists) {
                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("error", "Movie not found in cart");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(errorResponse.toString());
                return;
            }
            
            synchronized (cartItems) {
                JsonObject movieItem = cartItems.get(movieId);
                int currentQuantity = movieItem.get("quantity").getAsInt();
                int newQuantity = currentQuantity;
                
                switch (action) {
                    case "increase":
                        newQuantity = currentQuantity + 1;
                        responseJsonObject.addProperty("message", "Quantity increased successfully");
                        System.out.println("Increasing quantity for movie: " + movieId + " from " + currentQuantity + " to " + newQuantity);
                        break;
                        
                    case "decrease":
                        if (currentQuantity > 1) {
                            newQuantity = currentQuantity - 1;
                            responseJsonObject.addProperty("message", "Quantity decreased successfully");
                            System.out.println("Decreasing quantity for movie: " + movieId + " from " + currentQuantity + " to " + newQuantity);
                        } else {
                            // Remove item if quantity would be 0
                            cartItems.remove(movieId);
                            responseJsonObject.addProperty("message", "Item removed from cart (quantity was 1)");
                            System.out.println("Removing movie from cart (quantity was 1): " + movieId);
                        }
                        break;
                        
                    default:
                        JsonObject errorResponse = new JsonObject();
                        errorResponse.addProperty("error", "Invalid action. Use 'increase' or 'decrease'");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        response.getWriter().write(errorResponse.toString());
                        return;
                }
                
                if (newQuantity > 0) {
                    movieItem.addProperty("quantity", newQuantity);
                }
            }
            
            // Save the updated cart items back to the session
            session.setAttribute("cartItems", cartItems);
            
            JsonArray cartItemsJsonArray = new JsonArray();
            for (JsonObject movieItem : cartItems.values()) {
                cartItemsJsonArray.add(movieItem);
            }
            responseJsonObject.add("cartItems", cartItemsJsonArray);
            
            // Calculate total price
            double totalPrice = 0.0;
            for (JsonObject movieItem : cartItems.values()) {
                totalPrice += movieItem.get("price").getAsDouble() * movieItem.get("quantity").getAsInt();
            }
            responseJsonObject.addProperty("totalPrice", totalPrice);
            
            response.getWriter().write(responseJsonObject.toString());
        } catch (Exception e) {
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Failed to update cart: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(errorResponse.toString());
        }
    }
    
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try {
            HttpSession session = request.getSession();
            
            @SuppressWarnings("unchecked")
            Map<String, JsonObject> cartItems = (Map<String, JsonObject>) session.getAttribute("cartItems");
            if (cartItems == null) {
                cartItems = new HashMap<>();
            }
            
            JsonObject responseJsonObject = new JsonObject();
            
            // Remove specific item
            String movieId = request.getParameter("movieId");
            if (movieId == null || movieId.trim().isEmpty()) {
                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("error", "Movie ID parameter is required");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(errorResponse.toString());
                return;
            }
            
            System.out.println("Removing movie from cart: " + movieId);
            
            boolean removed = false;
            synchronized (cartItems) {
                removed = cartItems.remove(movieId) != null;
            }
            
            if (removed) {
                responseJsonObject.addProperty("message", "Movie removed from cart successfully");
            } else {
                responseJsonObject.addProperty("message", "Movie not found in cart");
            }
            
            // Save the updated cart items back to the session
            session.setAttribute("cartItems", cartItems);
            
            JsonArray cartItemsJsonArray = new JsonArray();
            for (JsonObject movieItem : cartItems.values()) {
                cartItemsJsonArray.add(movieItem);
            }
            responseJsonObject.add("cartItems", cartItemsJsonArray);
            
            // Calculate total price
            double totalPrice = 0.0;
            for (JsonObject movieItem : cartItems.values()) {
                totalPrice += movieItem.get("price").getAsDouble() * movieItem.get("quantity").getAsInt();
            }
            responseJsonObject.addProperty("totalPrice", totalPrice);
            
            response.getWriter().write(responseJsonObject.toString());
        } catch (Exception e) {
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Failed to remove item from cart: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(errorResponse.toString());
        }
    }
}