package uk.co.nstauthority.licensingmanagementservice.configuration;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "energy-portal")
@Validated
public record EnergyPortalConfiguration(
    @NotNull String logoutUrl,
    @NotNull String logoutPreSharedKey,
    @NotNull String registrationUrl
) {
}
