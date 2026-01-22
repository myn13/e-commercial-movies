import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Disabled: Login filter is currently disabled
// @WebFilter(filterName = "LoginFilter", urlPatterns = {"/*"})
public class LoginFilter implements Filter {
    private final List<String> allowedURIs = new ArrayList<>(); //user can access these URIs without login

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        System.out.println("LoginFilter: " + httpRequest.getRequestURI());

        // Allow all requests to pass through (login disabled)
        chain.doFilter(request, response);

//        if (this.isUrlAllowedWithoutLogin(httpRequest.getRequestURI())) {
//            chain.doFilter(request, response);
//            return;
//        }
//        if (httpRequest.getSession().getAttribute("user") == null) {
//            httpResponse.sendRedirect("/MT_movie_project_war/");
//        } else {
//            chain.doFilter(request, response);
//        }
    }
    private boolean isUrlAllowedWithoutLogin(String uri) {
        String lowerUri = uri.toLowerCase();

        // Special handling for login endpoints
        if (lowerUri.endsWith("/api/login") || lowerUri.contains("/api/login?") ||
            lowerUri.endsWith("/login") || lowerUri.contains("/login?")) {
            return true;
        }

        // Block /api/ paths that are not /api/login
        if (lowerUri.contains("/api/") && !lowerUri.contains("/api/login")) {
            return false;
        }

        // Check if URI starts with any allowed prefix
        boolean allowed = allowedURIs.stream().anyMatch(lowerUri::startsWith);

        // If allowed by prefix, return true
        if (allowed) {
            return true;
        }

        // Check if URI ends with any allowed suffix (for file extensions)
        return allowedURIs.stream().anyMatch(lowerUri::endsWith);
    }
    public void init(FilterConfig filterConfig) throws ServletException {
        allowedURIs.add("/login");
        allowedURIs.add("/api/login");
        allowedURIs.add("/login.js");
        allowedURIs.add("/");
        allowedURIs.add("/index.html");
        allowedURIs.add("/src/");
        allowedURIs.add("/assets/");
        allowedURIs.add("/dist/");
        allowedURIs.add(".css");
        allowedURIs.add(".js");
        allowedURIs.add(".svg");
        allowedURIs.add(".tsx");
        allowedURIs.add(".ts");
        allowedURIs.add(".json");
        allowedURIs.add(".ico");
        allowedURIs.add(".png");
        allowedURIs.add(".jpg");
        allowedURIs.add(".jpeg");
        allowedURIs.add(".gif");
        allowedURIs.add(".woff");
        allowedURIs.add(".woff2");
        allowedURIs.add(".ttf");
        allowedURIs.add(".eot");
        //could add more allowed URI here
    }

    public void destroy() {
        // Cleanup code if needed
    }
}
