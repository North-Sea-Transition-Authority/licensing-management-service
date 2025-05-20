package uk.co.nstauthority.template.authentication;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ServiceUserDetailTest {

  @Test
  void displayName() {
    var user = ServiceUserDetailTestUtil.newBuilder().build();

    assertThat(user.displayName()).isEqualTo("Forename Surname");
  }

  @Test
  void displayNameAndEmail() {
    var user = ServiceUserDetailTestUtil.newBuilder().build();

    assertThat(user.displayNameAndEmail()).isEqualTo("Forename Surname (test.user@test.com)");
  }

  @Test
  void displayNameIncludingAnyProxyUser_proxyWuaIdIsNull() {
    var user = ServiceUserDetailTestUtil.newBuilder()
        .withProxyWuaId(null)
        .build();

    assertThat(user.displayNameIncludingAnyProxyUser()).isEqualTo("Forename Surname");
  }

  @Test
  void displayNameIncludingAnyProxyUser_proxyWuaIdIsNotNull() {
    var user = ServiceUserDetailTestUtil.newBuilder()
        .withProxyWuaId(3L)
        .build();

    assertThat(user.displayNameIncludingAnyProxyUser()).isEqualTo("proxyUsername/Forename Surname");
  }
}
