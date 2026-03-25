package kr.ac.knu.comit.global.docs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public class ApiDocGenerator {

    public static void main(String[] args) throws Exception {
        String basePackage = args.length > 0 ? args[0] : "kr.ac.knu.comit";
        Path outputDirectory = args.length > 1 ? Path.of(args[1]) : Path.of("docs/api");
        generate(basePackage, outputDirectory);
    }

    public static List<GeneratedApiDocument> generate(String basePackage, Path outputDirectory) throws Exception {
        ApiDocIntrospector introspector = new ApiDocIntrospector(basePackage);
        List<GeneratedApiDocument> documents = introspector.inspect();

        recreateDirectory(outputDirectory);
        write(outputDirectory.resolve("index.html"), ApiDocHtmlRenderer.renderIndexHtml());
        write(outputDirectory.resolve("index.js"), ApiDocHtmlRenderer.renderIndexScript(documents));

        for (GeneratedApiDocument document : documents) {
            Path outputPath = outputDirectory.resolve(document.relativeOutputPath());
            write(outputPath, ApiDocHtmlRenderer.renderDocument(document));
        }

        return documents;
    }

    private static void recreateDirectory(Path outputDirectory) throws IOException {
        if (Files.exists(outputDirectory)) {
            try (var paths = Files.walk(outputDirectory)) {
                paths.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException exception) {
                                throw new IllegalStateException("Failed to delete " + path, exception);
                            }
                        });
            }
        }
        Files.createDirectories(outputDirectory);
    }

    private static void write(Path path, String contents) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, contents, StandardCharsets.UTF_8);
    }
}
