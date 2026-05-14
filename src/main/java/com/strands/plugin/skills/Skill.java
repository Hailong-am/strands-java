package com.strands.plugin.skills;

import lombok.Builder;
import lombok.Getter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Getter
@Builder
public class Skill {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-z0-9]([a-z0-9-]*[a-z0-9])?$");
    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile("^---\\s*\\n(.*?)\\n---\\s*\\n(.*)$", Pattern.DOTALL);

    private final String name;
    private final String description;
    @Builder.Default
    private final String instructions = "";
    private final Path path;
    @Builder.Default
    private final List<String> allowedTools = List.of();
    @Builder.Default
    private final Map<String, Object> metadata = Map.of();

    public static Skill fromFile(Path path) {
        Path skillFile = Files.isDirectory(path) ? path.resolve("SKILL.md") : path;
        Path skillDir = skillFile.getParent();

        try {
            String content = Files.readString(skillFile);
            Skill skill = fromContent(content);
            return Skill.builder()
                    .name(skill.name)
                    .description(skill.description)
                    .instructions(skill.instructions)
                    .path(skillDir)
                    .allowedTools(skill.allowedTools)
                    .metadata(skill.metadata)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read skill file: " + skillFile, e);
        }
    }

    public static Skill fromContent(String content) {
        Matcher matcher = FRONTMATTER_PATTERN.matcher(content);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid SKILL.md format: missing YAML frontmatter");
        }

        String yaml = matcher.group(1);
        String body = matcher.group(2).trim();

        Map<String, String> frontmatter = parseSimpleYaml(yaml);

        String name = frontmatter.get("name");
        String description = frontmatter.get("description");
        if (name == null || description == null) {
            throw new IllegalArgumentException("SKILL.md frontmatter must contain 'name' and 'description'");
        }

        List<String> allowedTools = List.of();
        String toolsStr = frontmatter.get("allowed-tools");
        if (toolsStr != null && !toolsStr.isBlank()) {
            allowedTools = List.of(toolsStr.trim().split("\\s+"));
        }

        return Skill.builder()
                .name(name)
                .description(description)
                .instructions(body)
                .allowedTools(allowedTools)
                .metadata(Map.of())
                .build();
    }

    public static List<Skill> fromDirectory(Path parentDir) {
        List<Skill> skills = new ArrayList<>();
        try (Stream<Path> dirs = Files.list(parentDir)) {
            dirs.filter(Files::isDirectory)
                    .filter(d -> Files.exists(d.resolve("SKILL.md")))
                    .forEach(d -> skills.add(fromFile(d)));
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan skills directory: " + parentDir, e);
        }
        return skills;
    }

    public static Skill fromUrl(String url) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new RuntimeException("Failed to fetch skill from " + url + ": HTTP " + response.statusCode());
            }
            return fromContent(response.body());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to fetch skill from URL: " + url, e);
        }
    }

    public boolean isValidName() {
        return name != null && NAME_PATTERN.matcher(name).matches() && name.length() <= 64;
    }

    public List<Path> getResourceFiles() {
        if (path == null) return List.of();
        List<Path> resources = new ArrayList<>();
        String[] resourceDirs = {"scripts", "references", "assets"};
        for (String dir : resourceDirs) {
            Path resourceDir = path.resolve(dir);
            if (Files.isDirectory(resourceDir)) {
                try (Stream<Path> files = Files.walk(resourceDir)) {
                    files.filter(Files::isRegularFile)
                            .limit(20)
                            .forEach(resources::add);
                } catch (IOException ignored) {
                }
            }
        }
        return resources;
    }

    private static Map<String, String> parseSimpleYaml(String yaml) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String line : yaml.split("\\n")) {
            int colonIdx = line.indexOf(':');
            if (colonIdx > 0) {
                String key = line.substring(0, colonIdx).trim();
                String value = line.substring(colonIdx + 1).trim();
                result.put(key, value);
            }
        }
        return result;
    }
}
