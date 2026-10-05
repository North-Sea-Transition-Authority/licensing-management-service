package uk.co.nstauthority.licensingmanagementservice.licence.correction.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.util.EnergyPortalUserTestUtil;

@ExtendWith(MockitoExtension.class)
class LicenceCorrectionSearchServiceTest {

  @Mock
  private LicenceCorrectionService licenceCorrectionService;

  @Mock
  private EnergyPortalUserService energyPortalUserService;

  private LicenceCorrectionSearchService licenceCorrectionSearchService;

  @BeforeEach
  void setUp() {
    licenceCorrectionSearchService = new LicenceCorrectionSearchService(
        licenceCorrectionService,
        energyPortalUserService
    );
  }

  @Test
  void getSearchItems() {
    var firstUserWuaId = new WebUserAccountId(100L);
    var secondUserWuaId = new WebUserAccountId(200L);

    var oldestCreatedInstant = Instant.parse("2026-01-01T09:00:00Z");
    var middleCreatedInstant = Instant.parse("2026-02-01T11:15:00Z");
    var newestCreatedInstant = Instant.parse("2026-03-01T14:30:00Z");

    var oldestCorrection = correction(
        "COR-1",
        "P1234",
        LicenceCorrectionStatus.IN_PROGRESS,
        oldestCreatedInstant,
        firstUserWuaId
    );
    var middleCorrection = correction(
        "COR-2",
        "P5678",
        LicenceCorrectionStatus.IN_PROGRESS,
        middleCreatedInstant,
        firstUserWuaId
    );
    var newestCorrection = correction(
        "COR-3",
        "CS001",
        LicenceCorrectionStatus.COMPLETE,
        newestCreatedInstant,
        secondUserWuaId
    );

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

    var result = licenceCorrectionSearchService.getSearchItems();

    var expectedNewestItem = SearchResultItem.newBuilder()
        .withId(newestCorrection.getId().toString())
        .withLinkHeadingText("COR-3")
        .withTagText("Complete")
        .withTagClass("govuk-tag--green")
        .withCaptionText("Created %s".formatted(DateFormatUtil.convertToDisplayTextWithTime(newestCreatedInstant)))
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

  private LicenceCorrection correction(
      String correctionReference,
      String licenceReference,
      LicenceCorrectionStatus status,
      Instant createdInstant,
      WebUserAccountId allocatedToWuaId
  ) {
    var licence = LicenceTestUtil.builder()
        .withLicenceReference(licenceReference)
        .build();

    return LicenceCorrectionTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .withCorrectionReference(correctionReference)
        .withLicence(licence)
        .withStatus(status)
        .withCreatedInstant(createdInstant)
        .withAllocatedToWuaId(allocatedToWuaId.id())
        .build();
  }
}