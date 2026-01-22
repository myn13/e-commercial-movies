package utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

import com.google.gson.JsonArray;

public final class JsonUtils {

    private JsonUtils() {
    }

    public static void writeIfNotNull(JsonWriter jw, String name, Object value) throws IOException {
        if (value == null) return;
        jw.name(name);
        if (value instanceof Number n) {
            jw.value(n);
        } else {
            jw.value(value.toString());
        }
    }

    public static void writeJsonArrayOrEmpty(JsonWriter jw, String json) throws IOException {
        JsonElement element = (json == null || json.isBlank())
                ? new JsonArray()
                : JsonParser.parseString(json);
        Streams.write(element, jw);
    }

    public static int parseIntOrDefault(String s, int def, int min, int max) {
        try {
            int v = Integer.parseInt(s);
            if (v < min) return min;
            if (v > max) return max;
            return v;
        } catch (Exception e) {
            return def;
        }
    }

    public static Integer parseNullableInt(String s) {
        try {
            return (s == null || s.isBlank()) ? null : Integer.valueOf(s);
        } catch (Exception e) {
            return null;
        }
    }
}
