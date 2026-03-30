package uk.co.nstauthority.licensingmanagementservice.document.signing;

import org.springframework.boot.context.properties.ConfigurationProperties;
import uk.co.fivium.ftss.client.FtssSignerProperties;

@ConfigurationProperties("digital-signature")
public record DigitalSignatureProperties(
    String commonName,
    String organisation,
    String email,
    String state,
    String country,
    String locality,
    String reason,
    String location,
    String line1,
    String line3
) {

  public FtssSignerProperties asFtssSignerProperties() {
    return new FtssSignerProperties(
        commonName,
        organisation,
        email,
        state,
        country,
        locality,
        reason,
        location
    );
  }

}