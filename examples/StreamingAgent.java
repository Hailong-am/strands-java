package examples;

import com.strands.agent.Agent;
import com.strands.model.StreamHandler;
import com.strands.model.bedrock.BedrockModel;
import com.strands.types.Message;
import com.strands.types.StopReason;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;

public class StreamingAgent {

    public static void main(String[] args) {
        String profile = System.getenv().getOrDefault("AWS_PROFILE", "default");

        BedrockRuntimeAsyncClient client = BedrockRuntimeAsyncClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(ProfileCredentialsProvider.create(profile))
                .build();

        Agent agent = Agent.builder()
                .model(new BedrockModel(client, "us.anthropic.claude-sonnet-4-6"))
                .systemPrompt("You are a helpful assistant. Respond in 2-3 sentences.")
                .build();

        StreamHandler handler = new StreamHandler() {
            @Override
            public void onTextDelta(String delta) {
                System.out.print(delta);
            }

            @Override
            public void onComplete(Message message, StopReason stopReason) {
                System.out.println("\n\n[Done. Stop reason: " + stopReason + "]");
            }
        };

        agent.invoke("Explain quantum entanglement in simple terms.", handler);
    }
}
