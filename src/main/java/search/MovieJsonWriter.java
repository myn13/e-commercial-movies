package search;

import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import static utils.JsonUtils.writeIfNotNull;
import static utils.JsonUtils.writeJsonArrayOrEmpty;

public final class MovieJsonWriter {
    private MovieJsonWriter() {
    }

    public static void writeOne(
            JsonWriter jsonWriter,
            ResultSet resultSet) throws IOException, SQLException {
        jsonWriter.beginObject();

        writeIfNotNull(jsonWriter, "id", resultSet.getString("id"));
        writeIfNotNull(jsonWriter, "title", resultSet.getString("title_name"));
        writeIfNotNull(jsonWriter, "director", resultSet.getString("director"));
        writeIfNotNull(jsonWriter, "year", resultSet.getObject("year"));
        writeIfNotNull(jsonWriter, "rating", resultSet.getObject("rating"));
        writeIfNotNull(jsonWriter, "vote_count", resultSet.getObject("vote_count"));

        jsonWriter.name("stars");
        writeJsonArrayOrEmpty(jsonWriter, resultSet.getString("stars"));

        jsonWriter.name("genres");
        writeJsonArrayOrEmpty(jsonWriter, resultSet.getString("genres"));

        jsonWriter.endObject();
    }

    public static void writeArray(JsonWriter jw, ResultSet rs) throws IOException, SQLException {
        jw.beginArray();
        while (rs.next()) {
            writeOne(jw, rs);
        }
        jw.endArray();
    }

    public static boolean writeSingleIfPresent(JsonWriter jw, ResultSet rs) throws IOException, SQLException {
        if (!rs.next()) return false;
        writeOne(jw, rs);
        return true;
    }
}

