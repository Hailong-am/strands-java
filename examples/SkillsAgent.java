package examples;

import com.strands.agent.Agent;
import com.strands.agent.AgentResult;
import com.strands.model.bedrock.BedrockModel;
import com.strands.plugin.skills.AgentSkillsPlugin;
import com.strands.plugin.skills.SkillSource;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;

import java.nio.file.Path;

public class SkillsAgent {

    public static void main(String[] args) {
        String profile = System.getenv().getOrDefault("AWS_PROFILE", "default");

        BedrockRuntimeAsyncClient client = BedrockRuntimeAsyncClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(ProfileCredentialsProvider.create(profile))
                .build();

        AgentSkillsPlugin skillsPlugin = new AgentSkillsPlugin(
                new SkillSource.Directory(Path.of("examples/skills"))
        );

        Agent agent = Agent.builder()
                .model(new BedrockModel(client, "us.anthropic.claude-sonnet-4-6"))
                .systemPrompt("You are a helpful assistant. Use the skills tool to load detailed instructions when needed.")
                .plugins(skillsPlugin)
                .build();

        System.out.println("Loaded skills: " + skillsPlugin.getSkills().keySet());
        System.out.println();

        AgentResult result = agent.invoke(
                "Please summarize this text: 'The Java programming language was developed by James Gosling "
                + "at Sun Microsystems in the mid-1990s. It was designed to be platform-independent, "
                + "following the write once run anywhere principle. Java quickly became one of the most "
                + "popular languages for enterprise software, web applications, and Android development. "
                + "Its garbage collection, strong typing, and vast ecosystem of libraries make it a "
                + "reliable choice for large-scale systems.'");

        System.out.println(result);
        System.out.println("\nActivated skills: " + skillsPlugin.getActivatedSkills());
    }
}
