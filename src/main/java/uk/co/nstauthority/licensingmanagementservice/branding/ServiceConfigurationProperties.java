package uk.co.nstauthority.licensingmanagementservice.branding;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "branding.service")
@Validated
public record ServiceConfigurationProperties(
    @NotNull String name,
    @NotNull String mnemonic,
    @NotNull SupportContact supportContact,
    @NotNull String baseUrl
) {
  public record SupportContact(
      @NotNull String email,
      @NotNull String phone
  ) {
  }
}
