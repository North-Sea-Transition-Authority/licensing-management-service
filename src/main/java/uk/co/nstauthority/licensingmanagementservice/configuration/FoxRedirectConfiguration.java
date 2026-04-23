package uk.co.nstauthority.licensingmanagementservice.configuration;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "fox")
@Validated
public record FoxRedirectConfiguration(
    @NotNull String pearsRedirectUrl,
    @NotNull String epasRedirectUrl
) {
}
