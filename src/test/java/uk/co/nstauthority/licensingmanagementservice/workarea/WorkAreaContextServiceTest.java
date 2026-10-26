package uk.co.nstauthority.licensingmanagementservice.workarea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.OrganisationUnit;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemView;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemViewService;

@ExtendWith(MockitoExtension.class)
class WorkAreaContextServiceTest {

  private static final long WUA_ID = 123L;

  @Mock
  private LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  @Mock
  private OrganisationGroupQueryService organisationGroupQueryService;

  @Mock
  private WorkAreaItemViewService workAreaItemViewService;

  @InjectMocks
  private WorkAreaContextService workAreaContextService;

  private final ServiceUserDetail user = ServiceUserDetailTestUtil.newBuilder().withWuaId(WUA_ID).build();
  private final Licence firstLicence = LicenceTestUtil.builder().withId(1).withLicenceReference("P1").build();
  private final Licence secondLicence = LicenceTestUtil.builder().withId(2).withLicenceReference("P2").build();
  private final Licence thirdLicence = LicenceTestUtil.builder().withId(3).withLicenceReference("P3").build();

  @Test
  void buildContext_looksUpEverySectionsLicenseesTogetherAndOrganisationGroupsForSectionsThatNeedThem() {
    var groupSection = new WorkAreaSection(
        WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION,
        List.of(firstLicence, secondLicence),
        OrganisationGroupLookup.REQUIRED,
        context -> List.of()
    );
    var otherSection = new WorkAreaSection(
        WorkAreaDataItemType.DRAFT_LICENCE_SCHEDULE,
        List.of(secondLicence, thirdLicence),
        OrganisationGroupLookup.NOT_REQUIRED,
        context -> List.of()
    );
    var licenseesByLicence = Map.of(
        firstLicence, List.of(new OrganisationUnit(10, "Org 10")),
        secondLicence, List.of(new OrganisationUnit(20, "Org 20")),
        thirdLicence, List.of(new OrganisationUnit(30, "Org 30"))
    );
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(
        List.of(firstLicence, secondLicence, thirdLicence)
    )).thenReturn(licenseesByLicence);
    when(licenceResponsibleOrganisationService.getOrgGroupIdByOrgUnitIds(List.of(10, 20)))
        .thenReturn(Map.of(10, 100, 20, 200));

    var result = workAreaContextService.buildContext(List.of(groupSection, otherSection), new WorkAreaFilterForm(), user);

    assertThat(result).usingRecursiveComparison().isEqualTo(new WorkAreaContext(
        licenseesByLicence,
        Map.of(10, 100, 20, 200),
        null,
        Map.of()
    ));
  }

  @Test
  void buildContext_whenNoSectionNeedsOrganisationGroups_doesNotLookThemUp() {
    var section = new WorkAreaSection(
        WorkAreaDataItemType.LICENCE_CORRECTION,
        List.of(firstLicence),
        OrganisationGroupLookup.NOT_REQUIRED,
        context -> List.of()
    );
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(List.of(firstLicence)))
        .thenReturn(Map.of(firstLicence, List.of(new OrganisationUnit(10, "Org 10"))));

    var result = workAreaContextService.buildContext(List.of(section), new WorkAreaFilterForm(), user);

    assertThat(result.organisationGroupIdsByUnitId()).isEmpty();
  }

  @Test
  void buildContext_whenSectionsHaveNoLicences_doesNotLookUpLicensees() {
    var section = new WorkAreaSection(
        WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION, List.of(), OrganisationGroupLookup.REQUIRED, context -> List.of());

    var result = workAreaContextService.buildContext(List.of(section), new WorkAreaFilterForm(), user);

    assertThat(result.licenseesByLicence()).isEmpty();
    assertThat(result.organisationGroupIdsByUnitId()).isEmpty();
    verifyNoInteractions(licenceResponsibleOrganisationService);
  }

  @Test
  void buildContext_whenFilteredByLicenseeGroup_looksUpTheGroupsOrganisationUnits() {
    var form = new WorkAreaFilterForm();
    form.setLicenseeOrgGroupId(99);
    when(organisationGroupQueryService.getOrganisationUnitIdsByOrganisationGroupId(99)).thenReturn(List.of(10, 20));

    var result = workAreaContextService.buildContext(List.of(), form, user);

    assertThat(result.licenseeGroupOrganisationUnitIds()).containsExactly(10, 20);
  }

  @Test
  void buildContext_looksUpViewedItemsForEveryItemTypeTogether() {
    var applicationSection = WorkAreaSection.empty(WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION);
    var correctionSection = WorkAreaSection.empty(WorkAreaDataItemType.LICENCE_CORRECTION);
    var viewedApplicationId = UUID.randomUUID();
    var viewedCorrectionId = UUID.randomUUID();
    when(workAreaItemViewService.getWorkAreaItemLogsForUser(
        List.of(WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION, WorkAreaDataItemType.LICENCE_CORRECTION),
        WUA_ID
    )).thenReturn(List.of(
        new WorkAreaItemView(viewedApplicationId, WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION, WUA_ID),
        new WorkAreaItemView(viewedCorrectionId, WorkAreaDataItemType.LICENCE_CORRECTION, WUA_ID)
    ));

    var result = workAreaContextService.buildContext(
        List.of(applicationSection, correctionSection), new WorkAreaFilterForm(), user);

    assertThat(result.viewedItemIdsByType()).isEqualTo(Map.of(
        WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION, Set.of(viewedApplicationId),
        WorkAreaDataItemType.LICENCE_CORRECTION, Set.of(viewedCorrectionId)
    ));
    verify(workAreaItemViewService).getWorkAreaItemLogsForUser(
        List.of(WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION, WorkAreaDataItemType.LICENCE_CORRECTION),
        WUA_ID
    );
  }
}
