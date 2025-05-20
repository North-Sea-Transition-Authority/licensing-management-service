package uk.co.nstauthority.template.energyportal.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.co.nstauthority.template.energyportal.user.EnergyPortalUserService.USERS_PROJECTION_ROOT;
import static uk.co.nstauthority.template.energyportal.user.EnergyPortalUserService.USER_PROJECTION_ROOT;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.energyportalapi.client.LogCorrelationId;
import uk.co.fivium.energyportalapi.client.RequestPurpose;
import uk.co.fivium.energyportalapi.client.user.UserApi;
import uk.co.fivium.energyportalapi.generated.types.User;
import uk.co.nstauthority.template.util.EnergyPortalUserTestUtil;


@ExtendWith(MockitoExtension.class)
class EnergyPortalUserServiceTest {
  private static final RequestPurpose REQUEST_PURPOSE = new RequestPurpose("purpose");
  private final User user1 = EnergyPortalUserTestUtil.newBuilder().withForename("Vince").withWebUserAccountId(1L).build();
  private final WebUserAccountId wuaId1 = new WebUserAccountId(1);
  private final WebUserAccountId wuaId2 = new WebUserAccountId(2);

  @Mock
  private UserApi userApi;

  @InjectMocks
  private EnergyPortalUserService energyPortalUserService;

  @Test
  void findUsersByEmail() {
    var user = EnergyPortalUserTestUtil.newBuilder().withLoginId("vince@gov.uk").build();
    var userJson = EnergyPortalUserJson.from(user);

    when(userApi.searchUsersByEmail(
        user.getLoginId(),
        USERS_PROJECTION_ROOT,
        REQUEST_PURPOSE
    )).thenReturn(List.of(user));

    var result = energyPortalUserService.findUsersByEmail(user.getLoginId(), REQUEST_PURPOSE.purpose());
    assertThat(result).containsExactly(userJson);
  }

  @Test
  void findUsersByEmail_whenUserCannotLogin_thenEmptyResult() {
    var user = EnergyPortalUserTestUtil.newBuilder()
        .withLoginId("howard@gov.uk")
        .canLogin(false)
        .build();

    when(userApi.searchUsersByEmail(
        user.getLoginId(),
        USERS_PROJECTION_ROOT,
        REQUEST_PURPOSE
    )).thenReturn(List.of(user));

    var result = energyPortalUserService.findUsersByEmail(user.getLoginId(), REQUEST_PURPOSE.purpose());
    assertThat(result).isEmpty();
  }

  @ParameterizedTest
  @NullAndEmptySource
  void findUserByEmail_whenNoEmailGiven_thenDontCallEpa(String email) {
    assertThat(energyPortalUserService.findUsersByEmail(email, REQUEST_PURPOSE.purpose())).isEmpty();
    verify(userApi, never()).searchUsersByEmail(any(), any(), any(), any(LogCorrelationId.class));
  }

  @Test
  void findUsersByEmail_whenNoMatchingUsers_thenEmptyResult() {
    var loginId = "no matches";
    when(userApi.searchUsersByEmail(
        loginId,
        USERS_PROJECTION_ROOT,
        REQUEST_PURPOSE
    )).thenReturn(Collections.emptyList());

    var result = energyPortalUserService.findUsersByEmail(loginId, REQUEST_PURPOSE.purpose());
    assertThat(result).isEmpty();
  }

  @Test
  void findByWuaIds() {
    var user2 = EnergyPortalUserTestUtil.newBuilder().withForename("Alyssa").withWebUserAccountId(2L).build();

    when(userApi.searchUsersByIds(
        List.of(user1.getWebUserAccountId(), user2.getWebUserAccountId()),
        USERS_PROJECTION_ROOT,
        REQUEST_PURPOSE
    )).thenReturn(List.of(user1, user2));

    var result = energyPortalUserService.findByWuaIds(List.of(wuaId1, wuaId2), REQUEST_PURPOSE.purpose());
    assertThat(result).containsExactly(EnergyPortalUserJson.from(user2), EnergyPortalUserJson.from(user1));
  }

  @Test
  void findByWuaIds_whenNoMatch_thenEmptyResult() {
    when(userApi.searchUsersByIds(
        List.of(1L, 2L),
        USERS_PROJECTION_ROOT,
        REQUEST_PURPOSE
    )).thenReturn(Collections.emptyList());

    var result = energyPortalUserService.findByWuaIds(List.of(wuaId1, wuaId2), REQUEST_PURPOSE.purpose());
    assertThat(result).isEmpty();
  }

  @Test
  void findByWuaIds_whenEmptyList_thenDontCallEpa(){
    var result = energyPortalUserService.findByWuaIds(Collections.emptyList(), REQUEST_PURPOSE.purpose());

    verify(userApi, never()).searchUsersByIds(any(), any(), any(), any(LogCorrelationId.class));
    assertThat(result).isEmpty();
  }

  @Test
  void getEnergyPortalUserMap() {
    var user2 = EnergyPortalUserTestUtil.newBuilder().withForename("Alyssa").withWebUserAccountId(2L).build();

    var user1Json = EnergyPortalUserJson.from(user1);
    var user2Json = EnergyPortalUserJson.from(user2);

    when(userApi.searchUsersByIds(
        List.of(user1.getWebUserAccountId(), user2.getWebUserAccountId()),
        USERS_PROJECTION_ROOT,
        REQUEST_PURPOSE
    )).thenReturn(List.of(user1, user2));

    var result = energyPortalUserService.getEnergyPortalUserMap(List.of(wuaId1, wuaId2), REQUEST_PURPOSE.purpose());
    assertThat(result).containsExactly(
        entry(wuaId1, user1Json),
        entry(wuaId2, user2Json)
    );
  }

  @Test
  void findByWuaId() {
    var user = EnergyPortalUserTestUtil.newBuilder().withForename("Vince").withWebUserAccountId(1L).build();
    var wuaId = new WebUserAccountId(1);
    var userJson = EnergyPortalUserJson.from(user);

    when(userApi.findUserById(
        user.getWebUserAccountId(),
        USER_PROJECTION_ROOT,
        REQUEST_PURPOSE
    )).thenReturn(Optional.of(user));

    var result = energyPortalUserService.findByWuaId(wuaId, REQUEST_PURPOSE.purpose());
    assertThat(result)
        .isPresent()
        .contains(userJson);
  }

  @Test
  void findByWuaId_whenNoMatch_thenEmptyOptional() {
    var wuaId = new WebUserAccountId(1);

    when(userApi.findUserById(
        wuaId.toInt(),
        USER_PROJECTION_ROOT,
        REQUEST_PURPOSE
    )).thenReturn(Optional.empty());

    var result = energyPortalUserService.findByWuaId(wuaId, REQUEST_PURPOSE.purpose());
    assertThat(result).isEmpty();
  }

  @Test
  void findByWuaId_whenNullId_thenDontCallEpa() {
    assertThat(energyPortalUserService.findByWuaId(null, REQUEST_PURPOSE.purpose())).isEmpty();
    verify(userApi, never()).findUserById(anyInt(), any(), any(), any(LogCorrelationId.class));
  }

  @Test
  void getByWuaId() {
    var user = EnergyPortalUserTestUtil.newBuilder().withForename("Vince").withWebUserAccountId(1L).build();
    var wuaId = new WebUserAccountId(1);
    var userJson = EnergyPortalUserJson.from(user);

    when(userApi.findUserById(
        user.getWebUserAccountId(),
        USER_PROJECTION_ROOT,
        REQUEST_PURPOSE
    )).thenReturn(Optional.of(user));

    var result = energyPortalUserService.getByWuaId(wuaId, REQUEST_PURPOSE.purpose());
    assertThat(result).isEqualTo(userJson);
  }

  @Test
  void getByWuaId_whenNoMatch_thenError() {
    var wuaId = new WebUserAccountId(1);

    when(userApi.findUserById(
        wuaId.toInt(),
        USER_PROJECTION_ROOT,
        REQUEST_PURPOSE
    )).thenReturn(Optional.empty());

    var purpose = REQUEST_PURPOSE.purpose();

    assertThatThrownBy(() -> energyPortalUserService.getByWuaId(wuaId, purpose))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessage("Energy portal user with wua id %s not found"
            .formatted(wuaId.toString()));
  }
}
