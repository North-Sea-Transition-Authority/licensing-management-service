package uk.co.nstauthority.template.authentication.saml;

public enum EnergyPortalSamlAttribute {

  WEB_USER_ACCOUNT_ID("WUA_ID"),
  PERSON_ID("PERSON_ID"),
  FORENAME("FORENAME"),
  SURNAME("SURNAME"),
  EMAIL_ADDRESS("PRIMARY_EMAIL_ADDRESS"),
  PORTAL_PRIVILEGES("PRIVILEGES"),
  PROXY_USER_NAME("PROXY_USER_NAME"),
  PROXY_USER_WUA_ID("PROXY_USER_WUA_ID");

  private final String attributeName;

  EnergyPortalSamlAttribute(String attributeName) {
    this.attributeName = attributeName;
  }

  public String getAttributeName() {
    return attributeName;
  }
}
