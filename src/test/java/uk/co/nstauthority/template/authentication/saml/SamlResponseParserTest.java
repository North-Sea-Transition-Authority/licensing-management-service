package uk.co.nstauthority.template.authentication.saml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.impl.XSAnyBuilder;
import org.opensaml.saml.saml1.core.AttributeValue;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.impl.AssertionBuilder;
import org.opensaml.saml.saml2.core.impl.AttributeBuilder;
import org.opensaml.saml.saml2.core.impl.AttributeStatementBuilder;
import org.opensaml.saml.saml2.core.impl.ResponseBuilder;
import org.springframework.security.core.GrantedAuthority;
import uk.co.nstauthority.template.authentication.ServiceUserDetail;

class SamlResponseParserTest {

  private SamlResponseParser samlResponseParser;

  @BeforeEach
  void setUp() {
    samlResponseParser = new SamlResponseParser();
  }

  @Test
  void parseSamlResponse() {
    var samlResponse = samlResponseBuilder()
      .withWebUserAccountId("1")
      .withPersonId("2")
      .withForename("Forename")
      .withSurname("Surname")
      .withEmailAddress("email@address.com")
      .withPortalPrivileges("PRIV_ONE,PRIV_TWO,PRIV_THREE")
      .build();

    var authentication = samlResponseParser.parseSamlResponse(samlResponse);
    var userDetails = (ServiceUserDetail) authentication.getPrincipal();

    assertThat(userDetails.wuaId()).isEqualTo(1L);
    assertThat(userDetails.personId()).isEqualTo(2L);
    assertThat(userDetails.forename()).isEqualTo("Forename");
    assertThat(userDetails.surname()).isEqualTo("Surname");
    assertThat(userDetails.emailAddress()).isEqualTo("email@address.com");
    assertThat(authentication.getAuthorities())
      .extracting(GrantedAuthority::getAuthority)
      .containsExactly(
        "PRIV_ONE",
        "PRIV_TWO",
        "PRIV_THREE"
      );
  }

  @Test
  void parseSamlResponse_withProxy() {
    var samlResponse = samlResponseBuilder()
        .withWebUserAccountId("1")
        .withPersonId("2")
        .withForename("Forename")
        .withSurname("Surname")
        .withEmailAddress("email@address.com")
        .withPortalPrivileges("PRIV_ONE,PRIV_TWO,PRIV_THREE")
        .withProxyWuaId("999")
        .withProxyUserName("proxy")
        .build();

    var authentication = samlResponseParser.parseSamlResponse(samlResponse);
    var userDetails = (ServiceUserDetail) authentication.getPrincipal();

    assertThat(userDetails.wuaId()).isEqualTo(1L);
    assertThat(userDetails.personId()).isEqualTo(2L);
    assertThat(userDetails.forename()).isEqualTo("Forename");
    assertThat(userDetails.surname()).isEqualTo("Surname");
    assertThat(userDetails.emailAddress()).isEqualTo("email@address.com");
    assertThat(userDetails.proxyWuaId()).isEqualTo(999L);
    assertThat(userDetails.proxyUsername()).isEqualTo("proxy");
    assertThat(authentication.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly(
            "PRIV_ONE",
            "PRIV_TWO",
            "PRIV_THREE"
        );
  }

  @Test
  void parseSamlResponse_missingAttributes() {
    var samlResponse = samlResponseBuilder().buildNoAttrs();
    assertThatExceptionOfType(NullPointerException.class)
      .isThrownBy(() -> samlResponseParser.parseSamlResponse(samlResponse));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void parseSamlResponse_whenWebUserAccountIdAttributeEmpty_thenException(String webUserAccountId) {

    var samlResponse = samlResponseBuilder()
      .withWebUserAccountId(webUserAccountId)
      .build();

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> samlResponseParser.parseSamlResponse(samlResponse));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void parseSamlResponse_whenPersonIdAttributeEmpty_thenException(String personId) {

    var samlResponse = samlResponseBuilder()
      .withPersonId(personId)
      .build();

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> samlResponseParser.parseSamlResponse(samlResponse));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void parseSamlResponse_whenForenameAttributeEmpty_thenException(String forename) {

    var samlResponse = samlResponseBuilder()
      .withForename(forename)
      .build();

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> samlResponseParser.parseSamlResponse(samlResponse));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void parseSamlResponse_whenSurnameAttributeEmpty_thenException(String surname) {

    var samlResponse = samlResponseBuilder()
      .withSurname(surname)
      .build();

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> samlResponseParser.parseSamlResponse(samlResponse));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void parseSamlResponse_whenEmailAttributeEmpty_thenException(String emailAddress) {

    var samlResponse = samlResponseBuilder()
      .withEmailAddress(emailAddress)
      .build();

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> samlResponseParser.parseSamlResponse(samlResponse));
  }

  @Test
  void parseSamlResponse_whenPrivilegesEmpty_thenAuthoritiesEmpty() {

    var samlResponse = samlResponseBuilder()
      .withPortalPrivileges("")
      .build();

    assertThat(samlResponseParser.parseSamlResponse(samlResponse).getAuthorities()).isEmpty();
  }

  @Test
  void parseSamlResponse_whenPrivilegesNull_thenException() {

    var samlResponse = samlResponseBuilder()
      .withEmailAddress(null)
      .build();

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> samlResponseParser.parseSamlResponse(samlResponse));
  }

  @Test
  void parseSamlResponse_whenNotAllProxyAttrsSet_thenException() {
    var samlResponseMissingProxyWuaId = samlResponseBuilder()
        .withProxyUserName("proxy")
        .build();

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> samlResponseParser.parseSamlResponse(samlResponseMissingProxyWuaId));

    var samlResponseMissingProxyName = samlResponseBuilder()
        .withProxyWuaId("999")
        .build();

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> samlResponseParser.parseSamlResponse(samlResponseMissingProxyName));
  }

  static SamlResposneBuilder samlResponseBuilder() {
    return new SamlResposneBuilder();
  }

  static class SamlResposneBuilder {

    private String webUserAccountId = "1";
    private String personId = "2";
    private String forename = "Forename";
    private String surname = "Surname";
    private String emailAddress = "email@address.com";
    private String portalPrivilegeCsv = "PRIVILEGE_1";
    private String proxyWuaId = null;
    private String proxyUserName = null;

    private final List<Attribute> attributes = new ArrayList<>();

    SamlResposneBuilder withWebUserAccountId(String webUserAccountId) {
      this.webUserAccountId = webUserAccountId;
      return this;
    }

    SamlResposneBuilder withPersonId(String personId) {
      this.personId = personId;
      return this;
    }

    SamlResposneBuilder withForename(String forename) {
      this.forename = forename;
      return this;
    }

    SamlResposneBuilder withSurname(String surname) {
      this.surname = surname;
      return this;
    }

    SamlResposneBuilder withEmailAddress(String emailAddress) {
      this.emailAddress = emailAddress;
      return this;
    }

    SamlResposneBuilder withPortalPrivileges(String portalPrivilegeCsv) {
      this.portalPrivilegeCsv = portalPrivilegeCsv;
      return this;
    }

    SamlResposneBuilder withProxyWuaId(String proxyWuaId) {
      this.proxyWuaId = proxyWuaId;
      return this;
    }

    SamlResposneBuilder withProxyUserName(String proxyUserName) {
      this.proxyUserName = proxyUserName;
      return this;
    }

    Response build() {
      createSamlAttribute(EnergyPortalSamlAttribute.WEB_USER_ACCOUNT_ID, webUserAccountId);
      createSamlAttribute(EnergyPortalSamlAttribute.PERSON_ID, personId);
      createSamlAttribute(EnergyPortalSamlAttribute.FORENAME, forename);
      createSamlAttribute(EnergyPortalSamlAttribute.SURNAME, surname);
      createSamlAttribute(EnergyPortalSamlAttribute.EMAIL_ADDRESS, emailAddress);
      createSamlAttribute(EnergyPortalSamlAttribute.PORTAL_PRIVILEGES, portalPrivilegeCsv);
      if(proxyWuaId != null) {
        createSamlAttribute(EnergyPortalSamlAttribute.PROXY_USER_WUA_ID, proxyWuaId);
      }
      if(proxyUserName != null) {
        createSamlAttribute(EnergyPortalSamlAttribute.PROXY_USER_NAME, proxyUserName);
      }
      return createResponse();
    }

    Response buildNoAttrs() {
      return createResponse();
    }

    private Response createResponse() {
      var samlResponse = new ResponseBuilder().buildObject();
      var samlAssertion = new AssertionBuilder().buildObject();
      var attributeStatement = new AttributeStatementBuilder().buildObject();
      attributeStatement.getAttributes().addAll(this.attributes);
      samlAssertion.getAttributeStatements().add(attributeStatement);
      samlResponse.getAssertions().add(samlAssertion);
      return samlResponse;
    }

    private void createSamlAttribute(EnergyPortalSamlAttribute samlAttribute, String value) {
      try {
        var attribute = new AttributeBuilder().buildObject();
        attribute.setName(samlAttribute.getAttributeName());

        var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        var element = document.createElement(samlAttribute.getAttributeName());
        element.setTextContent(value);

        var attributeValue = new XSAnyBuilder().buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSAny.TYPE_NAME);
        attributeValue.setDOM(element);

        attribute.getAttributeValues().add(attributeValue);

        this.attributes.add(attribute);
      } catch (Exception e) {
        throw new RuntimeException("Failed to construct SAML attribute", e);
      }
    }
  }
}
