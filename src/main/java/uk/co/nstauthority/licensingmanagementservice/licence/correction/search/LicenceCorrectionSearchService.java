package uk.co.nstauthority.licensingmanagementservice.licence.correction.search;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionRoles;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply.CorrectionSummaryController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;

@Service
class LicenceCorrectionSearchService {

  static final String ALLOCATED_USERS_PURPOSE = "Fetch users allocated to licence corrections for the corrections list";

  private final LicenceCorrectionService licenceCorrectionService;
  private final EnergyPortalUserService energyPortalUserService;
  private final TeamQueryService teamQueryService;

  LicenceCorrectionSearchService(
      LicenceCorrectionService licenceCorrectionService,
      EnergyPortalUserService energyPortalUserService,
      TeamQueryService teamQueryService
  ) {
    this.licenceCorrectionService = licenceCorrectionService;
    this.energyPortalUserService = energyPortalUserService;
    this.teamQueryService = teamQueryService;
  }

  List<SearchResultItem> getSearchItems(ServiceUserDetail user) {
    var corrections = licenceCorrectionService.getCorrectionsForSearch();
    var allocatedUsersByWuaId = getAllocatedUsersByWuaId(corrections);
    var userRoles = teamQueryService.getTeamRolesForUser(user.wuaId()).stream()
        .map(TeamRole::getRole)
        .collect(Collectors.toSet());

    return corrections.stream()
        .sorted(Comparator.comparing(this::getEffectiveInstant).reversed())
        .map(correction -> getSearchItem(correction, allocatedUsersByWuaId, userRoles))
        .toList();
  }

  private Map<WebUserAccountId, EnergyPortalUserJson> getAllocatedUsersByWuaId(
      Collection<LicenceCorrection> corrections
  ) {
    var allocatedWuaIds = corrections.stream()
        .map(LicenceCorrection::getAllocatedToWuaId)
        .map(WebUserAccountId::from)
        .collect(Collectors.toSet());

    return energyPortalUserService.getEnergyPortalUserMap(allocatedWuaIds, ALLOCATED_USERS_PURPOSE);
  }

  private SearchResultItem getSearchItem(
      LicenceCorrection correction,
      Map<WebUserAccountId, EnergyPortalUserJson> allocatedUsersByWuaId,
      Set<Role> userRoles
  ) {
    var allocatedUserName = allocatedUsersByWuaId.get(WebUserAccountId.from(correction.getAllocatedToWuaId())).displayName();

    var dataItemRow = SummaryDataView.newBuilder()
        .addStringValue("Licence", correction.getLicence().getLicenceReference())
        .addStringValue("Allocated to", allocatedUserName)
        .build();

    return SearchResultItem.newBuilder()
        .withId(correction.getId().toString())
        .withLinkHeadingText(correction.getCorrectionReference())
        .withLinkHeadingUrl(getSummaryUrlIfUserIsCorrector(correction, userRoles))
        .withTagText(correction.getStatus().getDisplayName())
        .withTagClass(correction.getStatus().getTagClass())
        .withCaptionText(getCaptionText(correction))
        .withDataItemRow(dataItemRow)
        .build();
  }

  private Instant getEffectiveInstant(LicenceCorrection correction) {
    return correction.isComplete()
        ? correction.getCompletedInstant()
        : correction.getCreatedInstant();
  }

  private String getCaptionText(LicenceCorrection correction) {
    var captionPrefix = correction.isComplete() ? "Completed" : "Created";
    return "%s %s".formatted(
        captionPrefix,
        DateFormatUtil.convertToDisplayTextWithTime(getEffectiveInstant(correction))
    );
  }

  private String getSummaryUrlIfUserIsCorrector(LicenceCorrection correction, Set<Role> userRoles) {
    if (!LicenceCorrectionRoles.hasCorrectorRole(correction.getLicence().getType(), userRoles)) {
      return null;
    }
    return ReverseRouter.route(on(CorrectionSummaryController.class)
        .renderCorrectionSummary(correction));
  }
}