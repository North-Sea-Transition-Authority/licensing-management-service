package uk.co.nstauthority.licensingmanagementservice.configuration;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "saml")
@Validated
public record SamlProperties(@NotNull String registrationId,
                             @NotNull String entityId,
                             @NotNull String certificate,
                             @NotNull String loginUrl,
                             @NotNull String consumerServiceLocation) {
}
