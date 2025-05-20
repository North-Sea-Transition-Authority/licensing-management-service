package uk.co.nstauthority.template.util;

import uk.co.fivium.energyportalapi.generated.types.User;
import uk.co.nstauthority.template.energyportal.user.EnergyPortalUserJson;

public class EnergyPortalUserTestUtil {

  public EnergyPortalUserTestUtil() {
    throw new IllegalStateException("Cannot instantiate static util class");
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private long webUserAccountId = 1L;
    private String title = "title";
    private String forename = "forename";
    private String surname = "surname";
    private String primaryEmailAddress = "email address";
    private String loginId = "loginId";
    private boolean canLogin = true;
    private String telephoneNumber = "0123456789";
    private boolean sharedAccount = false;
    private Builder() {}

    public Builder withWebUserAccountId(long webUserAccountId) {
      this.webUserAccountId = webUserAccountId;
      return this;
    }

    public Builder withTitle(String title) {
      this.title = title;
      return this;
    }

    public Builder withForename(String forename) {
      this.forename = forename;
      return this;
    }

    public Builder withSurname(String surname) {
      this.surname = surname;
      return this;
    }

    public Builder withEmailAddress(String emailAddress) {
      this.primaryEmailAddress = emailAddress;
      return this;
    }

    public Builder withLoginId(String loginId) {
      this.loginId = loginId;
      return this;
    }

    public Builder canLogin(boolean canLogin) {
      this.canLogin = canLogin;
      return this;
    }

    public String getTelephoneNumber() {
      return telephoneNumber;
    }

    public Builder setTelephoneNumber(String telephoneNumber) {
      this.telephoneNumber = telephoneNumber;
      return this;
    }

    public boolean isSharedAccount() {
      return sharedAccount;
    }

    public Builder setSharedAccount(boolean sharedAccount) {
      this.sharedAccount = sharedAccount;
      return this;
    }

    public User build() {
      return User.newBuilder()
          .webUserAccountId(webUserAccountId)
          .title(title)
          .forename(forename)
          .surname(surname)
          .primaryEmailAddress(primaryEmailAddress)
          .loginId(loginId)
          .canLogin(canLogin)
          .telephoneNumber(telephoneNumber)
          .isAccountShared(sharedAccount)
          .build();
    }

    public EnergyPortalUserJson buildJson() {
      return EnergyPortalUserJson.from(build());
    }
  }
}
