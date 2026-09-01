package uk.co.nstauthority.licensingmanagementservice.licence.correction.workarea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupQueryService;
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
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaFilterForm;

@ExtendWith(MockitoExtension.class)
class CorrectionWorkAreaServiceTest {

  private static final long WUA_ID = 123L;

  @Mock
  private LicenceCorrectionService licenceCorrectionService;

  @Mock
  private LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  @Mock
  private OrganisationGroupQueryService organisationGroupQueryService;

  private CorrectionWorkAreaService correctionWorkAreaService;

  private ServiceUserDetail user;

  @BeforeEach
  void setUp() {
    correctionWorkAreaService = new CorrectionWorkAreaService(
        licenceCorrectionService,
        licenceResponsibleOrganisationService,
        organisationGroupQueryService
    );
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

  @Test
  void getWorkAreaItems_filtersByLicenceType() {
    var createdInstant = Instant.parse("2024-01-01T00:00:00Z");
    var matchingId = UUID.randomUUID();
    var matching = correction(matchingId, "P1234", LicenceType.SEAWARD_PRODUCTION, "COR-1", createdInstant);
    var other = correction(UUID.randomUUID(), "CS9999", LicenceType.CARBON_STORAGE, "COR-2", createdInstant);

    when(licenceCorrectionService.getAllInProgressCorrectionsForUser(user))
        .thenReturn(List.of(matching, other));

    var form = new WorkAreaFilterForm();
    form.setLicenceTypes(List.of(LicenceType.SEAWARD_PRODUCTION.name()));
    var result = correctionWorkAreaService.getWorkAreaItems(form, user);

    assertThat(result).extracting(SearchResultItem::id).containsExactly(matchingId.toString());
  }

  @Test
  void getWorkAreaItems_filteredByLicenseeOrgUnitId_matching() {
    var createdInstant = Instant.parse("2024-01-01T00:00:00Z");
    var matchingId = UUID.randomUUID();
    var matching = correction(matchingId, "P1234", "COR-1", createdInstant);
    var other = correction(UUID.randomUUID(), "P9999", "COR-2", createdInstant);

    when(licenceCorrectionService.getAllInProgressCorrectionsForUser(user))
        .thenReturn(List.of(matching, other));
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any())).thenReturn(Map.of(
        matching.getLicence(), List.of(new OrganisationUnit(1, "Org 1")),
        other.getLicence(), List.of(new OrganisationUnit(2, "Org 2"))
    ));
    when(licenceResponsibleOrganisationService.getOrganisationUnitIdsFromLicenceOrgUnitMap(any(), eq(matching.getLicence())))
        .thenReturn(List.of(1));
    when(licenceResponsibleOrganisationService.getOrganisationUnitIdsFromLicenceOrgUnitMap(any(), eq(other.getLicence())))
        .thenReturn(List.of(2));

    var form = new WorkAreaFilterForm();
    form.setLicenseeOrgUnitId(1);
    var result = correctionWorkAreaService.getWorkAreaItems(form, user);

    assertThat(result).extracting(SearchResultItem::id).containsExactly(matchingId.toString());
  }

  @Test
  void getWorkAreaItems_filteredByLicenseeOrgGroupId_matching() {
    var createdInstant = Instant.parse("2024-01-01T00:00:00Z");
    var matchingId = UUID.randomUUID();
    var matching = correction(matchingId, "P1234", "COR-1", createdInstant);
    var other = correction(UUID.randomUUID(), "P9999", "COR-2", createdInstant);

    when(licenceCorrectionService.getAllInProgressCorrectionsForUser(user))
        .thenReturn(List.of(matching, other));
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any())).thenReturn(Map.of(
        matching.getLicence(), List.of(new OrganisationUnit(1, "Org 1")),
        other.getLicence(), List.of(new OrganisationUnit(2, "Org 2"))
    ));
    when(licenceResponsibleOrganisationService.getOrganisationUnitIdsFromLicenceOrgUnitMap(any(), eq(matching.getLicence())))
        .thenReturn(List.of(1));
    when(licenceResponsibleOrganisationService.getOrganisationUnitIdsFromLicenceOrgUnitMap(any(), eq(other.getLicence())))
        .thenReturn(List.of(2));
    when(organisationGroupQueryService.getOrganisationUnitIdsByOrganisationGroupId(99))
        .thenReturn(List.of(1));

    var form = new WorkAreaFilterForm();
    form.setLicenseeOrgGroupId(99);
    var result = correctionWorkAreaService.getWorkAreaItems(form, user);

    assertThat(result).extracting(SearchResultItem::id).containsExactly(matchingId.toString());
  }

  @Test
  void getWorkAreaItems_whenApplicationReferenceFilterApplied_thenExcluded() {
    var createdInstant = Instant.parse("2024-01-01T00:00:00Z");
    var correctionId = UUID.randomUUID();
    var correction = correction(correctionId, "P1234", LicenceType.SEAWARD_PRODUCTION, "COR-1", createdInstant);

    when(licenceCorrectionService.getAllInProgressCorrectionsForUser(user))
        .thenReturn(List.of(correction));

    var form = new WorkAreaFilterForm();
    form.setApplicationReference("LMS/EEA/001");
    var result = correctionWorkAreaService.getWorkAreaItems(form, user);

    assertThat(result).isEmpty();
  }

  @Test
  void getWorkAreaItems_whenApplicationTypeFilterApplied_thenExcluded() {
    var createdInstant = Instant.parse("2024-01-01T00:00:00Z");
    var correctionId = UUID.randomUUID();
    var correction = correction(correctionId, "P1234", LicenceType.SEAWARD_PRODUCTION, "COR-1", createdInstant);

    when(licenceCorrectionService.getAllInProgressCorrectionsForUser(user))
        .thenReturn(List.of(correction));

    var form = new WorkAreaFilterForm();
    form.setApplicationTypes(List.of(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.name()));
    var result = correctionWorkAreaService.getWorkAreaItems(form, user);

    assertThat(result).isEmpty();
  }

  @Test
  void getWorkAreaItems_whenApplicationStatusFilterApplied_thenExcluded() {
    var createdInstant = Instant.parse("2024-01-01T00:00:00Z");
    var correctionId = UUID.randomUUID();
    var correction = correction(correctionId, "P1234", LicenceType.SEAWARD_PRODUCTION, "COR-1", createdInstant);

    when(licenceCorrectionService.getAllInProgressCorrectionsForUser(user))
        .thenReturn(List.of(correction));

    var form = new WorkAreaFilterForm();
    form.setApplicationStatuses(List.of(ApplicationStatus.DRAFT.name()));
    var result = correctionWorkAreaService.getWorkAreaItems(form, user);

    assertThat(result).isEmpty();
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
}