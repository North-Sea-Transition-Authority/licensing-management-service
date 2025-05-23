package uk.co.nstauthority.licensingmanagementservice.energyportal.epa;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "energy-portal-api")
@Validated
public record EnergyPortalApiConfig(
    @NotNull String url,
    @NotNull String preSharedKey
) {}