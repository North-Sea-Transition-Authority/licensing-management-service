package uk.co.nstauthority.template.energyportal.access;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "energy-portal.access-api")
@Validated
record EnergyPortalAccessApiConfiguration(
    @NotNull String baseUrl,
    @NotNull String token
) {
}
