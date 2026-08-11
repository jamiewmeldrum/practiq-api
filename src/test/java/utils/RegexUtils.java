package utils;

public class RegexUtils {

    public static final String ISO_8601_UTC = "\\d{4}-\\d{2}-\\d{2}T.*Z";

    private static final String UUID = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    public static String uuidWithExtension(String extension) {
        return UUID + "\\." + extension;
    }
}
