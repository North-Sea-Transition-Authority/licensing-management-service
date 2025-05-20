package uk.co.nstauthority.template.branding;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "branding.customer")
@Validated
public record CustomerConfigurationProperties(
    @NotNull String name,
    @NotNull String mnemonic
) {}
