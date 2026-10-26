package uk.co.nstauthority.licensingmanagementservice.licence.correction.workarea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.OrganisationUnit;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaContextTestUtil;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaFilterForm;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaScope;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemView;

@ExtendWith(MockitoExtension.class)
class CorrectionWorkAreaServiceTest {

  private static final long WUA_ID = 123L;

  @Mock
  private LicenceCorrectionService licenceCorrectionService;

  private CorrectionWorkAreaService correctionWorkAreaService;

  private ServiceUserDetail user;

  private final WorkAreaContextTestUtil.Builder contextBuilder = WorkAreaContextTestUtil.builder();

  @BeforeEach
  void setUp() {
    correctionWorkAreaService = new CorrectionWorkAreaService(licenceCorrectionService);
    user = ServiceUserDetailTestUtil.newBuilder().withWuaId(WUA_ID).build();
  }

  @Test
  void getAllWorkItems_queriesInProgressCorrectionsForCurrentUser() {
    when(licenceCorrectionService.getAllInProgressCorrectionsForUser(user))
        .thenReturn(List.of());

    getAllWorkItems(filterForm(null), user);

    verify(licenceCorrectionService).getAllInProgressCorrectionsForUser(user);
  }

  @Test
  void getAllWorkItems_whenNoCorrections_thenEmptyList() {
    when(licenceCorrectionService.getAllInProgressCorrectionsForUser(user))
        .thenReturn(List.of());

    var result = getAllWorkItems(filterForm(null), user);

    assertThat(result).isEmpty();
  }

  @Test
  void getAllWorkItems_filtersToMatchingLicence_andMapsToSearchResultItem() {
    var createdInstant = Instant.parse("2024-01-01T00:00:00Z");
    var matchingId = UUID.randomUUID();
    var matching = correction(matchingId, "P1234", "COR-1", createdInstant);
    var other = correction(UUID.randomUUID(), "P9999", "COR-2", createdInstant);

    when(licenceCorrectionService.getAllInProgressCorrectionsForUser(user))
        .thenReturn(List.of(matching, other));

    var result = getAllWorkItems(filterForm("P1234"), user);

    var expected = SearchResultItem.newBuilder()
        .withId(matchingId.toString())
        .withLinkHeadingText("P1234 - licence correction")
        .withLinkHeadingUrl(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderCorrection(matching)))
        .withCaptionText(String.format("Created %s", DateFormatUtil.convertToDisplayTextWithTime(createdInstant)))
        .withDataItemRow(SummaryDataView.newBuilder()
            .addStringValue("Correction reference", "COR-1")
            .build())
        .withTransactionDatetime(createdInstant)
        .withNewLabel()
        .build();

    assertThat(result).usingRecursiveComparison().isEqualTo(List.of(expected));
  }

  @Test
  void getAllWorkItems_whenCorrectionAlreadyViewedByUser_thenNoNewLabel() {
    var createdInstant = Instant.parse("2024-01-01T00:00:00Z");
    var correctionId = UUID.randomUUID();
    var correction = correction(correctionId, "P1234", "COR-1", createdInstant);

    when(licenceCorrectionService.getAllInProgressCorrectionsForUser(user))
        .thenReturn(List.of(correction));
    contextBuilder.withViewedItems(List.of(
        new WorkAreaItemView(correctionId, WorkAreaDataItemType.LICENCE_CORRECTION, WUA_ID)
    ));

    var result = getAllWorkItems(filterForm(null), user);

    assertThat(result).extracting(SearchResultItem::hasNewLabel).containsExactly(false);
  }

  @Test
  void getAllWorkItems_filtersByLicenceType() {
    var createdInstant = Instant.parse("2024-01-01T00:00:00Z");
    var matchingId = UUID.randomUUID();
    var matching = correction(matchingId, "P1234", LicenceType.SEAWARD_PRODUCTION, "COR-1", createdInstant);
    var other = correction(UUID.randomUUID(), "CS9999", LicenceType.CARBON_STORAGE, "COR-2", createdInstant);

    when(licenceCorrectionService.getAllInProgressCorrectionsForUser(user))
        .thenReturn(List.of(matching, other));

    var form = new WorkAreaFilterForm();
    form.setLicenceTypes(List.of(LicenceType.SEAWARD_PRODUCTION.name()));
    var result = getAllWorkItems(form, user);

    assertThat(result).extracting(SearchResultItem::id).containsExactly(matchingId.toString());
  }

  @Test
  void getAllWorkItems_filteredByLicenseeOrgUnitId_matching() {
    var createdInstant = Instant.parse("2024-01-01T00:00:00Z");
    var matchingId = UUID.randomUUID();
    var matching = correction(matchingId, "P1234", "COR-1", createdInstant);
    var other = correction(UUID.randomUUID(), "P9999", "COR-2", createdInstant);

    when(licenceCorrectionService.getAllInProgressCorrectionsForUser(user))
        .thenReturn(List.of(matching, other));
    contextBuilder.withLicenseesByLicence(Map.of(
        matching.getLicence(), List.of(new OrganisationUnit(1, "Org 1")),
        other.getLicence(), List.of(new OrganisationUnit(2, "Org 2"))
    ));

    var form = new WorkAreaFilterForm();
    form.setLicenseeOrgUnitId(1);
    var result = getAllWorkItems(form, user);

    assertThat(result).extracting(SearchResultItem::id).containsExactly(matchingId.toString());
  }

  @Test
  void getAllWorkItems_filteredByLicenseeOrgGroupId_matching() {
    var createdInstant = Instant.parse("2024-01-01T00:00:00Z");
    var matchingId = UUID.randomUUID();
    var matching = correction(matchingId, "P1234", "COR-1", createdInstant);
    var other = correction(UUID.randomUUID(), "P9999", "COR-2", createdInstant);

    when(licenceCorrectionService.getAllInProgressCorrectionsForUser(user))
        .thenReturn(List.of(matching, other));
    contextBuilder.withLicenseesByLicence(Map.of(
        matching.getLicence(), List.of(new OrganisationUnit(1, "Org 1")),
        other.getLicence(), List.of(new OrganisationUnit(2, "Org 2"))
    ));
    contextBuilder.withLicenseeGroupOrganisationUnitIds(List.of(1));

    var form = new WorkAreaFilterForm();
    form.setLicenseeOrgGroupId(99);
    var result = getAllWorkItems(form, user);

    assertThat(result).extracting(SearchResultItem::id).containsExactly(matchingId.toString());
  }

  @ParameterizedTest
  @MethodSource("applicationFilters")
  void getAllWorkItems_whenApplicationFilterApplied_thenNoItemsAndNotQueried(WorkAreaFilterForm workAreaFilter) {
    var workAreaItems = getAllWorkItems(workAreaFilter, user);

    assertThat(workAreaItems).isEmpty();
    verifyNoInteractions(licenceCorrectionService);
  }

  private static Stream<WorkAreaFilterForm> applicationFilters() {
    var applicationReferenceFilter = new WorkAreaFilterForm();
    applicationReferenceFilter.setApplicationReference("LMS/EEA/001");
    var applicationTypeFilter = new WorkAreaFilterForm();
    applicationTypeFilter.setApplicationTypes(List.of(ApplicationType.CONTINUATION_APPLICATION.name()));
    var applicationStatusFilter = new WorkAreaFilterForm();
    applicationStatusFilter.setApplicationStatuses(List.of(ApplicationStatus.DRAFT.name()));
    return Stream.of(applicationReferenceFilter, applicationTypeFilter, applicationStatusFilter);
  }

  private WorkAreaFilterForm filterForm(String licenceReference) {
    var form = new WorkAreaFilterForm();
    form.setLicenceReference(licenceReference);
    return form;
  }

  private LicenceCorrection correction(
      UUID id, String licenceReference, String correctionReference, Instant createdInstant) {
    return correction(id, licenceReference, LicenceType.SEAWARD_PRODUCTION, correctionReference, createdInstant);
  }

  private LicenceCorrection correction(
      UUID id, String licenceReference, LicenceType licenceType, String correctionReference, Instant createdInstant) {
    var licence = LicenceTestUtil.builder()
        .withLicenceReference(licenceReference)
        .withLicenceType(licenceType)
        .build();
    return LicenceCorrectionTestUtil.newBuilder()
        .withId(id)
        .withLicence(licence)
        .withCorrectionReference(correctionReference)
        .withCreatedInstant(createdInstant)
        .build();
  }

  private List<SearchResultItem> getAllWorkItems(WorkAreaFilterForm workAreaFilterForm, ServiceUserDetail serviceUserDetail) {
    return correctionWorkAreaService.loadSection(workAreaFilterForm, serviceUserDetail, WorkAreaScope.ALL_WORK).buildItems(contextBuilder.build());
  }
}
