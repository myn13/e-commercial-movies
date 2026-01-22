package browse;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import com.google.gson.stream.JsonWriter;

@WebServlet(urlPatterns = {"/browse/title-initials"})
public class TitleInitialsServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        try (JsonWriter jsonWriter = new JsonWriter(resp.getWriter())) {
            jsonWriter.beginArray();
            jsonWriter.value("0-9");

            for (char character = 'A'; character <= 'Z'; character++)
                jsonWriter.value(String.valueOf(character));

            jsonWriter.endArray();

        }
    }
}
