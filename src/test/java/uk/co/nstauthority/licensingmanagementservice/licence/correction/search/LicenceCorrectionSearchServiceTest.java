package uk.co.nstauthority.licensingmanagementservice.licence.correction.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply.CorrectionSummaryController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRoleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.util.EnergyPortalUserTestUtil;

@ExtendWith(MockitoExtension.class)
class LicenceCorrectionSearchServiceTest {

  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder().build();

  @Mock
  private LicenceCorrectionService licenceCorrectionService;

  @Mock
  private EnergyPortalUserService energyPortalUserService;

  @Mock
  private TeamQueryService teamQueryService;

  private LicenceCorrectionSearchService licenceCorrectionSearchService;

  @BeforeEach
  void setUp() {
    licenceCorrectionSearchService = new LicenceCorrectionSearchService(
        licenceCorrectionService,
        energyPortalUserService,
        teamQueryService
    );
  }

  @Test
  void getSearchItems() {
    var firstUserWuaId = new WebUserAccountId(100L);
    var secondUserWuaId = new WebUserAccountId(200L);

    var oldestCreatedInstant = Instant.parse("2026-01-01T09:00:00Z");
    var middleCreatedInstant = Instant.parse("2026-02-01T11:15:00Z");
    var newestCompletedInstant = Instant.parse("2026-03-01T14:30:00Z");

    var oldestCorrection = correction("COR-1", "P1234", LicenceType.SEAWARD_PRODUCTION, firstUserWuaId)
        .withStatus(LicenceCorrectionStatus.IN_PROGRESS)
        .withCreatedInstant(oldestCreatedInstant)
        .build();
    var middleCorrection = correction("COR-2", "P5678", LicenceType.SEAWARD_PRODUCTION, firstUserWuaId)
        .withStatus(LicenceCorrectionStatus.IN_PROGRESS)
        .withCreatedInstant(middleCreatedInstant)
        .build();
    var newestCorrection = correction("COR-3", "CS001", LicenceType.CARBON_STORAGE, secondUserWuaId)
        .withStatus(LicenceCorrectionStatus.COMPLETE)
        .withCreatedInstant(Instant.parse("2026-02-20T09:00:00Z"))
        .withCompletedInstant(newestCompletedInstant)
        .build();

    var firstUser = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(firstUserWuaId.id())
        .withForename("Jane")
        .withSurname("Smith")
        .buildJson();
    var secondUser = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(secondUserWuaId.id())
        .withForename("John")
        .withSurname("Jones")
        .buildJson();

    when(licenceCorrectionService.getCorrectionsForSearch())
        .thenReturn(List.of(oldestCorrection, newestCorrection, middleCorrection));
    when(energyPortalUserService.getEnergyPortalUserMap(
        Set.of(firstUserWuaId, secondUserWuaId),
        LicenceCorrectionSearchService.ALLOCATED_USERS_PURPOSE
    ))
        .thenReturn(Map.of(firstUserWuaId, firstUser, secondUserWuaId, secondUser));
    when(teamQueryService.getTeamRolesForUser(USER.wuaId())).thenReturn(Set.of());

    var result = licenceCorrectionSearchService.getSearchItems(USER);

    var expectedNewestItem = SearchResultItem.newBuilder()
        .withId(newestCorrection.getId().toString())
        .withLinkHeadingText("COR-3")
        .withTagText("Complete")
        .withTagClass("govuk-tag--green")
        .withCaptionText("Completed %s".formatted(DateFormatUtil.convertToDisplayTextWithTime(newestCompletedInstant)))
        .withDataItemRow(SummaryDataView.newBuilder()
            .addStringValue("Licence", "CS001")
            .addStringValue("Allocated to", "John Jones")
            .build())
        .build();
    var expectedMiddleItem = SearchResultItem.newBuilder()
        .withId(middleCorrection.getId().toString())
        .withLinkHeadingText("COR-2")
        .withTagText("In progress")
        .withTagClass("govuk-tag--light-blue")
        .withCaptionText("Created %s".formatted(DateFormatUtil.convertToDisplayTextWithTime(middleCreatedInstant)))
        .withDataItemRow(SummaryDataView.newBuilder()
            .addStringValue("Licence", "P5678")
            .addStringValue("Allocated to", "Jane Smith")
            .build())
        .build();
    var expectedOldestItem = SearchResultItem.newBuilder()
        .withId(oldestCorrection.getId().toString())
        .withLinkHeadingText("COR-1")
        .withTagText("In progress")
        .withTagClass("govuk-tag--light-blue")
        .withCaptionText("Created %s".formatted(DateFormatUtil.convertToDisplayTextWithTime(oldestCreatedInstant)))
        .withDataItemRow(SummaryDataView.newBuilder()
            .addStringValue("Licence", "P1234")
            .addStringValue("Allocated to", "Jane Smith")
            .build())
        .build();

    assertThat(result).usingRecursiveComparison()
        .isEqualTo(List.of(expectedNewestItem, expectedMiddleItem, expectedOldestItem));
  }

  @Test
  void getSearchItems_whenOldCorrectionCompletedRecently_thenOrderedByEffectiveDate() {
    var allocatedUserWuaId = new WebUserAccountId(100L);

    var recentlyCompletedInstant = Instant.parse("2026-04-01T16:45:00Z");
    var inProgressCreatedInstant = Instant.parse("2026-03-01T14:30:00Z");
    var earlierCompletedInstant = Instant.parse("2026-02-15T10:00:00Z");

    var recentlyCompletedCorrection = correction("COR-1", "P1234", LicenceType.SEAWARD_PRODUCTION, allocatedUserWuaId)
        .withStatus(LicenceCorrectionStatus.COMPLETE)
        .withCreatedInstant(Instant.parse("2025-12-01T09:00:00Z"))
        .withCompletedInstant(recentlyCompletedInstant)
        .build();
    var inProgressCorrection = correction("COR-2", "P5678", LicenceType.SEAWARD_PRODUCTION, allocatedUserWuaId)
        .withStatus(LicenceCorrectionStatus.IN_PROGRESS)
        .withCreatedInstant(inProgressCreatedInstant)
        .build();
    var earlierCompletedCorrection = correction("COR-3", "P9012", LicenceType.SEAWARD_PRODUCTION, allocatedUserWuaId)
        .withStatus(LicenceCorrectionStatus.COMPLETE)
        .withCreatedInstant(Instant.parse("2026-01-01T09:00:00Z"))
        .withCompletedInstant(earlierCompletedInstant)
        .build();

    var allocatedUser = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(allocatedUserWuaId.id())
        .withForename("Jane")
        .withSurname("Smith")
        .buildJson();

    when(licenceCorrectionService.getCorrectionsForSearch())
        .thenReturn(List.of(inProgressCorrection, earlierCompletedCorrection, recentlyCompletedCorrection));
    when(energyPortalUserService.getEnergyPortalUserMap(
        Set.of(allocatedUserWuaId),
        LicenceCorrectionSearchService.ALLOCATED_USERS_PURPOSE
    ))
        .thenReturn(Map.of(allocatedUserWuaId, allocatedUser));
    when(teamQueryService.getTeamRolesForUser(USER.wuaId())).thenReturn(Set.of());

    var result = licenceCorrectionSearchService.getSearchItems(USER);

    var expectedRecentlyCompletedItem = SearchResultItem.newBuilder()
        .withId(recentlyCompletedCorrection.getId().toString())
        .withLinkHeadingText("COR-1")
        .withTagText("Complete")
        .withTagClass("govuk-tag--green")
        .withCaptionText(
            "Completed %s".formatted(DateFormatUtil.convertToDisplayTextWithTime(recentlyCompletedInstant))
        )
        .withDataItemRow(SummaryDataView.newBuilder()
            .addStringValue("Licence", "P1234")
            .addStringValue("Allocated to", "Jane Smith")
            .build())
        .build();
    var expectedInProgressItem = SearchResultItem.newBuilder()
        .withId(inProgressCorrection.getId().toString())
        .withLinkHeadingText("COR-2")
        .withTagText("In progress")
        .withTagClass("govuk-tag--light-blue")
        .withCaptionText("Created %s".formatted(DateFormatUtil.convertToDisplayTextWithTime(inProgressCreatedInstant)))
        .withDataItemRow(SummaryDataView.newBuilder()
            .addStringValue("Licence", "P5678")
            .addStringValue("Allocated to", "Jane Smith")
            .build())
        .build();
    var expectedEarlierCompletedItem = SearchResultItem.newBuilder()
        .withId(earlierCompletedCorrection.getId().toString())
        .withLinkHeadingText("COR-3")
        .withTagText("Complete")
        .withTagClass("govuk-tag--green")
        .withCaptionText(
            "Completed %s".formatted(DateFormatUtil.convertToDisplayTextWithTime(earlierCompletedInstant))
        )
        .withDataItemRow(SummaryDataView.newBuilder()
            .addStringValue("Licence", "P9012")
            .addStringValue("Allocated to", "Jane Smith")
            .build())
        .build();

    assertThat(result).usingRecursiveComparison()
        .isEqualTo(List.of(expectedRecentlyCompletedItem, expectedInProgressItem, expectedEarlierCompletedItem));
  }

  @ParameterizedTest
  @MethodSource("correctorRoleArguments")
  void getSearchItems_whenUserHasCorrectorRoles_thenOnlyMatchingLicenceTypesLinkToSummary(
      Set<Role> userRoles,
      boolean productionRowLinked,
      boolean carbonStorageRowLinked
  ) {
    var allocatedUserWuaId = new WebUserAccountId(100L);
    var productionCreatedInstant = Instant.parse("2026-02-01T09:00:00Z");
    var carbonStorageCreatedInstant = Instant.parse("2026-01-01T09:00:00Z");

    var productionCorrection = correction("COR-1", "P1234", LicenceType.SEAWARD_PRODUCTION, allocatedUserWuaId)
        .withStatus(LicenceCorrectionStatus.IN_PROGRESS)
        .withCreatedInstant(productionCreatedInstant)
        .build();
    var carbonStorageCorrection = correction("COR-2", "CS001", LicenceType.CARBON_STORAGE, allocatedUserWuaId)
        .withStatus(LicenceCorrectionStatus.IN_PROGRESS)
        .withCreatedInstant(carbonStorageCreatedInstant)
        .build();

    var allocatedUser = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(allocatedUserWuaId.id())
        .withForename("Jane")
        .withSurname("Smith")
        .buildJson();

    when(licenceCorrectionService.getCorrectionsForSearch())
        .thenReturn(List.of(productionCorrection, carbonStorageCorrection));
    when(energyPortalUserService.getEnergyPortalUserMap(
        Set.of(allocatedUserWuaId),
        LicenceCorrectionSearchService.ALLOCATED_USERS_PURPOSE
    ))
        .thenReturn(Map.of(allocatedUserWuaId, allocatedUser));
    var userTeamRoles = userRoles.stream()
        .map(role -> TeamRoleTestUtil.newBuilder()
            .withRole(role)
            .withWuaId(USER.wuaId())
            .build())
        .collect(Collectors.toSet());

    when(teamQueryService.getTeamRolesForUser(USER.wuaId())).thenReturn(userTeamRoles);

    var result = licenceCorrectionSearchService.getSearchItems(USER);

    var expectedProductionItem = SearchResultItem.newBuilder()
        .withId(productionCorrection.getId().toString())
        .withLinkHeadingText("COR-1")
        .withLinkHeadingUrl(productionRowLinked ? summaryUrl(productionCorrection) : null)
        .withTagText("In progress")
        .withTagClass("govuk-tag--light-blue")
        .withCaptionText("Created %s".formatted(DateFormatUtil.convertToDisplayTextWithTime(productionCreatedInstant)))
        .withDataItemRow(SummaryDataView.newBuilder()
            .addStringValue("Licence", "P1234")
            .addStringValue("Allocated to", "Jane Smith")
            .build())
        .build();
    var expectedCarbonStorageItem = SearchResultItem.newBuilder()
        .withId(carbonStorageCorrection.getId().toString())
        .withLinkHeadingText("COR-2")
        .withLinkHeadingUrl(carbonStorageRowLinked ? summaryUrl(carbonStorageCorrection) : null)
        .withTagText("In progress")
        .withTagClass("govuk-tag--light-blue")
        .withCaptionText(
            "Created %s".formatted(DateFormatUtil.convertToDisplayTextWithTime(carbonStorageCreatedInstant))
        )
        .withDataItemRow(SummaryDataView.newBuilder()
            .addStringValue("Licence", "CS001")
            .addStringValue("Allocated to", "Jane Smith")
            .build())
        .build();

    assertThat(result).usingRecursiveComparison()
        .isEqualTo(List.of(expectedProductionItem, expectedCarbonStorageItem));
  }

  private static Stream<Arguments> correctorRoleArguments() {
    return Stream.of(
        Arguments.of(Set.of(Role.PRODUCTION_LICENCE_CORRECTOR), true, false),
        Arguments.of(Set.of(Role.CARBON_STORAGE_LICENCE_CORRECTOR), false, true),
        Arguments.of(Set.of(Role.PRODUCTION_LICENCE_CORRECTOR, Role.CARBON_STORAGE_LICENCE_CORRECTOR), true, true),
        Arguments.of(Set.of(Role.VIEW_ANY_LICENCE), false, false)
    );
  }

  private String summaryUrl(LicenceCorrection correction) {
    return ReverseRouter.route(on(CorrectionSummaryController.class)
        .renderCorrectionSummary(correction));
  }

  private LicenceCorrectionTestUtil correction(
      String correctionReference,
      String licenceReference,
      LicenceType licenceType,
      WebUserAccountId allocatedToWuaId
  ) {
    var licence = LicenceTestUtil.builder()
        .withLicenceReference(licenceReference)
        .withLicenceType(licenceType)
        .build();

    return LicenceCorrectionTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .withCorrectionReference(correctionReference)
        .withLicence(licence)
        .withAllocatedToWuaId(allocatedToWuaId.id());
  }
}