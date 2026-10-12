package uk.co.nstauthority.licensingmanagementservice.licence.correction.search;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionRoles;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply.CorrectionSummaryController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.util.FilterUtil;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;

@Service
class LicenceCorrectionSearchService {

  static final String ALLOCATED_USERS_PURPOSE = "Fetch users allocated to licence corrections for the corrections list";
  static final String ALLOCATED_USER_OPTIONS_PURPOSE =
      "Fetch users allocated to licence corrections for the corrections list filter";

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

  List<SearchResultItem> getSearchItems(LicenceCorrectionSearchFilterForm form, ServiceUserDetail user) {
    var corrections = licenceCorrectionService.getCorrectionsForSearch().stream()
        .filter(c -> FilterUtil.matchesTextInput(c.getLicence().getLicenceReference(), form.getLicenceReference()))
        .filter(c -> FilterUtil.matchesEnum(LicenceCorrectionStatus.class, c.getStatus(), form.getCorrectionStatuses()))
        .filter(c -> matchesAllocatedUser(c, form.getAllocatedToWuaId()))
        .toList();
    var allocatedUsersByWuaId = getAllocatedUsersByWuaId(corrections);
    var userRoles = teamQueryService.getTeamRolesForUser(user.wuaId()).stream()
        .map(TeamRole::getRole)
        .collect(Collectors.toSet());

    return corrections.stream()
        .filter(c -> FilterUtil.matchesEnum(LicenceCorrectionStatus.class, c.getStatus(), form.getCorrectionStatuses()))
        .sorted(Comparator.comparing(this::getEffectiveInstant).reversed())
        .map(correction -> getSearchItem(correction, allocatedUsersByWuaId, userRoles))
        .toList();
  }

  Map<String, String> getAllocatedUserOptions() {
    var allocatedWuaIds = licenceCorrectionService.getCorrectionsForSearch().stream()
        .map(LicenceCorrection::getAllocatedToWuaId)
        .map(WebUserAccountId::from)
        .collect(Collectors.toSet());

    return energyPortalUserService.findByWuaIds(allocatedWuaIds, ALLOCATED_USER_OPTIONS_PURPOSE)
        .stream()
        .sorted(Comparator.comparing(EnergyPortalUserJson::displayName))
        .collect(StreamUtil.toLinkedHashMap(
            energyPortalUserJson -> String.valueOf(energyPortalUserJson.webUserAccountId()),
            EnergyPortalUserJson::displayName
        ));
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

  private boolean matchesAllocatedUser(LicenceCorrection correction, String allocatedToWuaIdFilter) {
    return StringUtils.isBlank(allocatedToWuaIdFilter)
        || allocatedToWuaIdFilter.equals(String.valueOf(correction.getAllocatedToWuaId()));
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