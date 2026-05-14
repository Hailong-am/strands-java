package examples;

import com.strands.agent.Agent;
import com.strands.model.bedrock.BedrockModel;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;

import java.util.List;

public class StructuredOutputAgent {

    public static class BookRecommendation {
        public String title;
        public String author;
        public int year;
        public String genre;
        public String summary;
    }

    public static class BookList {
        public List<BookRecommendation> books;
        public String theme;
    }

    public static void main(String[] args) {
        String profile = System.getenv().getOrDefault("AWS_PROFILE", "default");

        BedrockRuntimeAsyncClient client = BedrockRuntimeAsyncClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(ProfileCredentialsProvider.create(profile))
                .build();

        Agent agent = Agent.builder()
                .model(new BedrockModel(client, "us.anthropic.claude-sonnet-4-6"))
                .systemPrompt("You are a book recommendation assistant.")
                .build();

        BookList result = agent.structuredOutput(BookList.class,
                "Recommend 3 science fiction books about AI");

        System.out.println("Theme: " + result.theme);
        System.out.println("Books:");
        for (BookRecommendation book : result.books) {
            System.out.printf("  - %s by %s (%d) [%s]%n", book.title, book.author, book.year, book.genre);
            System.out.println("    " + book.summary);
        }
    }
}
