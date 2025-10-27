package uk.co.nstauthority.licensingmanagementservice.licence;

public class LicenceTestUtil {

  private LicenceTestUtil() {}

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private Integer id;
    private LicenceType licenceType;
    private String licenceNumber;
    private String licencePrefix;
    private String licenceReference;
    private String roundIssuedOn;
    private LicenceStatus status;

    private Builder() {}

    public Builder withId(Integer id) {
      this.id = id;
      return this;
    }

    public Builder withLicenceType(LicenceType licenceType) {
      this.licenceType = licenceType;
      return this;
    }

    public Builder withLicenceNumber(String licenceNumber) {
      this.licenceNumber = licenceNumber;
      return this;
    }

    public Builder withLicencePrefix(String licencePrefix) {
      this.licencePrefix = licencePrefix;
      return this;
    }

    public Builder withLicenceReference(String licenceReference) {
      this.licenceReference = licenceReference;
      return this;
    }

    public Builder withRoundIssuedOn(String roundIssuedOn) {
      this.roundIssuedOn = roundIssuedOn;
      return this;
    }

    public Builder withStatus(LicenceStatus status) {
      this.status = status;
      return this;
    }

    public Licence build() {
      var licence = new Licence();
      licence.setId(id);
      licence.setType(licenceType);
      licence.setLicenceNumber(licenceNumber);
      licence.setPrefix(licencePrefix);
      licence.setLicenceReference(licenceReference);
      licence.setRoundIssuedOn(roundIssuedOn);
      licence.setStatus(status);

      return licence;
    }
  }
}
