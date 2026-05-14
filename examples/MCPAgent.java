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

import java.util.List;

/**
 * Example: Using MCP (Model Context Protocol) servers as tool providers.
 *
 * This demonstrates connecting to an MCP server process via stdio transport,
 * discovering its tools, and making them available to the agent.
 *
 * Prerequisites:
 *   - An MCP server installed (e.g., @modelcontextprotocol/server-filesystem)
 *   - Node.js for npx-based servers, or any MCP-compatible executable
 *
 * Usage:
 *   ./gradlew runExample -Pexample=examples.MCPAgent
 */
public class MCPAgent {

    public static void main(String[] args) {
        String profile = System.getenv().getOrDefault("AWS_PROFILE", "default");

        BedrockRuntimeAsyncClient client = BedrockRuntimeAsyncClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(ProfileCredentialsProvider.create(profile))
                .build();

        // Connect to an MCP server via stdio transport.
        // This example uses the filesystem server that provides read/write/search tools.
        MCPClient mcpClient = new MCPClient(
                new StdioMCPTransport(
                        List.of("npx", "-y", "@modelcontextprotocol/server-filesystem", "/tmp/mcp-workspace")
                ),
                30000,  // startup timeout
                null,   // no allow filter (accept all tools)
                null,   // no reject filter
                null    // no prefix
        );

        try {
            // Start the MCP server and perform initialization handshake
            mcpClient.start();
            System.out.println("MCP server connected.");
            System.out.println("Server instructions: " + mcpClient.getServerInstructions());

            // Load tools discovered from the MCP server
            List<AgentTool> mcpTools = mcpClient.loadTools();
            System.out.println("Discovered " + mcpTools.size() + " tools:");
            for (AgentTool tool : mcpTools) {
                System.out.println("  - " + tool.getToolName() + ": " + tool.getToolSpec().getDescription());
            }
            System.out.println();

            // Create agent with MCP tools
            Agent agent = Agent.builder()
                    .model(new BedrockModel(client, "us.anthropic.claude-sonnet-4-6"))
                    .systemPrompt("You are a helpful file system assistant. "
                            + "Use the available tools to read, write, and search files.")
                    .tools(mcpTools.toArray(new AgentTool[0]))
                    .build();

            // Ask the agent to perform file operations
            System.out.println("--- Task: Create and read a file ---");
            AgentResult result = agent.invoke(
                    "Create a file called /tmp/mcp-workspace/hello.txt with the content "
                    + "'Hello from Strands Java SDK!' then read it back and confirm the contents.");
            System.out.println(result);
            System.out.println();

            // Demonstrate listing resources via MCP protocol
            System.out.println("--- MCP Resources ---");
            var resources = mcpClient.listResources();
            System.out.println("Available resources: " + resources);

            // Demonstrate listing prompts via MCP protocol
            System.out.println("\n--- MCP Prompts ---");
            var prompts = mcpClient.listPrompts();
            System.out.println("Available prompts: " + prompts);

        } finally {
            mcpClient.close();
            System.out.println("\nMCP server disconnected.");
        }
    }
}
