package uk.co.nstauthority.template.energyportal.user;

import io.micrometer.common.util.StringUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.co.fivium.energyportalapi.client.RequestPurpose;
import uk.co.fivium.energyportalapi.client.user.UserApi;
import uk.co.fivium.energyportalapi.generated.client.UserProjectionRoot;
import uk.co.fivium.energyportalapi.generated.client.UsersProjectionRoot;
import uk.co.fivium.energyportalapi.generated.types.User;

@Service
public class EnergyPortalUserService {

  static final UsersProjectionRoot USERS_PROJECTION_ROOT = new UsersProjectionRoot()
      .webUserAccountId()
      .title()
      .forename()
      .surname()
      .primaryEmailAddress()
      .loginId()
      .canLogin();

  static final UserProjectionRoot USER_PROJECTION_ROOT = new UserProjectionRoot()
      .webUserAccountId()
      .title()
      .forename()
      .surname()
      .primaryEmailAddress()
      .loginId()
      .canLogin();

  private final UserApi userApi;

  @Autowired
  public EnergyPortalUserService(UserApi userApi) {
    this.userApi = userApi;
  }

  public List<EnergyPortalUserJson> findUsersByEmail(String username, String purpose) {
    return StringUtils.isBlank(username)
        ? List.of()
        : userApi.searchUsersByEmail(
            username,
            USERS_PROJECTION_ROOT,
            new RequestPurpose(purpose)
        )
        .stream()
        .filter(User::getCanLogin)
        .map(EnergyPortalUserJson::from)
        .toList();
  }

  public List<EnergyPortalUserJson> findByWuaIds(Collection<WebUserAccountId> webUserAccountIds, String purpose) {
    if (CollectionUtils.isEmpty(webUserAccountIds)) {
      return Collections.emptyList();
    }

    List<Long> webUserAccountIdApiInputs = webUserAccountIds
        .stream()
        .map(WebUserAccountId::id)
        .toList();

    return userApi.searchUsersByIds(
            webUserAccountIdApiInputs,
            USERS_PROJECTION_ROOT,
            new RequestPurpose(purpose)
        )
        .stream()
        .map(EnergyPortalUserJson::from)
        .sorted(Comparator.comparing(EnergyPortalUserJson::displayName))
        .toList();
  }

  public Map<WebUserAccountId, EnergyPortalUserJson> getEnergyPortalUserMap(
      Collection<WebUserAccountId> webUserAccountIds,
      String purpose
  ) {
    return findByWuaIds(webUserAccountIds, purpose)
        .stream()
        .collect(Collectors.toMap(
            energyPortalUser -> WebUserAccountId.from(energyPortalUser.webUserAccountId()),
            Function.identity()
        ));
  }

  public Optional<EnergyPortalUserJson> findByWuaId(WebUserAccountId webUserAccountId, String purpose) {
    return webUserAccountId == null
        ? Optional.empty()
        : userApi.findUserById(
            webUserAccountId.toInt(),
            USER_PROJECTION_ROOT,
            new RequestPurpose(purpose)
        )
        .stream()
        .map(EnergyPortalUserJson::from)
        .findFirst();
  }

  public EnergyPortalUserJson getByWuaId(WebUserAccountId webUserAccountId, String purpose) {
    return findByWuaId(webUserAccountId, purpose)
        .orElseThrow(() ->
            new NoSuchElementException("Energy portal user with wua id %s not found"
                .formatted(webUserAccountId.toString())));
  }
}
