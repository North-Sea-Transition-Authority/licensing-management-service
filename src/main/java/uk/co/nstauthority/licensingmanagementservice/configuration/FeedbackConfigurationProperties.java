package uk.co.nstauthority.licensingmanagementservice.configuration;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "fms")
@Validated
public record FeedbackConfigurationProperties(
    @NotNull String urlBase,
    @NotNull Long connectionTimeoutSeconds,
    @NotNull String saveFeedbackUrl,
    @NotNull String serviceName,
    @NotNull String presharedKey
) {}
