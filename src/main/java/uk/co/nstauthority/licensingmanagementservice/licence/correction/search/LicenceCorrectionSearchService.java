package uk.co.nstauthority.licensingmanagementservice.licence.correction.search;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;

@Service
class LicenceCorrectionSearchService {

  static final String ALLOCATED_USERS_PURPOSE = "Fetch users allocated to licence corrections for the corrections list";

  private final LicenceCorrectionService licenceCorrectionService;
  private final EnergyPortalUserService energyPortalUserService;

  LicenceCorrectionSearchService(
      LicenceCorrectionService licenceCorrectionService,
      EnergyPortalUserService energyPortalUserService
  ) {
    this.licenceCorrectionService = licenceCorrectionService;
    this.energyPortalUserService = energyPortalUserService;
  }

  List<SearchResultItem> getSearchItems() {
    var corrections = licenceCorrectionService.getCorrectionsForSearch();
    var allocatedUsersByWuaId = getAllocatedUsersByWuaId(corrections);

    return corrections.stream()
        .sorted(Comparator.comparing(LicenceCorrection::getCreatedInstant).reversed())
        .map(correction -> getSearchItem(correction, allocatedUsersByWuaId))
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
      Map<WebUserAccountId, EnergyPortalUserJson> allocatedUsersByWuaId
  ) {
    var allocatedUserName = allocatedUsersByWuaId.get(WebUserAccountId.from(correction.getAllocatedToWuaId())).displayName();

    var dataItemRow = SummaryDataView.newBuilder()
        .addStringValue("Licence", correction.getLicence().getLicenceReference())
        .addStringValue("Allocated to", allocatedUserName)
        .build();

    return SearchResultItem.newBuilder()
        .withId(correction.getId().toString())
        .withLinkHeadingText(correction.getCorrectionReference())
        .withTagText(correction.getStatus().getDisplayName())
        .withTagClass(correction.getStatus().getTagClass())
        .withCaptionText(
            "Created %s".formatted(DateFormatUtil.convertToDisplayTextWithTime(correction.getCreatedInstant()))
        )
        .withDataItemRow(dataItemRow)
        .build();
  }
}