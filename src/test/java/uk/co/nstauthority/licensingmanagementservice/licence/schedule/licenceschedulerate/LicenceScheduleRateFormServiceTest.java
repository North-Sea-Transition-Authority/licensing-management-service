package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.common.LicenceScheduleRelativeOptionsService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReference;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReferenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleRateFormServiceTest {

  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder().build();

  @Mock
  private LicenceScheduleRateRepository licenceScheduleRateRepository;

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

  @Mock
  private EventCommentService eventCommentService;

  @InjectMocks
  private LicenceScheduleRateFormService licenceScheduleRateFormService;

  @Captor
  private ArgumentCaptor<LicenceScheduleRate> licenceScheduleRateArgumentCaptor;

  private Licence licence;

  private LicenceScheduleDetail licenceScheduleDetail;

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder()
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);

    licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);
  }

  @Test
  void saveRateFromForm_termOption() {
    var form = new LicenceScheduleRateForm();
    form.setRateDefinitionOption(RateDefinitionOption.TERM);
    form.getRentalRate().setInputValue("1");

    var termId = UUID.randomUUID();

    form.setLicenceScheduleTermId(String.valueOf(termId));

    var term = new LicenceScheduleTerm();

    when(licenceScheduleTermService.getTermByIdOrThrow(termId)).thenReturn(term);

    var eventReference = new EventReference();
    when(eventReferenceService.createEventReference(licenceScheduleDetail.getLicenceSchedule(), ScheduleEventType.RATE)).thenReturn(eventReference);

    licenceScheduleRateFormService.saveRateFromForm(form, licenceScheduleDetail, new LicenceScheduleRate(), USER);

    verify(licenceScheduleRateRepository).save(licenceScheduleRateArgumentCaptor.capture());

    assertThat(licenceScheduleRateArgumentCaptor.getValue()).extracting(
        LicenceScheduleRate::getLicenceScheduleDetail,
        LicenceScheduleRate::getRateDefinitionOption,
        LicenceScheduleRate::getLicenceScheduleTerm,
        LicenceScheduleRate::getLicenceSchedulePhase,
        LicenceScheduleRate::getRateRelativeDateOption,
        LicenceScheduleRate::getRelativeDuration,
        LicenceScheduleRate::getStartDate,
        LicenceScheduleRate::getRentalRate,
        LicenceScheduleRate::getEventReference
    ).containsExactly(
        licenceScheduleDetail,
        RateDefinitionOption.TERM,
        term,
        null,
        null,
        null,
        null,
        BigDecimal.ONE,
        eventReference
    );

    verify(eventCommentService).addOrUpdatePendingComment(form.getComments(), eventReference, USER);
    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  @Test
  void saveRateFromForm_termOption_existingRate_doesntOverwriteEventReference() {
    var form = new LicenceScheduleRateForm();
    form.setRateDefinitionOption(RateDefinitionOption.TERM);
    form.getRentalRate().setInputValue("1");

    var termId = UUID.randomUUID();

    form.setLicenceScheduleTermId(String.valueOf(termId));

    var term = new LicenceScheduleTerm();

    when(licenceScheduleTermService.getTermByIdOrThrow(termId)).thenReturn(term);

    var rate = new LicenceScheduleRate();
    rate.setEventReference(new EventReference());

    licenceScheduleRateFormService.saveRateFromForm(form, licenceScheduleDetail, rate, USER);

    verify(licenceScheduleRateRepository).save(licenceScheduleRateArgumentCaptor.capture());

    assertThat(licenceScheduleRateArgumentCaptor.getValue()).extracting(
        LicenceScheduleRate::getLicenceScheduleDetail,
        LicenceScheduleRate::getRateDefinitionOption,
        LicenceScheduleRate::getLicenceScheduleTerm,
        LicenceScheduleRate::getLicenceSchedulePhase,
        LicenceScheduleRate::getRateRelativeDateOption,
        LicenceScheduleRate::getRelativeDuration,
        LicenceScheduleRate::getStartDate,
        LicenceScheduleRate::getRentalRate,
        LicenceScheduleRate::getEventReference
    ).containsExactly(
        licenceScheduleDetail,
        RateDefinitionOption.TERM,
        term,
        null,
        null,
        null,
        null,
        BigDecimal.ONE,
        rate.getEventReference()
    );

    verify(eventCommentService).addOrUpdatePendingComment(form.getComments(), rate.getEventReference(), USER);
    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  @Test
  void saveRateFromForm_termOption_clearsExistingStartDate() {
    var form = new LicenceScheduleRateForm();
    form.setRateDefinitionOption(RateDefinitionOption.TERM);
    form.getRentalRate().setInputValue("1");

    var termId = UUID.randomUUID();
    form.setLicenceScheduleTermId(String.valueOf(termId));

    when(licenceScheduleTermService.getTermByIdOrThrow(termId)).thenReturn(new LicenceScheduleTerm());

    var rate = new LicenceScheduleRate();
    rate.setStartDate(LocalDate.of(2025, 1, 1));

    licenceScheduleRateFormService.saveRateFromForm(form, licenceScheduleDetail, rate, USER);

    verify(licenceScheduleRateRepository).save(licenceScheduleRateArgumentCaptor.capture());

    assertThat(licenceScheduleRateArgumentCaptor.getValue().getStartDate()).isNull();
  }

  @Test
  void saveRateFromForm_phaseOption() {
    var form = new LicenceScheduleRateForm();
    form.setRateDefinitionOption(RateDefinitionOption.PHASE);
    form.getRentalRate().setInputValue("1");

    var phaseId = UUID.randomUUID();

    form.setLicenceSchedulePhaseId(String.valueOf(phaseId));

    var phase = new LicenceSchedulePhase();

    when(licenceSchedulePhaseService.getPhaseByIdOrThrow(phaseId)).thenReturn(phase);

    var eventReference = new EventReference();
    when(eventReferenceService.createEventReference(licenceScheduleDetail.getLicenceSchedule(), ScheduleEventType.RATE)).thenReturn(eventReference);

    licenceScheduleRateFormService.saveRateFromForm(form, licenceScheduleDetail, new LicenceScheduleRate(), USER);

    verify(licenceScheduleRateRepository).save(licenceScheduleRateArgumentCaptor.capture());

    assertThat(licenceScheduleRateArgumentCaptor.getValue()).extracting(
        LicenceScheduleRate::getLicenceScheduleDetail,
        LicenceScheduleRate::getRateDefinitionOption,
        LicenceScheduleRate::getLicenceScheduleTerm,
        LicenceScheduleRate::getLicenceSchedulePhase,
        LicenceScheduleRate::getRateRelativeDateOption,
        LicenceScheduleRate::getRelativeDuration,
        LicenceScheduleRate::getStartDate,
        LicenceScheduleRate::getRentalRate,
        LicenceScheduleRate::getEventReference
    ).containsExactly(
        licenceScheduleDetail,
        RateDefinitionOption.PHASE,
        null,
        phase,
        null,
        null,
        null,
        BigDecimal.ONE,
        eventReference
    );

    verify(eventCommentService).addOrUpdatePendingComment(form.getComments(), eventReference, USER);
    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  @Test
  void saveRateFromForm_phaseOption_clearsExistingStartDate() {
    var form = new LicenceScheduleRateForm();
    form.setRateDefinitionOption(RateDefinitionOption.PHASE);
    form.getRentalRate().setInputValue("1");

    var phaseId = UUID.randomUUID();
    form.setLicenceSchedulePhaseId(String.valueOf(phaseId));

    when(licenceSchedulePhaseService.getPhaseByIdOrThrow(phaseId)).thenReturn(new LicenceSchedulePhase());

    var rate = new LicenceScheduleRate();
    rate.setStartDate(LocalDate.of(2025, 1, 1));

    licenceScheduleRateFormService.saveRateFromForm(form, licenceScheduleDetail, rate, USER);

    verify(licenceScheduleRateRepository).save(licenceScheduleRateArgumentCaptor.capture());

    assertThat(licenceScheduleRateArgumentCaptor.getValue().getStartDate()).isNull();
  }

  @Test
  void saveRateFromForm_customPeriodOption_onStartDateOfTerm() {
    var form = new LicenceScheduleRateForm();
    form.setRateDefinitionOption(RateDefinitionOption.CUSTOM_PERIOD);
    form.getRentalRate().setInputValue("1");
    form.setRateRelativeDateOption(RateRelativeDateOption.ON_START_DATE);

    var termId = UUID.randomUUID();
    form.setRelativeEventId(String.valueOf(termId));

    var term = new LicenceScheduleTerm();
    term.setId(termId);

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term));

    var eventReference = new EventReference();
    when(eventReferenceService.createEventReference(licenceScheduleDetail.getLicenceSchedule(), ScheduleEventType.RATE)).thenReturn(eventReference);

    licenceScheduleRateFormService.saveRateFromForm(form, licenceScheduleDetail, new LicenceScheduleRate(), USER);

    verify(licenceScheduleRateRepository).save(licenceScheduleRateArgumentCaptor.capture());

    assertThat(licenceScheduleRateArgumentCaptor.getValue()).extracting(
        LicenceScheduleRate::getLicenceScheduleDetail,
        LicenceScheduleRate::getRateDefinitionOption,
        LicenceScheduleRate::getLicenceScheduleTerm,
        LicenceScheduleRate::getLicenceSchedulePhase,
        LicenceScheduleRate::getRateRelativeDateOption,
        LicenceScheduleRate::getRelativeDuration,
        LicenceScheduleRate::getStartDate,
        LicenceScheduleRate::getRentalRate,
        LicenceScheduleRate::getEventReference
    ).containsExactly(
        licenceScheduleDetail,
        RateDefinitionOption.CUSTOM_PERIOD,
        term,
        null,
        RateRelativeDateOption.ON_START_DATE,
        null,
        null,
        BigDecimal.ONE,
        eventReference
    );

    verify(eventCommentService).addOrUpdatePendingComment(form.getComments(), eventReference, USER);
    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  @Test
  void saveRateFromForm_customPeriodOption_onStartDateOfPhase() {
    var form = new LicenceScheduleRateForm();
    form.setRateDefinitionOption(RateDefinitionOption.CUSTOM_PERIOD);
    form.getRentalRate().setInputValue("1");
    form.setRateRelativeDateOption(RateRelativeDateOption.ON_START_DATE);

    var phaseId = UUID.randomUUID();

    form.setRelativeEventId(String.valueOf(phaseId));

    var phase = new LicenceSchedulePhase();

    when(licenceSchedulePhaseService.getPhaseByIdOrThrow(phaseId)).thenReturn(phase);
    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of());

    var eventReference = new EventReference();
    when(eventReferenceService.createEventReference(licenceScheduleDetail.getLicenceSchedule(), ScheduleEventType.RATE)).thenReturn(eventReference);

    licenceScheduleRateFormService.saveRateFromForm(form, licenceScheduleDetail, new LicenceScheduleRate(), USER);

    verify(licenceScheduleRateRepository).save(licenceScheduleRateArgumentCaptor.capture());

    assertThat(licenceScheduleRateArgumentCaptor.getValue()).extracting(
        LicenceScheduleRate::getLicenceScheduleDetail,
        LicenceScheduleRate::getRateDefinitionOption,
        LicenceScheduleRate::getLicenceScheduleTerm,
        LicenceScheduleRate::getLicenceSchedulePhase,
        LicenceScheduleRate::getRateRelativeDateOption,
        LicenceScheduleRate::getRelativeDuration,
        LicenceScheduleRate::getStartDate,
        LicenceScheduleRate::getRentalRate,
        LicenceScheduleRate::getEventReference
    ).containsExactly(
        licenceScheduleDetail,
        RateDefinitionOption.CUSTOM_PERIOD,
        null,
        phase,
        RateRelativeDateOption.ON_START_DATE,
        null,
        null,
        BigDecimal.ONE,
        eventReference
    );

    verify(eventCommentService).addOrUpdatePendingComment(form.getComments(), eventReference, USER);
    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  @Test
  void saveRateFromForm_customPeriodOption_relativeToStartDateOfTerm() {
    var form = new LicenceScheduleRateForm();
    form.setRateDefinitionOption(RateDefinitionOption.CUSTOM_PERIOD);
    form.getRentalRate().setInputValue("1");
    form.setRateRelativeDateOption(RateRelativeDateOption.RELATIVE_TO_START_DATE);

    var termId = UUID.randomUUID();
    form.setRelativeEventId(String.valueOf(termId));

    var term = new LicenceScheduleTerm();
    term.setId(termId);

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term));

    var eventReference = new EventReference();
    when(eventReferenceService.createEventReference(licenceScheduleDetail.getLicenceSchedule(), ScheduleEventType.RATE)).thenReturn(eventReference);

    var duration = new ThreeFieldDuration(1, 0, 0);
    form.getRelativeDuration().setFromThreeFieldDuration(duration);

    licenceScheduleRateFormService.saveRateFromForm(form, licenceScheduleDetail, new LicenceScheduleRate(), USER);

    verify(licenceScheduleRateRepository).save(licenceScheduleRateArgumentCaptor.capture());

    assertThat(licenceScheduleRateArgumentCaptor.getValue()).extracting(
        LicenceScheduleRate::getLicenceScheduleDetail,
        LicenceScheduleRate::getRateDefinitionOption,
        LicenceScheduleRate::getLicenceScheduleTerm,
        LicenceScheduleRate::getLicenceSchedulePhase,
        LicenceScheduleRate::getRateRelativeDateOption,
        LicenceScheduleRate::getRelativeDuration,
        LicenceScheduleRate::getStartDate,
        LicenceScheduleRate::getRentalRate,
        LicenceScheduleRate::getEventReference
    ).containsExactly(
        licenceScheduleDetail,
        RateDefinitionOption.CUSTOM_PERIOD,
        term,
        null,
        RateRelativeDateOption.RELATIVE_TO_START_DATE,
        duration,
        null,
        BigDecimal.ONE,
        eventReference
    );

    verify(eventCommentService).addOrUpdatePendingComment(form.getComments(), eventReference, USER);
    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  @Test
  void saveRateFromForm_customPeriodOption_relativeToStartDateOfPhase() {
    var form = new LicenceScheduleRateForm();
    form.setRateDefinitionOption(RateDefinitionOption.CUSTOM_PERIOD);
    form.getRentalRate().setInputValue("1");
    form.setRateRelativeDateOption(RateRelativeDateOption.RELATIVE_TO_START_DATE);

    var phaseId = UUID.randomUUID();

    form.setRelativeEventId(String.valueOf(phaseId));

    var phase = new LicenceSchedulePhase();

    when(licenceSchedulePhaseService.getPhaseByIdOrThrow(phaseId)).thenReturn(phase);
    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of());

    var eventReference = new EventReference();
    when(eventReferenceService.createEventReference(licenceScheduleDetail.getLicenceSchedule(), ScheduleEventType.RATE)).thenReturn(eventReference);

    var duration = new ThreeFieldDuration(1, 0, 0);
    form.getRelativeDuration().setFromThreeFieldDuration(duration);

    licenceScheduleRateFormService.saveRateFromForm(form, licenceScheduleDetail, new LicenceScheduleRate(), USER);

    verify(licenceScheduleRateRepository).save(licenceScheduleRateArgumentCaptor.capture());

    assertThat(licenceScheduleRateArgumentCaptor.getValue()).extracting(
        LicenceScheduleRate::getLicenceScheduleDetail,
        LicenceScheduleRate::getRateDefinitionOption,
        LicenceScheduleRate::getLicenceScheduleTerm,
        LicenceScheduleRate::getLicenceSchedulePhase,
        LicenceScheduleRate::getRateRelativeDateOption,
        LicenceScheduleRate::getRelativeDuration,
        LicenceScheduleRate::getStartDate,
        LicenceScheduleRate::getRentalRate,
        LicenceScheduleRate::getEventReference
    ).containsExactly(
        licenceScheduleDetail,
        RateDefinitionOption.CUSTOM_PERIOD,
        null,
        phase,
        RateRelativeDateOption.RELATIVE_TO_START_DATE,
        duration,
        null,
        BigDecimal.ONE,
        eventReference
    );

    verify(eventCommentService).addOrUpdatePendingComment(form.getComments(), eventReference, USER);
    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  @Test
  void getFormFromRate_termOption() {
    var term = new LicenceScheduleTerm();
    term.setId(UUID.randomUUID());

    var rate = new LicenceScheduleRate();
    rate.setRateDefinitionOption(RateDefinitionOption.TERM);
    rate.setRentalRate(BigDecimal.ONE);
    rate.setLicenceScheduleTerm(term);

    var result = licenceScheduleRateFormService.getFormFromRate(rate);

    assertThat(result).extracting(
        LicenceScheduleRateForm::getRateDefinitionOption,
        form -> form.getRentalRate().getAsBigDecimal().get(),
        LicenceScheduleRateForm::getComments,
        LicenceScheduleRateForm::getLicenceScheduleTermId,
        LicenceScheduleRateForm::getLicenceSchedulePhaseId,
        LicenceScheduleRateForm::getRelativeEventId,
        LicenceScheduleRateForm::getRateRelativeDateOption
    ).containsExactly(
        rate.getRateDefinitionOption(),
        rate.getRentalRate(),
        null,
        rate.getLicenceScheduleTerm().getId().toString(),
        null,
        null,
        null
    );
  }

  @Test
  void getFormFromRate_phaseOption() {
    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());

    var rate = new LicenceScheduleRate();
    rate.setRateDefinitionOption(RateDefinitionOption.PHASE);
    rate.setRentalRate(BigDecimal.ONE);
    rate.setLicenceSchedulePhase(phase);

    var result = licenceScheduleRateFormService.getFormFromRate(rate);

    assertThat(result).extracting(
        LicenceScheduleRateForm::getRateDefinitionOption,
        form -> form.getRentalRate().getAsBigDecimal().get(),
        LicenceScheduleRateForm::getComments,
        LicenceScheduleRateForm::getLicenceScheduleTermId,
        LicenceScheduleRateForm::getLicenceSchedulePhaseId,
        LicenceScheduleRateForm::getRelativeEventId,
        LicenceScheduleRateForm::getRateRelativeDateOption
    ).containsExactly(
        rate.getRateDefinitionOption(),
        rate.getRentalRate(),
        null,
        null,
        rate.getLicenceSchedulePhase().getId().toString(),
        null,
        null
    );
  }

  @Test
  void getFormFromRate_customOption_onStartDate() {
    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());

    var rate = new LicenceScheduleRate();
    rate.setRateDefinitionOption(RateDefinitionOption.CUSTOM_PERIOD);
    rate.setRentalRate(BigDecimal.ONE);
    rate.setLicenceSchedulePhase(phase);
    rate.setRateRelativeDateOption(RateRelativeDateOption.ON_START_DATE);

    var result = licenceScheduleRateFormService.getFormFromRate(rate);

    assertThat(result).extracting(
        LicenceScheduleRateForm::getRateDefinitionOption,
        form -> form.getRentalRate().getAsBigDecimal().get(),
        LicenceScheduleRateForm::getComments,
        LicenceScheduleRateForm::getLicenceScheduleTermId,
        LicenceScheduleRateForm::getLicenceSchedulePhaseId,
        LicenceScheduleRateForm::getRelativeEventId,
        LicenceScheduleRateForm::getRateRelativeDateOption
    ).containsExactly(
        rate.getRateDefinitionOption(),
        rate.getRentalRate(),
        null,
        null,
        null,
        rate.getLicenceSchedulePhase().getId().toString(),
        rate.getRateRelativeDateOption()
    );
  }

  @Test
  void getFormFromRate_customOption_onRelativeDate() {
    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());

    var rate = new LicenceScheduleRate();
    rate.setRateDefinitionOption(RateDefinitionOption.CUSTOM_PERIOD);
    rate.setRentalRate(BigDecimal.ONE);
    rate.setLicenceSchedulePhase(phase);
    rate.setRateRelativeDateOption(RateRelativeDateOption.RELATIVE_TO_START_DATE);
    rate.setRelativeDuration(new ThreeFieldDuration(1, 2, 3));

    var result = licenceScheduleRateFormService.getFormFromRate(rate);

    assertThat(result).extracting(
        LicenceScheduleRateForm::getRateDefinitionOption,
        form -> form.getRentalRate().getAsBigDecimal().get(),
        LicenceScheduleRateForm::getComments,
        LicenceScheduleRateForm::getLicenceScheduleTermId,
        LicenceScheduleRateForm::getLicenceSchedulePhaseId,
        LicenceScheduleRateForm::getRelativeEventId,
        LicenceScheduleRateForm::getRateRelativeDateOption
    ).containsExactly(
        rate.getRateDefinitionOption(),
        rate.getRentalRate(),
        null,
        null,
        null,
        rate.getLicenceSchedulePhase().getId().toString(),
        rate.getRateRelativeDateOption()
    );

    var duration = result.getRelativeDuration().toThreeFieldDuration();

    assertThat(duration.days()).isEqualTo(rate.getRelativeDuration().days());
    assertThat(duration.months()).isEqualTo(rate.getRelativeDuration().months());
    assertThat(duration.years()).isEqualTo(rate.getRelativeDuration().years());
  }

  @Test
  void getRateDefinitionOptions_phasesDisabled() {
    when(licenceTypeRulesResolver.arePhasesCaptured(licence.getType())).thenReturn(false);

    var expectedResult = Map.of(
        RateDefinitionOption.TERM.getEnumName(), RateDefinitionOption.TERM.getDisplayName(),
        RateDefinitionOption.CUSTOM_PERIOD.getEnumName(), RateDefinitionOption.CUSTOM_PERIOD.getDisplayName()
    );

    assertThat(licenceScheduleRateFormService.getRateDefinitionOptions(licenceScheduleDetail)).isEqualTo(expectedResult);
  }

  @Test
  void getRateDefinitionOptions_phasesEnabled_noPhases() {
    when(licenceTypeRulesResolver.arePhasesCaptured(licence.getType())).thenReturn(true);
    when(licenceScheduleRelativeOptionsService.getSchedulePhaseOptions(licenceScheduleDetail)).thenReturn(Map.of());

    var expectedResult = Map.of(
        RateDefinitionOption.TERM.getEnumName(), RateDefinitionOption.TERM.getDisplayName(),
        RateDefinitionOption.CUSTOM_PERIOD.getEnumName(), RateDefinitionOption.CUSTOM_PERIOD.getDisplayName()
    );

    assertThat(licenceScheduleRateFormService.getRateDefinitionOptions(licenceScheduleDetail)).isEqualTo(expectedResult);
  }

  @Test
  void getRateDefinitionOptions_phasesEnabled_hasPhases() {
    when(licenceTypeRulesResolver.arePhasesCaptured(licence.getType())).thenReturn(true);

    var phase =  new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());
    phase.setPhaseType(PhaseType.PHASE_A);

    when(licenceScheduleRelativeOptionsService.getSchedulePhaseOptions(licenceScheduleDetail)).thenReturn(
        Map.of(phase.getId().toString(),
        phase.getPhaseType().getDisplayName())
    );

    var expectedResult = Map.of(
        RateDefinitionOption.TERM.getEnumName(), RateDefinitionOption.TERM.getDisplayName(),
        RateDefinitionOption.PHASE.getEnumName(), RateDefinitionOption.PHASE.getDisplayName(),
        RateDefinitionOption.CUSTOM_PERIOD.getEnumName(), RateDefinitionOption.CUSTOM_PERIOD.getDisplayName()
    );

    assertThat(licenceScheduleRateFormService.getRateDefinitionOptions(licenceScheduleDetail)).isEqualTo(expectedResult);
  }
}