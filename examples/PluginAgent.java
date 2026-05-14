package examples;

import com.strands.agent.Agent;
import com.strands.agent.AgentResult;
import com.strands.hook.HookRegistry;
import com.strands.hook.events.AfterToolCallEvent;
import com.strands.hook.events.BeforeToolCallEvent;
import com.strands.model.bedrock.BedrockModel;
import com.strands.plugin.Hook;
import com.strands.plugin.Plugin;
import com.strands.tool.Param;
import com.strands.tool.Tool;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;

/**
 * Demonstrates the plugin system: a plugin that provides both tools and hooks.
 */
public class PluginAgent {

    public static class TimerPlugin extends Plugin {

        private long startTime;

        @Hook
        public void onBeforeTool(BeforeToolCallEvent event) {
            startTime = System.currentTimeMillis();
            System.out.println("  [Plugin] Calling tool: " + event.getToolName());
        }

        @Hook
        public void onAfterTool(AfterToolCallEvent event) {
            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("  [Plugin] Tool " + event.getToolName() + " completed in " + elapsed + "ms");
        }

        @Tool(name = "get_time", description = "Returns the current time")
        public String getTime() {
            return java.time.LocalDateTime.now().toString();
        }

        @Tool(name = "reverse_text", description = "Reverses the given text")
        public String reverseText(@Param(value = "text", description = "Text to reverse") String text) {
            return new StringBuilder(text).reverse().toString();
        }
    }

    public static void main(String[] args) {
        String profile = System.getenv().getOrDefault("AWS_PROFILE", "default");

        BedrockRuntimeAsyncClient client = BedrockRuntimeAsyncClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(ProfileCredentialsProvider.create(profile))
                .build();

        Agent agent = Agent.builder()
                .model(new BedrockModel(client, "us.anthropic.claude-sonnet-4-6"))
                .systemPrompt("You are a helpful assistant. Use the available tools.")
                .plugins(new TimerPlugin())
                .build();

        System.out.println("--- Ask for current time ---");
        AgentResult result1 = agent.invoke("What time is it right now?");
        System.out.println(result1);

        System.out.println("\n--- Ask to reverse text ---");
        AgentResult result2 = agent.invoke("Reverse the text 'Hello World'");
        System.out.println(result2);
    }
}
