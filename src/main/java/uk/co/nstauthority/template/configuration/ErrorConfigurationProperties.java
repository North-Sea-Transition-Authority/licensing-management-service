package uk.co.nstauthority.template.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("service.error")
public record ErrorConfigurationProperties(
    boolean includeStacktrace
) {
}