package github.com.gengyoubo.ASM;

import java.util.LinkedHashSet;
import java.util.Set;

public class AsmTargetInfo {
    private final Set<String> invokes = new LinkedHashSet<>();
    private final Set<String> fields = new LinkedHashSet<>();
    private final Set<String> news = new LinkedHashSet<>();

    public Set<String> getInvokes() {
        return invokes;
    }

    public Set<String> getFields() {
        return fields;
    }

    public Set<String> getNews() {
        return news;
    }
}
