package com.runbook_agent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeClient;

@Configuration
@Profile("bedrock")
public class AwsConfig {

    @Value("${aws.region}")
    private String region;

    @Bean
    public BedrockAgentRuntimeClient bedrockAgentRuntimeClient() {
        // DefaultCredentialsProvider: ~/.aws/credentials → 환경변수 → IAM 역할 순으로 자동 탐색
        return BedrockAgentRuntimeClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
