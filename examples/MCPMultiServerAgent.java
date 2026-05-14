package examples;

import com.strands.agent.Agent;
import com.strands.agent.AgentResult;
import com.strands.model.bedrock.BedrockModel;
import com.strands.tool.AgentTool;
import com.strands.tool.mcp.MCPClient;
import com.strands.tool.mcp.StdioMCPTransport;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Example: Connecting to multiple MCP servers with tool filtering and prefixing.
 *
 * Demonstrates:
 *   - Multiple MCP servers providing tools to a single agent
 *   - Tool name prefixing to avoid collisions
 *   - Allow/reject filters to control which tools are exposed
 *
 * Usage:
 *   ./gradlew runExample -Pexample=examples.MCPMultiServerAgent
 */
public class MCPMultiServerAgent {

    public static void main(String[] args) {
        String profile = System.getenv().getOrDefault("AWS_PROFILE", "default");

        BedrockRuntimeAsyncClient client = BedrockRuntimeAsyncClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(ProfileCredentialsProvider.create(profile))
                .build();

        // Server 1: Filesystem server with "fs_" prefix
        MCPClient fsServer = new MCPClient(
                new StdioMCPTransport(
                        List.of("npx", "-y", "@modelcontextprotocol/server-filesystem", "/tmp/workspace")
                ),
                30000,
                null,
                List.of(name -> name.contains("delete")),  // reject delete tools for safety
                "fs_"  // prefix all tools with "fs_"
        );

        // Server 2: Memory/knowledge server with "mem_" prefix
        MCPClient memServer = new MCPClient(
                new StdioMCPTransport(
                        List.of("npx", "-y", "@modelcontextprotocol/server-memory")
                ),
                30000,
                null,
                null,
                "mem_"  // prefix all tools with "mem_"
        );

        try {
            fsServer.start();
            memServer.start();

            // Combine tools from both servers
            List<AgentTool> allTools = new ArrayList<>();
            allTools.addAll(fsServer.loadTools());
            allTools.addAll(memServer.loadTools());

            System.out.println("Combined tools from 2 MCP servers:");
            for (AgentTool tool : allTools) {
                System.out.println("  - " + tool.getToolName());
            }
            System.out.println();

            Agent agent = Agent.builder()
                    .model(new BedrockModel(client, "us.anthropic.claude-sonnet-4-6"))
                    .systemPrompt("You are an assistant with access to a filesystem (fs_* tools) "
                            + "and a knowledge graph memory (mem_* tools). "
                            + "Use the filesystem tools to read/write files, "
                            + "and memory tools to store and retrieve knowledge.")
                    .tools(allTools.toArray(new AgentTool[0]))
                    .build();

            System.out.println("--- Task: Store knowledge then use filesystem ---");
            AgentResult result = agent.invoke(
                    "Remember that our project deadline is June 15, 2026. "
                    + "Then create a file at /tmp/workspace/notes.md summarizing what you stored in memory.");
            System.out.println(result);

        } finally {
            fsServer.close();
            memServer.close();
        }
    }
}
