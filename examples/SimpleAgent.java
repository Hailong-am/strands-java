package examples;

import com.strands.agent.Agent;
import com.strands.agent.AgentResult;
import com.strands.model.bedrock.BedrockModel;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;

public class SimpleAgent {

    public static void main(String[] args) {
        String profile = System.getenv().getOrDefault("AWS_PROFILE", "default");

        BedrockRuntimeAsyncClient client = BedrockRuntimeAsyncClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(ProfileCredentialsProvider.create(profile))
                .build();

        Agent agent = Agent.builder()
                .model(new BedrockModel(client, "us.anthropic.claude-sonnet-4-6"))
                .systemPrompt("You are a helpful assistant. Be concise.")
                .build();

        AgentResult result = agent.invoke("What are the 3 largest planets in our solar system?");
        System.out.println(result);
    }
}
