package uk.co.nstauthority.licensingmanagementservice.authentication;

public class ServiceUserDetailTestUtil {

  private ServiceUserDetailTestUtil() {}

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private Builder() {}

    private Long wuaId = 1L;
    private Long personId = 2L;
    private String forename = "Forename";
    private String surname = "Surname";
    private String emailAddress = "test.user@test.com";
    private Long proxyWuaId = 3L;
    private String proxyUsername = "proxyUsername";

    public ServiceUserDetail build() {
      return new ServiceUserDetail(wuaId, personId, forename, surname, emailAddress, proxyWuaId, proxyUsername);
    }

    public ServiceUserDetail buildWithoutProxy() {
      return new ServiceUserDetail(wuaId, personId, forename, surname, emailAddress, null, null);
    }

    public Builder withWuaId(Long wuaId) {
      this.wuaId = wuaId;
      return this;
    }

    public Builder withPersonId(Long personId) {
      this.personId = personId;
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
      this.emailAddress = emailAddress;
      return this;
    }

    public Builder withProxyWuaId(Long proxyWuaId) {
      this.proxyWuaId = proxyWuaId;
      return this;
    }

    public Builder withProxyUserName(String proxyUserName) {
      this.proxyUsername = proxyUserName;
      return this;
    }
  }
}
