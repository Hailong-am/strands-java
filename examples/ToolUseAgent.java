package examples;

import com.strands.agent.Agent;
import com.strands.agent.AgentResult;
import com.strands.model.bedrock.BedrockModel;
import com.strands.tool.Param;
import com.strands.tool.Tool;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;

public class ToolUseAgent {

    @Tool(name = "calculator", description = "Performs arithmetic. Supports +, -, *, /")
    public String calculate(
            @Param(value = "expression", description = "Math expression like '2 + 3' or '10 / 2'") String expression) {
        try {
            String[] parts = expression.trim().split("\\s+");
            double a = Double.parseDouble(parts[0]);
            String op = parts[1];
            double b = Double.parseDouble(parts[2]);
            double result = switch (op) {
                case "+" -> a + b;
                case "-" -> a - b;
                case "*" -> a * b;
                case "/" -> a / b;
                default -> throw new IllegalArgumentException("Unknown op: " + op);
            };
            return String.valueOf(result);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(name = "weather", description = "Gets current weather for a city (mock)")
    public String getWeather(@Param(value = "city", description = "City name") String city) {
        return switch (city.toLowerCase()) {
            case "seattle" -> "Seattle: 58°F, cloudy";
            case "new york" -> "New York: 72°F, sunny";
            case "london" -> "London: 55°F, rainy";
            default -> city + ": 65°F, partly cloudy";
        };
    }

    public static void main(String[] args) {
        String profile = System.getenv().getOrDefault("AWS_PROFILE", "default");

        BedrockRuntimeAsyncClient client = BedrockRuntimeAsyncClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(ProfileCredentialsProvider.create(profile))
                .build();

        ToolUseAgent tools = new ToolUseAgent();

        Agent agent = Agent.builder()
                .model(new BedrockModel(client, "us.anthropic.claude-sonnet-4-6"))
                .systemPrompt("You are a helpful assistant with access to tools. Use them when appropriate.")
                .toolProviders(tools)
                .build();

        System.out.println("--- Question 1: Math ---");
        AgentResult result1 = agent.invoke("What is 42 * 17 + 3?");
        System.out.println(result1);

        System.out.println("\n--- Question 2: Weather ---");
        AgentResult result2 = agent.invoke("What's the weather in Seattle and New York?");
        System.out.println(result2);
    }
}
