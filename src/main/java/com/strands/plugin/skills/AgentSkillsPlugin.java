package com.strands.plugin.skills;

import com.strands.agent.Agent;
import com.strands.hook.HookRegistry;
import com.strands.hook.events.BeforeModelCallEvent;
import com.strands.plugin.Hook;
import com.strands.plugin.Plugin;
import com.strands.tool.Param;
import com.strands.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

public class AgentSkillsPlugin extends Plugin {

    private static final Logger log = LoggerFactory.getLogger(AgentSkillsPlugin.class);

    private final Map<String, Skill> skills = new LinkedHashMap<>();
    private final List<String> activatedSkills = new ArrayList<>();
    private String lastInjectedXml;

    public AgentSkillsPlugin(SkillSource... sources) {
        this(List.of(sources));
    }

    public AgentSkillsPlugin(List<SkillSource> sources) {
        for (SkillSource source : sources) {
            for (Skill skill : source.resolve()) {
                if (!skill.isValidName()) {
                    log.warn("Skill name '{}' does not match naming convention", skill.getName());
                }
                skills.put(skill.getName(), skill);
            }
        }
        log.debug("Loaded {} skills: {}", skills.size(), skills.keySet());
    }

    @Override
    public void initAgent(Agent agent) {
        String catalog = buildSkillsCatalogXml();
        if (!catalog.isEmpty()) {
            String existing = agent.getSystemPrompt() != null ? agent.getSystemPrompt() : "";
            agent.setSystemPrompt(existing + "\n\n" + catalog);
        }
        log.debug("AgentSkillsPlugin initialized with {} skills for agent {}", skills.size(), agent.getAgentId());
    }

    @Hook
    public void onBeforeModelCall(BeforeModelCallEvent event) {
        // Progressive disclosure: inject skill catalog into system prompt context
        // The model sees names/descriptions and can activate via the skills tool
    }

    @Tool(name = "skills", description = "Load the full instructions for a skill. "
            + "Call this when you need detailed guidance for a specific task. "
            + "Available skills are listed in the system prompt.")
    public String activateSkill(
            @Param(value = "skill_name", description = "Name of the skill to activate") String skillName) {

        Skill skill = skills.get(skillName);
        if (skill == null) {
            return "Unknown skill: '" + skillName + "'. Available: " + String.join(", ", skills.keySet());
        }

        if (!activatedSkills.contains(skillName)) {
            activatedSkills.add(skillName);
        }

        StringBuilder response = new StringBuilder();
        response.append(skill.getInstructions());

        if (!skill.getAllowedTools().isEmpty()) {
            response.append("\n\n---\nAllowed tools: ").append(String.join(", ", skill.getAllowedTools()));
        }

        if (skill.getPath() != null) {
            response.append("\nLocation: ").append(skill.getPath());

            List<Path> resources = skill.getResourceFiles();
            if (!resources.isEmpty()) {
                response.append("\n\nAvailable resources:");
                for (Path r : resources) {
                    response.append("\n  ").append(skill.getPath().relativize(r));
                }
            }
        }

        return response.toString();
    }

    public String buildSkillsCatalogXml() {
        if (skills.isEmpty()) return "";

        StringBuilder xml = new StringBuilder();
        xml.append("<available_skills>\n");
        for (Skill skill : skills.values()) {
            xml.append("<skill>\n");
            xml.append("  <name>").append(skill.getName()).append("</name>\n");
            xml.append("  <description>").append(skill.getDescription()).append("</description>\n");
            if (skill.getPath() != null) {
                xml.append("  <location>").append(skill.getPath()).append("</location>\n");
            }
            xml.append("</skill>\n");
        }
        xml.append("</available_skills>");
        lastInjectedXml = xml.toString();
        return lastInjectedXml;
    }

    public List<String> getActivatedSkills() {
        return Collections.unmodifiableList(activatedSkills);
    }

    public Map<String, Skill> getSkills() {
        return Collections.unmodifiableMap(skills);
    }

    @Override
    public void registerHooks(HookRegistry registry) {
        // @Hook annotation handles registration via PluginRegistry
    }
}
