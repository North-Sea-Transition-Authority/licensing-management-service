package uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.common.LicenceScheduleRelativeOptionsService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReference;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReferenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@ExtendWith(MockitoExtension.class)
class OtherScheduleEventFormServiceTest {

  @Mock
  private OtherScheduleEventRepository otherScheduleEventRepository;

  @Mock
  private LicenceScheduleTermService licenceScheduleTermService;

  @Mock
  private LicenceSchedulePhaseService licenceSchedulePhaseService;

  @Mock
  private LicenceTypeRulesResolver licenceTypeRulesResolver;

  @Mock
  private LicenceScheduleRelativeOptionsService licenceScheduleRelativeOptionsService;

  @Mock
  private LicenceScheduleCalculationService licenceScheduleCalculationService;

  @Mock
  private EventReferenceService eventReferenceService;

  @InjectMocks
  private OtherScheduleEventFormService otherScheduleEventFormService;

  @Captor
  private ArgumentCaptor<OtherScheduleEvent> otherScheduleEventArgumentCaptor;

  private LicenceScheduleDetail licenceScheduleDetail;

  @BeforeEach
  void setUp() {
    var licence = LicenceTestUtil.builder()
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);

    licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);
  }

  @Test
  void getDateOptions() {
    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());
    phase.setPhaseType(PhaseType.PHASE_A);


    when(licenceScheduleRelativeOptionsService.getSchedulePhaseOptions(licenceScheduleDetail)).thenReturn(
        Map.of(phase.getId().toString(),
        phase.getPhaseType().getDisplayName())
    );

    when(licenceTypeRulesResolver.arePhasesCaptured(LicenceType.SEAWARD_PRODUCTION)).thenReturn(true);

    assertThat(otherScheduleEventFormService.getDateOptions(licenceScheduleDetail))
        .isEqualTo(DisplayableEnumOptionUtil.getDisplayableOptions(OtherScheduleEventDateOption.class));
  }

  @Test
  void getDateOptions_licenceTypeDoesntHavePhases() {
    var options = new ArrayList<>(Arrays.asList(OtherScheduleEventDateOption.values()));
    options.remove(OtherScheduleEventDateOption.WITHIN_A_PHASE);

    when(licenceTypeRulesResolver.arePhasesCaptured(LicenceType.SEAWARD_PRODUCTION)).thenReturn(false);

    assertThat(otherScheduleEventFormService.getDateOptions(licenceScheduleDetail))
        .isEqualTo(DisplayableEnumOptionUtil.getDisplayableOptions(options));
  }

  @Test
  void getDateOptions_licenceDoesntHavePhases() {
    var options = new ArrayList<>(Arrays.asList(OtherScheduleEventDateOption.values()));
    options.remove(OtherScheduleEventDateOption.WITHIN_A_PHASE);

    when(licenceScheduleRelativeOptionsService.getSchedulePhaseOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceTypeRulesResolver.arePhasesCaptured(LicenceType.SEAWARD_PRODUCTION)).thenReturn(true);

    assertThat(otherScheduleEventFormService.getDateOptions(licenceScheduleDetail))
        .isEqualTo(DisplayableEnumOptionUtil.getDisplayableOptions(options));
  }

  @Test
  void saveEventFromForm_relativeDate_relatedToTerm() {
    var form = new OtherScheduleEventForm();
    form.setOtherScheduleEventCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    form.setDescription("description");
    form.setOtherScheduleEventDateOption(OtherScheduleEventDateOption.RELATIVE_DATE);

    var termId = UUID.randomUUID();

    form.setRelativeEventId(String.valueOf(termId));

    var term = new LicenceScheduleTerm();
    term.setId(termId);

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term));

    var eventReference = new EventReference();
    when(eventReferenceService.createEventReference(licenceScheduleDetail.getLicenceSchedule())).thenReturn(eventReference);

    var testDuration = new ThreeFieldDuration(1,0,0);

    form.getRelativeDuration().setFromThreeFieldDuration(testDuration);

    otherScheduleEventFormService.saveEventFromForm(form, licenceScheduleDetail, new OtherScheduleEvent());

    verify(otherScheduleEventRepository).save(otherScheduleEventArgumentCaptor.capture());

    assertThat(otherScheduleEventArgumentCaptor.getValue())
        .extracting(
            OtherScheduleEvent::getLicenceScheduleDetail,
            OtherScheduleEvent::getCategory,
            OtherScheduleEvent::getOtherCategoryName,
            OtherScheduleEvent::getDescription,
            OtherScheduleEvent::getDateOption,
            OtherScheduleEvent::getEventDate,
            OtherScheduleEvent::getLicenceScheduleTerm,
            OtherScheduleEvent::getLicenceSchedulePhase,
            OtherScheduleEvent::getRelativeDuration,
            OtherScheduleEvent::getEventReference
        )
        .containsExactly(
            licenceScheduleDetail,
            form.getOtherScheduleEventCategory(),
            null,
            form.getDescription(),
            form.getOtherScheduleEventDateOption(),
            null,
            term,
            null,
            testDuration,
            eventReference
        );

    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  @Test
  void saveEventFromForm_relativeDate_relatedToTerm_existingEvent_doesntOverwriteEventReference() {
    var form = new OtherScheduleEventForm();
    form.setOtherScheduleEventCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    form.setDescription("description");
    form.setOtherScheduleEventDateOption(OtherScheduleEventDateOption.RELATIVE_DATE);

    var termId = UUID.randomUUID();

    form.setRelativeEventId(String.valueOf(termId));

    var term = new LicenceScheduleTerm();
    term.setId(termId);

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term));

    var testDuration = new ThreeFieldDuration(1,0,0);

    form.getRelativeDuration().setFromThreeFieldDuration(testDuration);

    var event = new OtherScheduleEvent();
    event.setEventReference(new EventReference());

    otherScheduleEventFormService.saveEventFromForm(form, licenceScheduleDetail, event);

    verify(otherScheduleEventRepository).save(otherScheduleEventArgumentCaptor.capture());

    assertThat(otherScheduleEventArgumentCaptor.getValue())
        .extracting(
            OtherScheduleEvent::getLicenceScheduleDetail,
            OtherScheduleEvent::getCategory,
            OtherScheduleEvent::getOtherCategoryName,
            OtherScheduleEvent::getDescription,
            OtherScheduleEvent::getDateOption,
            OtherScheduleEvent::getEventDate,
            OtherScheduleEvent::getLicenceScheduleTerm,
            OtherScheduleEvent::getLicenceSchedulePhase,
            OtherScheduleEvent::getRelativeDuration,
            OtherScheduleEvent::getEventReference
        )
        .containsExactly(
            licenceScheduleDetail,
            form.getOtherScheduleEventCategory(),
            null,
            form.getDescription(),
            form.getOtherScheduleEventDateOption(),
            null,
            term,
            null,
            testDuration,
            event.getEventReference()
        );

    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  @Test
  void saveEventFromForm_relativeDate_relatedToPhase() {
    var form = new OtherScheduleEventForm();
    form.setOtherScheduleEventCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    form.setDescription("description");
    form.setOtherScheduleEventDateOption(OtherScheduleEventDateOption.RELATIVE_DATE);

    var phaseId = UUID.randomUUID();

    form.setRelativeEventId(String.valueOf(phaseId));

    var phase = new LicenceSchedulePhase();

    when(licenceSchedulePhaseService.getPhaseByIdOrThrow(phaseId)).thenReturn(phase);
    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of());

    var eventReference = new EventReference();
    when(eventReferenceService.createEventReference(licenceScheduleDetail.getLicenceSchedule())).thenReturn(eventReference);

    var testDuration = new ThreeFieldDuration(1,0,0);

    form.getRelativeDuration().setFromThreeFieldDuration(testDuration);

    otherScheduleEventFormService.saveEventFromForm(form, licenceScheduleDetail, new OtherScheduleEvent());

    verify(otherScheduleEventRepository).save(otherScheduleEventArgumentCaptor.capture());

    assertThat(otherScheduleEventArgumentCaptor.getValue())
        .extracting(
            OtherScheduleEvent::getLicenceScheduleDetail,
            OtherScheduleEvent::getCategory,
            OtherScheduleEvent::getOtherCategoryName,
            OtherScheduleEvent::getDescription,
            OtherScheduleEvent::getDateOption,
            OtherScheduleEvent::getEventDate,
            OtherScheduleEvent::getLicenceScheduleTerm,
            OtherScheduleEvent::getLicenceSchedulePhase,
            OtherScheduleEvent::getRelativeDuration,
            OtherScheduleEvent::getEventReference
        )
        .containsExactly(
            licenceScheduleDetail,
            form.getOtherScheduleEventCategory(),
            null,
            form.getDescription(),
            form.getOtherScheduleEventDateOption(),
            null,
            null,
            phase,
            testDuration,
            eventReference
        );

    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  @Test
  void saveEventFromForm_termOption() {
    var form = new OtherScheduleEventForm();
    form.setOtherScheduleEventCategory(OtherScheduleEventCategory.OTHER_ACTIVITY);
    form.setOtherCategoryName("otherCategoryName");
    form.setDescription("description");
    form.setOtherScheduleEventDateOption(OtherScheduleEventDateOption.WITHIN_A_TERM);

    var termId = UUID.randomUUID();

    form.setLicenceScheduleTermId(String.valueOf(termId));

    var term = new LicenceScheduleTerm();

    when(licenceScheduleTermService.getTermByIdOrThrow(termId)).thenReturn(term);

    var eventReference = new EventReference();
    when(eventReferenceService.createEventReference(licenceScheduleDetail.getLicenceSchedule())).thenReturn(eventReference);

    otherScheduleEventFormService.saveEventFromForm(form, licenceScheduleDetail, new OtherScheduleEvent());

    verify(otherScheduleEventRepository).save(otherScheduleEventArgumentCaptor.capture());

    assertThat(otherScheduleEventArgumentCaptor.getValue())
        .extracting(
            OtherScheduleEvent::getLicenceScheduleDetail,
            OtherScheduleEvent::getCategory,
            OtherScheduleEvent::getOtherCategoryName,
            OtherScheduleEvent::getDescription,
            OtherScheduleEvent::getDateOption,
            OtherScheduleEvent::getEventDate,
            OtherScheduleEvent::getLicenceScheduleTerm,
            OtherScheduleEvent::getLicenceSchedulePhase,
            OtherScheduleEvent::getEventReference
        )
        .containsExactly(
            licenceScheduleDetail,
            form.getOtherScheduleEventCategory(),
            form.getOtherCategoryName(),
            form.getDescription(),
            form.getOtherScheduleEventDateOption(),
            null,
            term,
            null,
            eventReference
        );

    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  @Test
  void saveEventFromForm_termOption_clearsExistingEventDate() {
    var form = new OtherScheduleEventForm();
    form.setOtherScheduleEventCategory(OtherScheduleEventCategory.OTHER_ACTIVITY);
    form.setOtherCategoryName("otherCategoryName");
    form.setDescription("description");
    form.setOtherScheduleEventDateOption(OtherScheduleEventDateOption.WITHIN_A_TERM);

    var termId = UUID.randomUUID();
    form.setLicenceScheduleTermId(String.valueOf(termId));

    when(licenceScheduleTermService.getTermByIdOrThrow(termId)).thenReturn(new LicenceScheduleTerm());

    var event = new OtherScheduleEvent();
    event.setEventDate(LocalDate.of(2025, 1, 1));

    otherScheduleEventFormService.saveEventFromForm(form, licenceScheduleDetail, event);

    verify(otherScheduleEventRepository).save(otherScheduleEventArgumentCaptor.capture());

    assertThat(otherScheduleEventArgumentCaptor.getValue().getEventDate()).isNull();
  }

  @Test
  void saveEventFromForm_phaseOption() {
    var form = new OtherScheduleEventForm();
    form.setOtherScheduleEventCategory(OtherScheduleEventCategory.OTHER_ACTIVITY);
    form.setOtherCategoryName("otherCategoryName");
    form.setDescription("description");
    form.setOtherScheduleEventDateOption(OtherScheduleEventDateOption.WITHIN_A_PHASE);

    var phaseId = UUID.randomUUID();

    form.setLicenceSchedulePhaseId(String.valueOf(phaseId));

    var phase = new LicenceSchedulePhase();

    when(licenceSchedulePhaseService.getPhaseByIdOrThrow(phaseId)).thenReturn(phase);

    var eventReference = new EventReference();
    when(eventReferenceService.createEventReference(licenceScheduleDetail.getLicenceSchedule())).thenReturn(eventReference);

    otherScheduleEventFormService.saveEventFromForm(form, licenceScheduleDetail, new OtherScheduleEvent());

    verify(otherScheduleEventRepository).save(otherScheduleEventArgumentCaptor.capture());

    assertThat(otherScheduleEventArgumentCaptor.getValue())
        .extracting(
            OtherScheduleEvent::getLicenceScheduleDetail,
            OtherScheduleEvent::getCategory,
            OtherScheduleEvent::getOtherCategoryName,
            OtherScheduleEvent::getDescription,
            OtherScheduleEvent::getDateOption,
            OtherScheduleEvent::getEventDate,
            OtherScheduleEvent::getLicenceScheduleTerm,
            OtherScheduleEvent::getLicenceSchedulePhase,
            OtherScheduleEvent::getEventReference
        )
        .containsExactly(
            licenceScheduleDetail,
            form.getOtherScheduleEventCategory(),
            form.getOtherCategoryName(),
            form.getDescription(),
            form.getOtherScheduleEventDateOption(),
            null,
            null,
            phase,
            eventReference
        );

    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  @Test
  void saveEventFromForm_phaseOption_clearsExistingEventDate() {
    var form = new OtherScheduleEventForm();
    form.setOtherScheduleEventCategory(OtherScheduleEventCategory.OTHER_ACTIVITY);
    form.setOtherCategoryName("otherCategoryName");
    form.setDescription("description");
    form.setOtherScheduleEventDateOption(OtherScheduleEventDateOption.WITHIN_A_PHASE);

    var phaseId = UUID.randomUUID();
    form.setLicenceSchedulePhaseId(String.valueOf(phaseId));

    when(licenceSchedulePhaseService.getPhaseByIdOrThrow(phaseId)).thenReturn(new LicenceSchedulePhase());

    var event = new OtherScheduleEvent();
    event.setEventDate(LocalDate.of(2025, 1, 1));

    otherScheduleEventFormService.saveEventFromForm(form, licenceScheduleDetail, event);

    verify(otherScheduleEventRepository).save(otherScheduleEventArgumentCaptor.capture());

    assertThat(otherScheduleEventArgumentCaptor.getValue().getEventDate()).isNull();
  }

  @Test
  void getEventForm_termOption() {
    var term = new LicenceScheduleTerm();
    term.setId(UUID.randomUUID());

    var otherScheduleEvent = new OtherScheduleEvent();
    otherScheduleEvent.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    otherScheduleEvent.setOtherCategoryName("otherCategoryName");
    otherScheduleEvent.setDescription("description");
    otherScheduleEvent.setDateOption(OtherScheduleEventDateOption.WITHIN_A_TERM);
    otherScheduleEvent.setLicenceScheduleTerm(term);
    otherScheduleEvent.setComments("comments");

    assertThat(otherScheduleEventFormService.getEventForm(otherScheduleEvent))
        .extracting(
            OtherScheduleEventForm::getOtherScheduleEventCategory,
            OtherScheduleEventForm::getOtherCategoryName,
            OtherScheduleEventForm::getDescription,
            OtherScheduleEventForm::getOtherScheduleEventDateOption,
            OtherScheduleEventForm::getLicenceScheduleTermId,
            OtherScheduleEventForm::getLicenceSchedulePhaseId,
            OtherScheduleEventForm::getRelativeEventId,
            OtherScheduleEventForm::getComments
        )
        .containsExactly(
            otherScheduleEvent.getCategory(),
            otherScheduleEvent.getOtherCategoryName(),
            otherScheduleEvent.getDescription(),
            otherScheduleEvent.getDateOption(),
            String.valueOf(otherScheduleEvent.getLicenceScheduleTerm().getId()),
            null,
            null,
            otherScheduleEvent.getComments()
    );
  }

  @Test
  void getEventForm_phaseOption() {
    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());

    var otherScheduleEvent = new OtherScheduleEvent();
    otherScheduleEvent.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    otherScheduleEvent.setOtherCategoryName("otherCategoryName");
    otherScheduleEvent.setDescription("description");
    otherScheduleEvent.setDateOption(OtherScheduleEventDateOption.WITHIN_A_PHASE);
    otherScheduleEvent.setLicenceSchedulePhase(phase);
    otherScheduleEvent.setComments("comments");

    assertThat(otherScheduleEventFormService.getEventForm(otherScheduleEvent))
        .extracting(
            OtherScheduleEventForm::getOtherScheduleEventCategory,
            OtherScheduleEventForm::getOtherCategoryName,
            OtherScheduleEventForm::getDescription,
            OtherScheduleEventForm::getOtherScheduleEventDateOption,
            OtherScheduleEventForm::getLicenceScheduleTermId,
            OtherScheduleEventForm::getLicenceSchedulePhaseId,
            OtherScheduleEventForm::getRelativeEventId,
            OtherScheduleEventForm::getComments
        )
        .containsExactly(
            otherScheduleEvent.getCategory(),
            otherScheduleEvent.getOtherCategoryName(),
            otherScheduleEvent.getDescription(),
            otherScheduleEvent.getDateOption(),
            null,
            String.valueOf(otherScheduleEvent.getLicenceSchedulePhase().getId()),
            null,
            otherScheduleEvent.getComments()
        );
  }

  @Test
  void getEventForm_relativeOption() {
    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());

    var otherScheduleEvent = new OtherScheduleEvent();
    otherScheduleEvent.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    otherScheduleEvent.setOtherCategoryName("otherCategoryName");
    otherScheduleEvent.setDescription("description");
    otherScheduleEvent.setDateOption(OtherScheduleEventDateOption.RELATIVE_DATE);
    otherScheduleEvent.setRelativeDuration(new ThreeFieldDuration(1, 2, 3));
    otherScheduleEvent.setLicenceSchedulePhase(phase);
    otherScheduleEvent.setComments("comments");

    var result = otherScheduleEventFormService.getEventForm(otherScheduleEvent);

    assertThat(result)
        .extracting(
            OtherScheduleEventForm::getOtherScheduleEventCategory,
            OtherScheduleEventForm::getOtherCategoryName,
            OtherScheduleEventForm::getDescription,
            OtherScheduleEventForm::getOtherScheduleEventDateOption,
            OtherScheduleEventForm::getLicenceScheduleTermId,
            OtherScheduleEventForm::getLicenceSchedulePhaseId,
            OtherScheduleEventForm::getRelativeEventId,
            OtherScheduleEventForm::getComments
        )
        .containsExactly(
            otherScheduleEvent.getCategory(),
            otherScheduleEvent.getOtherCategoryName(),
            otherScheduleEvent.getDescription(),
            otherScheduleEvent.getDateOption(),
            null,
            null,
            String.valueOf(otherScheduleEvent.getLicenceSchedulePhase().getId()),
            otherScheduleEvent.getComments()
        );

    var duration = result.getRelativeDuration().toThreeFieldDuration();

    assertThat(duration.days()).isEqualTo(otherScheduleEvent.getRelativeDuration().days());
    assertThat(duration.months()).isEqualTo(otherScheduleEvent.getRelativeDuration().months());
    assertThat(duration.years()).isEqualTo(otherScheduleEvent.getRelativeDuration().years());
  }
}