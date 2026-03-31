package github.com.gengyoubo;

import github.com.gengyoubo.Type.ValueType;

import java.io.InputStream;
import java.util.Properties;

public class completionConfig {
    private static final Properties CONFIG = new Properties();

    static {
        try (InputStream is =
                     MixinAnnotationCompletionContributor.class
                             .getResourceAsStream("/mixin-completion.properties")) {

            if (is != null) {
                CONFIG.load(is);
            }

        } catch (Exception ignored) {
        }
    }
    static boolean isEnabled(ValueType type) {
        return Boolean.parseBoolean(
                CONFIG.getProperty(type.name(), "true")
        );
    }
}