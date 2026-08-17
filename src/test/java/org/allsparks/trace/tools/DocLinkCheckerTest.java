package org.allsparks.trace.tools;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class DocLinkCheckerTest {
    @Test
    void repositoryMarkdownLinksResolve() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        List<String> missing = DocLinkChecker.missingLinks(root);
        assertTrue(missing.isEmpty(), "Broken relative Markdown links:\n" + String.join("\n", missing));
    }
}
