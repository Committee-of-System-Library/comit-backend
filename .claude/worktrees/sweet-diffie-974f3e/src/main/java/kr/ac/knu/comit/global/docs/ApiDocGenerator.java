package kr.ac.knu.comit.global.docs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class ApiDocGenerator {

    private static final Set<String> EXCLUDED_SPEC_FILES = Set.of("README.md", "feature-spec-template.md");

    public static void main(String[] args) throws Exception {
        String basePackage = args.length > 0 ? args[0] : "kr.ac.knu.comit";
        Path outputDirectory = args.length > 1 ? Path.of(args[1]) : Path.of("docs/api");
        Path featuresDirectory = args.length > 2 ? Path.of(args[2]) : Path.of("docs/features");
        generate(basePackage, outputDirectory, featuresDirectory);
    }

    public static List<GeneratedApiDocument> generate(String basePackage, Path outputDirectory) throws Exception {
        return generate(basePackage, outputDirectory, Path.of("docs/features"));
    }

    public static List<GeneratedApiDocument> generate(String basePackage, Path outputDirectory, Path featuresDirectory) throws Exception {
        ApiDocIntrospector introspector = new ApiDocIntrospector(basePackage);
        List<GeneratedApiDocument> documents = introspector.inspect();
        List<FeatureSpecHtmlRenderer.SpecInfo> specs = generateFeatureSpecs(featuresDirectory, outputDirectory);

        recreateDirectory(outputDirectory);
        write(outputDirectory.resolve("index.html"), ApiDocHtmlRenderer.renderIndexHtml());
        write(outputDirectory.resolve("index.js"), ApiDocHtmlRenderer.renderIndexScript(documents));
        write(outputDirectory.resolve("spec-index.js"), ApiDocHtmlRenderer.renderSpecIndexScript(specs));

        for (GeneratedApiDocument document : documents) {
            Path outputPath = outputDirectory.resolve(document.relativeOutputPath());
            write(outputPath, ApiDocHtmlRenderer.renderDocument(document));
        }

        for (FeatureSpecHtmlRenderer.SpecInfo spec : specs) {
            Path outputPath = outputDirectory.resolve(spec.href());
            String markdown = Files.readString(featuresDirectory.resolve(
                    spec.href().replace("spec/", "").replace(".html", ".md")), StandardCharsets.UTF_8);
            write(outputPath, FeatureSpecHtmlRenderer.renderSpecPage(markdown));
        }

        return documents;
    }

    private static List<FeatureSpecHtmlRenderer.SpecInfo> generateFeatureSpecs(Path featuresDirectory, Path outputDirectory) throws IOException {
        List<FeatureSpecHtmlRenderer.SpecInfo> specs = new ArrayList<>();
        if (!Files.exists(featuresDirectory)) return specs;

        try (var stream = Files.list(featuresDirectory)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".md"))
                    .filter(p -> !EXCLUDED_SPEC_FILES.contains(p.getFileName().toString()))
                    .sorted()
                    .forEach(p -> {
                        try {
                            String markdown = Files.readString(p, StandardCharsets.UTF_8);
                            specs.add(FeatureSpecHtmlRenderer.buildSpecInfo(p.getFileName().toString(), markdown));
                        } catch (IOException e) {
                            throw new IllegalStateException("Failed to read " + p, e);
                        }
                    });
        }
        return specs;
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
