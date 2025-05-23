package uk.co.nstauthority.licensingmanagementservice.energyportal.user;

import org.apache.commons.lang3.BooleanUtils;
import uk.co.fivium.energyportalapi.generated.types.User;
import uk.co.nstauthority.licensingmanagementservice.util.UserDisplayNameUtil;

public record EnergyPortalUserJson(
    Long webUserAccountId,
    String title,
    String forename,
    String surname,
    String emailAddress,
    String loginId,
    boolean canLogin,
    String telephoneNumber,
    boolean sharedAccount
) {

  public static EnergyPortalUserJson from(User user) {
    return new EnergyPortalUserJson(
        user.getWebUserAccountId().longValue(),
        user.getTitle(),
        user.getForename(),
        user.getSurname(),
        user.getPrimaryEmailAddress(),
        user.getLoginId(),
        user.getCanLogin(),
        user.getTelephoneNumber(),
        BooleanUtils.isTrue(user.getIsAccountShared())
    );
  }

  public String displayName() {
    return UserDisplayNameUtil.getUserDisplayName(forename, surname);
  }
}
