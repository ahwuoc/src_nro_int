package nro.utils;

import com.github.slugify.Slugify;

/**
 * Utility class for string operations
 */
public class StringUtil {

    /**
     * Convert Vietnamese string to slug format (remove diacritics, replace special chars with underscore)
     * Example: "Áo vải 3 lỗ" -> "AO_VAI_3_LO"
     * @param input input string
     * @return slugified string in uppercase
     */
    public static String slugify(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        // Use Slugify library with builder pattern to handle Vietnamese diacritics
        Slugify slugify = Slugify.builder().build();
        
        String slug = slugify.slugify(input);
        
        // Replace hyphens with underscores and convert to uppercase
        return slug
            .replace("-", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_|_$", "")
            .toUpperCase();
    }
}
