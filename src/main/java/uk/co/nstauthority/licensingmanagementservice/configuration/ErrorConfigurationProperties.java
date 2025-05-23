package uk.co.nstauthority.licensingmanagementservice.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("service.error")
public record ErrorConfigurationProperties(
    boolean includeStacktrace
) {
}