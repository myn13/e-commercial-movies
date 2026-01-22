import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;

import java.io.IOException;

@WebServlet(name = "LoginServlet", urlPatterns = {"/login", "/api/login"})
public class LoginServlet extends HttpServlet {
    
    protected void doPost(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) throws IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        JsonObject responseJson = new JsonObject();
        if (username.equals("anteater") && password.equals("88888888")) {
            request.getSession().setAttribute("user", username);
            responseJson.addProperty("status", "success");
            responseJson.addProperty("message", "success");
            System.out.println("User " + username + " logged in successfully");
        } else {
            responseJson.addProperty("status", "fail");
            request.getServletContext().log("Login failed for user " + username);
        }
        response.getWriter().write(responseJson.toString());
    }
}