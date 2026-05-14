package com.strands.plugin.skills;

import java.nio.file.Path;
import java.util.List;

public sealed interface SkillSource {

    record Direct(Skill skill) implements SkillSource {}

    record File(Path path) implements SkillSource {}

    record Directory(Path parentDir) implements SkillSource {}

    record Url(String url) implements SkillSource {}

    default List<Skill> resolve() {
        if (this instanceof Direct d) {
            return List.of(d.skill());
        } else if (this instanceof File f) {
            return List.of(Skill.fromFile(f.path()));
        } else if (this instanceof Directory d) {
            return Skill.fromDirectory(d.parentDir());
        } else if (this instanceof Url u) {
            return List.of(Skill.fromUrl(u.url()));
        }
        throw new IllegalStateException("Unknown SkillSource type");
    }
}
