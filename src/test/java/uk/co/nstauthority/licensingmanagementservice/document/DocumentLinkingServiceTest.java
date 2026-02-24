package uk.co.nstauthority.licensingmanagementservice.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.energyportalapi.generated.types.Address;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.DocumentInstanceDtoTestUtil;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;

@ExtendWith(MockitoExtension.class)
class DocumentLinkingServiceTest {

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @Mock
  private LicenceContinuationService licenceContinuationService;

  @Mock
  private ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;

  @InjectMocks
  private DocumentLinkingService documentLinkingService;

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final LicenceScheduleDetail LICENCE_SCHEDULE_DETAIL
      = LicenceScheduleTestUtil.createLicenceScheduleDetail(LicenceScheduleTestUtil.createLicenceSchedule(LICENCE));

  private static final LicenceContinuationApplicationDetail LICENCE_CONTINUATION_APPLICATION_DETAIL
      = LicenceContinuationApplicationTestUtil.createLicenceContinuationApplicationDetail(LICENCE_SCHEDULE_DETAIL);

  private static final ScheduleWorkProgrammeApplicationDetail SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL
      = ScheduleWorkProgrammeApplicationDetailTestUtil.createScheduleWorkProgrammeApplicationDetail(LICENCE_SCHEDULE_DETAIL) ;

  @Test
  void getContinuationApplicationCompanyNameFromDocumentInstanceDto() {
    when(licenceContinuationService.getDetailByIdOrThrow(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId()))
        .thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);

    var documentInstanceDto = DocumentInstanceDtoTestUtil.newBuilder()
        .withItemType(ApplicationType.CONTINUATION_APPLICATION.name())
        .withItemReference(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId().toString())
        .build();

    when(organisationUnitQueryService.getOrganisationUnitNameById(any()))
        .thenReturn(Optional.of("test name"));

    var result = documentLinkingService.getApplicationCompanyNameFromDto(documentInstanceDto);
    assertThat(result).isEqualTo("test name");
    verify(licenceContinuationService).getDetailByIdOrThrow(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId());
  }

  @Test
  void getContinuationApplicationCompanyAddressFromDocumentInstanceDto() {
    when(licenceContinuationService.getDetailByIdOrThrow(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId()))
        .thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);

    var documentInstanceDto = DocumentInstanceDtoTestUtil.newBuilder()
        .withItemType(ApplicationType.CONTINUATION_APPLICATION.name())
        .withItemReference(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId().toString())
        .build();

    when(organisationUnitQueryService.getOrganisationUnitAddressById(any()))
        .thenReturn(Optional.of(new Address("test address")));

    var result = documentLinkingService.getApplicationCompanyAddressFromDto(documentInstanceDto);
    assertThat(result).isEqualTo(new Address("test address"));
    verify(licenceContinuationService).getDetailByIdOrThrow(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId());
  }

  @Test
  void getScheduleAmendmentApplicationCompanyNameFromDocumentInstanceDto() {
    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL.getId()))
        .thenReturn(SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL);

    System.out.println(SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL.getId());
    var documentInstanceDto = DocumentInstanceDtoTestUtil.newBuilder()
        .withItemType(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.name())
        .withItemReference(SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL.getId().toString())
        .build();

    when(organisationUnitQueryService.getOrganisationUnitNameById(any()))
        .thenReturn(Optional.of("test name"));

    var result = documentLinkingService.getApplicationCompanyNameFromDto(documentInstanceDto);
    assertThat(result).isEqualTo("test name");
    verify(scheduleWorkProgrammeApplicationService).getDetailByIdOrThrow(SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL.getId());
  }

  @Test
  void getScheduleAmendmentApplicationCompanyAddressFromDocumentInstanceDto() {
    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL.getId()))
        .thenReturn(SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL);

    var documentInstanceDto = DocumentInstanceDtoTestUtil.newBuilder()
        .withItemType(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.name())
        .withItemReference(SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL.getId().toString())
        .build();

    when(organisationUnitQueryService.getOrganisationUnitAddressById(any()))
        .thenReturn(Optional.of(new Address("test address")));

    var result = documentLinkingService.getApplicationCompanyAddressFromDto(documentInstanceDto);
    assertThat(result).isEqualTo(new Address("test address"));
    verify(scheduleWorkProgrammeApplicationService).getDetailByIdOrThrow(SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL.getId());
  }

  @Test
  void getAmendmentApplicationCompanyFromDocumentInstanceDtoWrongItemType() {
    var documentInstanceDto = DocumentInstanceDtoTestUtil.newBuilder()
        .withItemType("wrong item type")
        .build();

    assertThatThrownBy(
        () -> documentLinkingService.getApplicationCompanyNameFromDto(documentInstanceDto))
        .isInstanceOf(IllegalArgumentException.class);

    verify(licenceContinuationService, never()).getDetailByIdOrThrow(any());
    verify(scheduleWorkProgrammeApplicationService, never()).getDetailByIdOrThrow(any());
  }
}