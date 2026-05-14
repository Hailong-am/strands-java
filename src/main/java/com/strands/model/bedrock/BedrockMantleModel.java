package com.strands.model.bedrock;

import com.strands.model.Model;
import com.strands.model.ModelConfig;
import com.strands.model.StreamRequest;
import com.strands.model.openai.OpenAIModel;
import com.strands.types.streaming.StreamEvent;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import java.util.Iterator;
import java.util.Map;

/**
 * Bedrock Mantle model. Routes requests through Bedrock's OpenAI-compatible
 * endpoint, allowing use of the OpenAI message format while leveraging
 * Bedrock's model catalog and authentication.
 */
public class BedrockMantleModel implements Model {

    private final OpenAIModel delegate;
    private final String region;

    public BedrockMantleModel(String modelId) {
        this(modelId, "us-east-1");
    }

    public BedrockMantleModel(String modelId, String region) {
        this.region = region;
        String baseUrl = "https://bedrock-runtime." + region + ".amazonaws.com/model/"
                + modelId + "/converse-stream";
        this.delegate = new OpenAIModel(getBedrockToken(), baseUrl, modelId);
    }

    @Override
    public Iterator<StreamEvent> stream(StreamRequest request) {
        return delegate.stream(request);
    }

    @Override
    public ModelConfig getConfig() {
        return delegate.getConfig();
    }

    @Override
    public void updateConfig(Map<String, Object> configUpdates) {
        delegate.updateConfig(configUpdates);
    }

    private String getBedrockToken() {
        try {
            var credentials = DefaultCredentialsProvider.create().resolveCredentials();
            return credentials.accessKeyId();
        } catch (Exception e) {
            return "bedrock-auth";
        }
    }
}
