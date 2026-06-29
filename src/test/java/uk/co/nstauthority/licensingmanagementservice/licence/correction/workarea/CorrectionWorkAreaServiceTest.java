package uk.co.nstauthority.licensingmanagementservice.licence.correction.workarea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaFilterForm;

@ExtendWith(MockitoExtension.class)
class CorrectionWorkAreaServiceTest {

  private static final long WUA_ID = 123L;

  @Mock
  private LicenceCorrectionService licenceCorrectionService;

  private CorrectionWorkAreaService correctionWorkAreaService;

  private ServiceUserDetail user;

  @BeforeEach
  void setUp() {
    correctionWorkAreaService = new CorrectionWorkAreaService(licenceCorrectionService);
    user = ServiceUserDetailTestUtil.newBuilder().withWuaId(WUA_ID).build();
  }

  @Test
  void getWorkAreaItems_queriesInProgressCorrectionsForCurrentUser() {
    when(licenceCorrectionService.getAllInProgressCorrectionsForUser(user))
        .thenReturn(List.of());

    correctionWorkAreaService.getWorkAreaItems(filterForm(null), user);

    verify(licenceCorrectionService).getAllInProgressCorrectionsForUser(user);
  }

  @Test
  void getWorkAreaItems_whenNoCorrections_thenEmptyList() {
    when(licenceCorrectionService.getAllInProgressCorrectionsForUser(user))
        .thenReturn(List.of());

    var result = correctionWorkAreaService.getWorkAreaItems(filterForm(null), user);

    assertThat(result).isEmpty();
  }

  @Test
  void getWorkAreaItems_filtersToMatchingLicence_andMapsToSearchResultItem() {
    var createdInstant = Instant.parse("2024-01-01T00:00:00Z");
    var matchingId = UUID.randomUUID();
    var matching = correction(matchingId, "P1234", "COR-1", createdInstant);
    var other = correction(UUID.randomUUID(), "P9999", "COR-2", createdInstant);

    when(licenceCorrectionService.getAllInProgressCorrectionsForUser(user))
        .thenReturn(List.of(matching, other));

    var result = correctionWorkAreaService.getWorkAreaItems(filterForm("P1234"), user);

    var expected = SearchResultItem.newBuilder()
        .withId(matchingId.toString())
        .withLinkHeadingText("P1234")
        .withLinkHeadingUrl(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderCorrection(matchingId, null)))
        .withCaptionText(String.format("Created %s", DateFormatUtil.convertToDisplayTextWithTime(createdInstant)))
        .withDataItemRow(SummaryDataView.newBuilder()
            .addStringValue("Correction reference", "COR-1")
            .build())
        .withTransactionDatetime(createdInstant)
        .build();

    assertThat(result).usingRecursiveComparison().isEqualTo(List.of(expected));
  }

  private WorkAreaFilterForm filterForm(String licenceReference) {
    var form = new WorkAreaFilterForm();
    form.setLicenceReference(licenceReference);
    return form;
  }

  private LicenceCorrection correction(
      UUID id, String licenceReference, String correctionReference, Instant createdInstant) {
    var licence = LicenceTestUtil.builder()
        .withLicenceReference(licenceReference)
        .build();
    return LicenceCorrectionTestUtil.newBuilder()
        .withId(id)
        .withLicence(licence)
        .withCorrectionReference(correctionReference)
        .withCreatedInstant(createdInstant)
        .build();
  }
}