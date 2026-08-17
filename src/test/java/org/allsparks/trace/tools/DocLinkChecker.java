package org.allsparks.trace.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Fails the build when Markdown relative links point at missing files. */
public final class DocLinkChecker {
    private static final Pattern LINK = Pattern.compile("\\[[^\\]]+\\]\\(([^)]+)\\)");

    private DocLinkChecker() {}

    public static List<String> missingLinks(Path root) throws IOException {
        List<String> missing = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(path -> path.toString().endsWith(".md"))
                    .filter(path -> {
                        String text = path.toString();
                        return !text.contains(".git")
                                && !text.contains(".gradle")
                                && !text.contains("build" + java.io.File.separator);
                    })
                    .forEach(markdown -> {
                        try {
                            String content = Files.readString(markdown);
                            Matcher matcher = LINK.matcher(content);
                            while (matcher.find()) {
                                String target = matcher.group(1);
                                if (target.startsWith("http://")
                                        || target.startsWith("https://")
                                        || target.startsWith("mailto:")) {
                                    continue;
                                }
                                String pathOnly = target.split("#", 2)[0].split("\\?", 2)[0];
                                if (pathOnly.isEmpty()) {
                                    continue;
                                }
                                Path resolved = markdown.getParent().resolve(pathOnly).normalize();
                                if (!Files.exists(resolved)) {
                                    missing.add(markdown + " -> " + pathOnly);
                                }
                            }
                        } catch (IOException exception) {
                            missing.add(markdown + " <unreadable>");
                        }
                    });
        }
        return missing;
    }
}
