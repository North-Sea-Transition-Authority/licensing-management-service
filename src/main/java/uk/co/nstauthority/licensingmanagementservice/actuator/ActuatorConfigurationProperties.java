package uk.co.nstauthority.licensingmanagementservice.actuator;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("actuator")
record ActuatorConfigurationProperties(@NotEmpty String apiKey) {}
