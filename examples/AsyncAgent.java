package examples;

import com.strands.agent.Agent;
import com.strands.agent.AgentResult;
import com.strands.agent.ConcurrentInvocationMode;
import com.strands.model.bedrock.BedrockModel;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;

import java.util.concurrent.CompletableFuture;

public class AsyncAgent {

    public static void main(String[] args) throws Exception {
        String profile = System.getenv().getOrDefault("AWS_PROFILE", "default");

        BedrockRuntimeAsyncClient client = BedrockRuntimeAsyncClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(ProfileCredentialsProvider.create(profile))
                .build();

        Agent agent1 = Agent.builder()
                .model(new BedrockModel(client, "us.anthropic.claude-sonnet-4-6"))
                .systemPrompt("You are a history expert. Answer in one sentence.")
                .concurrentInvocationMode(ConcurrentInvocationMode.UNSAFE_REENTRANT)
                .build();

        Agent agent2 = Agent.builder()
                .model(new BedrockModel(client, "us.anthropic.claude-sonnet-4-6"))
                .systemPrompt("You are a science expert. Answer in one sentence.")
                .build();

        long start = System.currentTimeMillis();

        CompletableFuture<AgentResult> future1 = agent1.invokeAsync("Who built the pyramids?");
        CompletableFuture<AgentResult> future2 = agent2.invokeAsync("What is photosynthesis?");

        CompletableFuture.allOf(future1, future2).join();

        long elapsed = System.currentTimeMillis() - start;

        System.out.println("History: " + future1.get());
        System.out.println("Science: " + future2.get());
        System.out.println("\nBoth completed in " + elapsed + "ms (parallel)");
    }
}
