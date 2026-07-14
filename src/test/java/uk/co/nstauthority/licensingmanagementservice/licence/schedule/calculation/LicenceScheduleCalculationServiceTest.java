package uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.RateDefinitionOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.RateRelativeDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleCalculationServiceTest {

  @Mock
  private LicenceStartDateService licenceStartDateService;

  @Mock
  private LicenceScheduleTermService licenceScheduleTermService;

  @Mock
  private LicenceSchedulePhaseService licenceSchedulePhaseService;

  @Mock
  private WorkProgrammeActivityService workProgrammeActivityService;

  @Mock
  private LicenceScheduleRateService licenceScheduleRateService;

  @Mock
  private OtherScheduleEventService otherScheduleEventService;

  @InjectMocks
  private LicenceScheduleCalculationService licenceScheduleCalculationService;

  @Captor
  private ArgumentCaptor<List<LicenceScheduleTerm>> licenceScheduleTermArgumentCaptor;

  @Captor
  private ArgumentCaptor<List<LicenceSchedulePhase>> licenceSchedulePhaseArgumentCaptor;

  @Captor
  private ArgumentCaptor<List<WorkProgrammeActivity>> workProgrammeActivityArgumentCaptor;

  @Captor
  private ArgumentCaptor<List<LicenceScheduleRate>> licenceScheduleRateArgumentCaptor;

  @Captor
  private ArgumentCaptor<List<OtherScheduleEvent>> otherScheduleEventArgumentCaptor;

  @Test
  void calculateAndSaveLicenceScheduleDates() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    var licenceStartDate = new LicenceStartDate();
    licenceStartDate.setStartDate(LocalDate.of(2025, 1, 1));

    var licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setTermType(TermType.INITIAL);
    licenceScheduleTerm.setTermDuration(new ThreeFieldDuration(1, 0, 0));

    var licenceScheduleTerm2 = new LicenceScheduleTerm();
    licenceScheduleTerm2.setTermType(TermType.SECOND);
    licenceScheduleTerm2.setTermDuration(new ThreeFieldDuration(1, 0, 0));

    var licenceScheduleTerm3 = new LicenceScheduleTerm();
    licenceScheduleTerm3.setTermType(TermType.THIRD);
    licenceScheduleTerm3.setTermDuration(new ThreeFieldDuration(1, 0, 0));

    var licenceSchedulePhase = new LicenceSchedulePhase();
    licenceSchedulePhase.setPhaseType(PhaseType.PHASE_A);
    licenceSchedulePhase.setPhaseDuration(new ThreeFieldDuration(0, 1, 0));

    when(licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail)).thenReturn(licenceStartDate);

    ArrayList<LicenceScheduleTerm> licenceScheduleTerms = new ArrayList<>();
    licenceScheduleTerms.add(licenceScheduleTerm);
    licenceScheduleTerms.add(licenceScheduleTerm2);
    licenceScheduleTerms.add(licenceScheduleTerm3);

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(licenceScheduleTerms);

    when(licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(licenceSchedulePhase));

    licenceScheduleCalculationService.calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);

    verify(licenceScheduleTermService).saveTerms(licenceScheduleTermArgumentCaptor.capture());

    var termResult = licenceScheduleTermArgumentCaptor.getValue();

    assertThat(termResult.getFirst()).extracting(
        LicenceScheduleTerm::getStartDate,
        LicenceScheduleTerm::getEndDate
    ).containsExactly(
        licenceStartDate.getStartDate(),
        LocalDate.of(2025, 12, 31)
    );

    assertThat(termResult.get(1)).extracting(
        LicenceScheduleTerm::getStartDate,
        LicenceScheduleTerm::getEndDate
    ).containsExactly(
        licenceStartDate.getStartDate().plusYears(1),
        LocalDate.of(2025, 12, 31).plusYears(1)
    );

    assertThat(termResult.get(2)).extracting(
        LicenceScheduleTerm::getStartDate,
        LicenceScheduleTerm::getEndDate
    ).containsExactly(
        licenceStartDate.getStartDate().plusYears(2),
        LocalDate.of(2025, 12, 31).plusYears(2)
    );

    verify(licenceSchedulePhaseService).saveLicenceSchedulePhases(licenceSchedulePhaseArgumentCaptor.capture());

    var phaseResult = licenceSchedulePhaseArgumentCaptor.getValue();

    assertThat(phaseResult.getFirst()).extracting(
        LicenceSchedulePhase::getStartDate,
        LicenceSchedulePhase::getEndDate
    ).containsExactly(
        licenceStartDate.getStartDate(),
        LocalDate.of(2025, 1, 31)
    );
  }

  @Test
  void calculateAndSavePhaseDatesForTerm() {
    var licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setTermType(TermType.INITIAL);
    licenceScheduleTerm.setStartDate(LocalDate.of(2025, 1, 1));

    var licenceSchedulePhase = new LicenceSchedulePhase();
    licenceSchedulePhase.setPhaseType(PhaseType.PHASE_A);
    licenceSchedulePhase.setPhaseDuration(new ThreeFieldDuration(0, 1, 0));

    var licenceSchedulePhase2 = new LicenceSchedulePhase();
    licenceSchedulePhase2.setPhaseType(PhaseType.PHASE_C);
    licenceSchedulePhase2.setPhaseDuration(new ThreeFieldDuration(0, 1, 0));

    licenceScheduleCalculationService.calculateAndSavePhaseDatesForTerm(
        List.of(licenceSchedulePhase, licenceSchedulePhase2),
        licenceScheduleTerm
    );

    verify(licenceSchedulePhaseService).saveLicenceSchedulePhases(licenceSchedulePhaseArgumentCaptor.capture());

    var phaseResult = licenceSchedulePhaseArgumentCaptor.getValue();

    assertThat(phaseResult.getFirst()).extracting(
        LicenceSchedulePhase::getStartDate,
        LicenceSchedulePhase::getEndDate
    ).containsExactly(
        licenceScheduleTerm.getStartDate(),
        LocalDate.of(2025, 1, 31)
    );

    assertThat(phaseResult.get(1)).extracting(
        LicenceSchedulePhase::getStartDate,
        LicenceSchedulePhase::getEndDate
    ).containsExactly(
        licenceScheduleTerm.getStartDate().plusMonths(1),
        LocalDate.of(2025, 2, 28)
    );
  }

  @Test
  void calculateAndSavePhaseDatesForTermNoPhasesToCalculate() {
    var licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setTermType(TermType.SECOND);
    licenceScheduleTerm.setStartDate(LocalDate.of(2025, 1, 1));

    var licenceSchedulePhase = new LicenceSchedulePhase();
    licenceSchedulePhase.setPhaseType(PhaseType.PHASE_A);
    licenceSchedulePhase.setPhaseDuration(new ThreeFieldDuration(0, 1, 0));

    var licenceSchedulePhase2 = new LicenceSchedulePhase();
    licenceSchedulePhase2.setPhaseType(PhaseType.PHASE_C);
    licenceSchedulePhase2.setPhaseDuration(new ThreeFieldDuration(0, 1, 0));

    licenceScheduleCalculationService.calculateAndSavePhaseDatesForTerm(
        List.of(licenceSchedulePhase, licenceSchedulePhase2),
        licenceScheduleTerm
    );

    verify(licenceSchedulePhaseService, never()).saveLicenceSchedulePhases(any());
  }

  @Test
  void calculateAndSaveWorkProgrammeActivityDatesForTerm() {
    var licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setStartDate(LocalDate.of(2025, 1, 1));

    var activity = new WorkProgrammeActivity();
    activity.setRelativeDuration(new ThreeFieldDuration(0, 1, 0));

    var activity2 = new WorkProgrammeActivity();
    activity2.setRelativeDuration(new ThreeFieldDuration(1, 0, 1));

    when(workProgrammeActivityService.getWorkProgrammeActivitiesByTermAndDateOption(
        licenceScheduleTerm,
        WorkProgrammeActivityDateOption.RELATIVE_DATE
    ))
        .thenReturn(List.of(activity, activity2));

    licenceScheduleCalculationService.calculateAndSaveWorkProgrammeActivityDatesForTerm(licenceScheduleTerm);

    verify(workProgrammeActivityService).saveWorkProgrammeActivities(workProgrammeActivityArgumentCaptor.capture());

    var result = workProgrammeActivityArgumentCaptor.getValue();

    assertThat(result.getFirst()).extracting(WorkProgrammeActivity::getDueDate).isEqualTo(LocalDate.of(2025, 2, 1));
    assertThat(result.get(1)).extracting(WorkProgrammeActivity::getDueDate).isEqualTo(LocalDate.of(2026, 1, 2));
  }

  @Test
  void calculateAndSaveWorkProgrammeActivityDatesForTerm_noActivitiesToCalculate() {
    var licenceScheduleTerm = new LicenceScheduleTerm();

    when(workProgrammeActivityService.getWorkProgrammeActivitiesByTermAndDateOption(
        licenceScheduleTerm,
        WorkProgrammeActivityDateOption.RELATIVE_DATE
    ))
        .thenReturn(List.of());

    licenceScheduleCalculationService.calculateAndSaveWorkProgrammeActivityDatesForTerm(licenceScheduleTerm);

    verify(workProgrammeActivityService, never()).saveWorkProgrammeActivities(any());
  }

  @Test
  void calculateAndSaveWorkProgrammeActivityDatesForPhase() {
    var licenceSchedulePhase = new LicenceSchedulePhase();
    licenceSchedulePhase.setStartDate(LocalDate.of(2025, 1, 1));

    var activity = new WorkProgrammeActivity();
    activity.setRelativeDuration(new ThreeFieldDuration(0, 1, 0));

    var activity2 = new WorkProgrammeActivity();
    activity2.setRelativeDuration(new ThreeFieldDuration(0, 0, 1));

    when(workProgrammeActivityService.getWorkProgrammeActivitiesByPhaseAndDateOption(
        licenceSchedulePhase,
        WorkProgrammeActivityDateOption.RELATIVE_DATE
    ))
        .thenReturn(List.of(activity, activity2));

    licenceScheduleCalculationService.calculateAndSaveWorkProgrammeActivityDatesForPhase(licenceSchedulePhase);

    verify(workProgrammeActivityService).saveWorkProgrammeActivities(workProgrammeActivityArgumentCaptor.capture());

    var result = workProgrammeActivityArgumentCaptor.getValue();

    assertThat(result.getFirst()).extracting(WorkProgrammeActivity::getDueDate).isEqualTo(LocalDate.of(2025, 2, 1));
    assertThat(result.get(1)).extracting(WorkProgrammeActivity::getDueDate).isEqualTo(LocalDate.of(2025, 1, 2));
  }

  @Test
  void calculateAndSaveWorkProgrammeActivityDatesForPhase_noActivitiesToCalculate() {
    var licenceSchedulePhase = new LicenceSchedulePhase();

    when(workProgrammeActivityService.getWorkProgrammeActivitiesByPhaseAndDateOption(
        licenceSchedulePhase,
        WorkProgrammeActivityDateOption.RELATIVE_DATE
    ))
        .thenReturn(List.of());

    licenceScheduleCalculationService.calculateAndSaveWorkProgrammeActivityDatesForPhase(licenceSchedulePhase);

    verify(workProgrammeActivityService, never()).saveWorkProgrammeActivities(any());
  }

  @Test
  void calculateAndSaveRateStartDatesForTerm_linkedDates() {
    var licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setStartDate(LocalDate.of(2025, 1, 1));

    var rate = new LicenceScheduleRate();

    when(licenceScheduleRateService.getLicenceScheduleRatesForTermAndDefinitionOption(licenceScheduleTerm, RateDefinitionOption.TERM))
        .thenReturn(List.of(rate));

    when(licenceScheduleRateService.getLicenceScheduleRatesForTermAndDefinitionOption(licenceScheduleTerm, RateDefinitionOption.CUSTOM_PERIOD))
        .thenReturn(List.of());

    licenceScheduleCalculationService.calculateAndSaveRateStartDatesForTerm(licenceScheduleTerm);

    verify(licenceScheduleRateService, times(2)).saveLicenceScheduleRates(licenceScheduleRateArgumentCaptor.capture());

    var result = licenceScheduleRateArgumentCaptor.getAllValues();

    var firstResult = result.getFirst();
    assertThat(firstResult.getFirst()).extracting(LicenceScheduleRate::getStartDate).isEqualTo(licenceScheduleTerm.getStartDate());

    var secondResult = result.get(1);
    assertThat(secondResult).isEmpty();
  }

  @Test
  void calculateAndSaveRateStartDatesForTerm_relativeDates() {
    var licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setStartDate(LocalDate.of(2025, 1, 1));

    var startDateRate = new LicenceScheduleRate();
    startDateRate.setRateRelativeDateOption(RateRelativeDateOption.ON_START_DATE);

    var relativeDateRate = new LicenceScheduleRate();
    relativeDateRate.setRateRelativeDateOption(RateRelativeDateOption.RELATIVE_TO_START_DATE);
    relativeDateRate.setRelativeDuration(new ThreeFieldDuration(0, 1, 0));

    when(licenceScheduleRateService.getLicenceScheduleRatesForTermAndDefinitionOption(licenceScheduleTerm, RateDefinitionOption.TERM))
        .thenReturn(List.of());

    when(licenceScheduleRateService.getLicenceScheduleRatesForTermAndDefinitionOption(licenceScheduleTerm, RateDefinitionOption.CUSTOM_PERIOD))
        .thenReturn(List.of(startDateRate, relativeDateRate));

    licenceScheduleCalculationService.calculateAndSaveRateStartDatesForTerm(licenceScheduleTerm);

    verify(licenceScheduleRateService, times(2)).saveLicenceScheduleRates(licenceScheduleRateArgumentCaptor.capture());

    var result = licenceScheduleRateArgumentCaptor.getAllValues();

    var firstResult = result.getFirst();
    assertThat(firstResult).isEmpty();

    var secondResult = result.get(1);
    assertThat(secondResult.getFirst()).extracting(LicenceScheduleRate::getStartDate).isEqualTo(licenceScheduleTerm.getStartDate());
    assertThat(secondResult.get(1)).extracting(LicenceScheduleRate::getStartDate).isEqualTo(licenceScheduleTerm.getStartDate().plusMonths(1));
  }

  @Test
  void calculateAndSaveRateStartDatesForPhase_linkedDates() {
    var licenceSchedulePhase = new LicenceSchedulePhase();
    licenceSchedulePhase.setStartDate(LocalDate.of(2025, 1, 1));

    var rate = new LicenceScheduleRate();

    when(licenceScheduleRateService.getLicenceScheduleRatesForPhaseAndDefinitionOption(licenceSchedulePhase, RateDefinitionOption.PHASE))
        .thenReturn(List.of(rate));

    when(licenceScheduleRateService.getLicenceScheduleRatesForPhaseAndDefinitionOption(licenceSchedulePhase, RateDefinitionOption.CUSTOM_PERIOD))
        .thenReturn(List.of());

    licenceScheduleCalculationService.calculateAndSaveRateStartDatesForPhase(licenceSchedulePhase);

    verify(licenceScheduleRateService, times(2)).saveLicenceScheduleRates(licenceScheduleRateArgumentCaptor.capture());

    var result = licenceScheduleRateArgumentCaptor.getAllValues();

    var firstResult = result.getFirst();
    assertThat(firstResult.getFirst()).extracting(LicenceScheduleRate::getStartDate).isEqualTo(licenceSchedulePhase.getStartDate());

    var secondResult = result.get(1);
    assertThat(secondResult).isEmpty();
  }

  @Test
  void calculateAndSaveRateStartDatesForPhase_relativeDates() {
    var licenceSchedulePhase = new LicenceSchedulePhase();
    licenceSchedulePhase.setStartDate(LocalDate.of(2025, 1, 1));

    var startDateRate = new LicenceScheduleRate();
    startDateRate.setRateRelativeDateOption(RateRelativeDateOption.ON_START_DATE);

    var relativeDateRate = new LicenceScheduleRate();
    relativeDateRate.setRateRelativeDateOption(RateRelativeDateOption.RELATIVE_TO_START_DATE);
    relativeDateRate.setRelativeDuration(new ThreeFieldDuration(0, 1, 0));

    when(licenceScheduleRateService.getLicenceScheduleRatesForPhaseAndDefinitionOption(licenceSchedulePhase, RateDefinitionOption.PHASE))
        .thenReturn(List.of());

    when(licenceScheduleRateService.getLicenceScheduleRatesForPhaseAndDefinitionOption(licenceSchedulePhase, RateDefinitionOption.CUSTOM_PERIOD))
        .thenReturn(List.of(startDateRate, relativeDateRate));

    licenceScheduleCalculationService.calculateAndSaveRateStartDatesForPhase(licenceSchedulePhase);

    verify(licenceScheduleRateService, times(2)).saveLicenceScheduleRates(licenceScheduleRateArgumentCaptor.capture());

    var result = licenceScheduleRateArgumentCaptor.getAllValues();

    var firstResult = result.getFirst();
    assertThat(firstResult).isEmpty();

    var secondResult = result.get(1);
    assertThat(secondResult.getFirst()).extracting(LicenceScheduleRate::getStartDate).isEqualTo(licenceSchedulePhase.getStartDate());
    assertThat(secondResult.get(1)).extracting(LicenceScheduleRate::getStartDate).isEqualTo(licenceSchedulePhase.getStartDate().plusMonths(1));
  }

  @Test
  void calculateAndSaveOtherScheduleEventDatesForTerm() {
    var licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setStartDate(LocalDate.of(2025, 1, 1));

    var event = new OtherScheduleEvent();
    event.setRelativeDuration(new ThreeFieldDuration(0, 1, 0));

    var event2 = new OtherScheduleEvent();
    event2.setRelativeDuration(new ThreeFieldDuration(1, 0, 1));

    when(otherScheduleEventService.getScheduleEventsByTermAndDateOption(
        licenceScheduleTerm,
        OtherScheduleEventDateOption.RELATIVE_DATE
    ))
        .thenReturn(List.of(event, event2));

    licenceScheduleCalculationService.calculateAndSaveOtherScheduleEventDatesForTerm(licenceScheduleTerm);

    verify(otherScheduleEventService).saveScheduleEvents(otherScheduleEventArgumentCaptor.capture());

    var result = otherScheduleEventArgumentCaptor.getValue();

    assertThat(result.getFirst()).extracting(OtherScheduleEvent::getEventDate).isEqualTo(LocalDate.of(2025, 2, 1));
    assertThat(result.get(1)).extracting(OtherScheduleEvent::getEventDate).isEqualTo(LocalDate.of(2026, 1, 2));
  }

  @Test
  void calculateAndSaveOtherScheduleEventDatesForTerm_noActivitiesToCalculate() {
    var licenceScheduleTerm = new LicenceScheduleTerm();

    when(otherScheduleEventService.getScheduleEventsByTermAndDateOption(
        licenceScheduleTerm,
        OtherScheduleEventDateOption.RELATIVE_DATE
    ))
        .thenReturn(List.of());

    licenceScheduleCalculationService.calculateAndSaveOtherScheduleEventDatesForTerm(licenceScheduleTerm);

    verify(otherScheduleEventService, never()).saveScheduleEvents(any());
  }

  @Test
  void calculateAndSaveOtherScheduleEventDatesForPhase() {
    var licenceSchedulePhase = new LicenceSchedulePhase();
    licenceSchedulePhase.setStartDate(LocalDate.of(2025, 1, 1));

    var event = new OtherScheduleEvent();
    event.setRelativeDuration(new ThreeFieldDuration(0, 1, 0));

    var event2 = new OtherScheduleEvent();
    event2.setRelativeDuration(new ThreeFieldDuration(0, 0, 1));

    when(otherScheduleEventService.getScheduleEventsByPhaseAndDateOption(
        licenceSchedulePhase,
        OtherScheduleEventDateOption.RELATIVE_DATE
    ))
        .thenReturn(List.of(event, event2));

    licenceScheduleCalculationService.calculateAndSaveOtherScheduleEventDatesForPhase(licenceSchedulePhase);

    verify(otherScheduleEventService).saveScheduleEvents(otherScheduleEventArgumentCaptor.capture());

    var result = otherScheduleEventArgumentCaptor.getValue();

    assertThat(result.getFirst()).extracting(OtherScheduleEvent::getEventDate).isEqualTo(LocalDate.of(2025, 2, 1));
    assertThat(result.get(1)).extracting(OtherScheduleEvent::getEventDate).isEqualTo(LocalDate.of(2025, 1, 2));
  }

  @Test
  void calculateAndSaveOtherScheduleEventDatesForPhase_noActivitiesToCalculate() {
    var licenceSchedulePhase = new LicenceSchedulePhase();

    when(otherScheduleEventService.getScheduleEventsByPhaseAndDateOption(
        licenceSchedulePhase,
        OtherScheduleEventDateOption.RELATIVE_DATE
    ))
        .thenReturn(List.of());

    licenceScheduleCalculationService.calculateAndSaveOtherScheduleEventDatesForPhase(licenceSchedulePhase);

    verify(otherScheduleEventService, never()).saveScheduleEvents(any());
  }

  @Test
  void calculateDurationEndDate_yearDuration() {
    var startDate = LocalDate.of(2025, 1, 1);

    var duration = new ThreeFieldDuration(1, 0, 0);

    assertThat(licenceScheduleCalculationService.calculateDurationEndDate(startDate, duration)).isEqualTo(LocalDate.of(2025, 12, 31));
  }

  @Test
  void calculateDurationEndDate_monthDuration() {
    var startDate = LocalDate.of(2025, 1, 1);

    var duration = new ThreeFieldDuration(0, 1, 0);

    assertThat(licenceScheduleCalculationService.calculateDurationEndDate(startDate, duration)).isEqualTo(LocalDate.of(2025, 1, 31));
  }

  @Test
  void calculateDurationEndDate_dayDuration() {
    var startDate = LocalDate.of(2025, 1, 1);

    var duration = new ThreeFieldDuration(0, 0, 1);

    assertThat(licenceScheduleCalculationService.calculateDurationEndDate(startDate, duration)).isEqualTo(LocalDate.of(2025, 1, 2));
  }

  @Test
  void calculateRelativeStartDueDate_yearDuration() {
    var startDate = LocalDate.of(2025, 1, 1);

    var duration = new ThreeFieldDuration(1, 0, 0);

    assertThat(licenceScheduleCalculationService.calculateRelativeStartDueDate(startDate, duration)).isEqualTo(LocalDate.of(2026, 1, 1));
  }

  @Test
  void calculateRelativeStartDueDate_monthDuration() {
    var startDate = LocalDate.of(2025, 1, 1);

    var duration = new ThreeFieldDuration(0, 1, 0);

    assertThat(licenceScheduleCalculationService.calculateRelativeStartDueDate(startDate, duration)).isEqualTo(LocalDate.of(2025, 2, 1));
  }

  @Test
  void calculateRelativeStartDueDate_dayDuration() {
    var startDate = LocalDate.of(2025, 1, 1);

    var duration = new ThreeFieldDuration(0, 0, 1);

    assertThat(licenceScheduleCalculationService.calculateRelativeStartDueDate(startDate, duration)).isEqualTo(LocalDate.of(2025, 1, 2));
  }

  @Test
  void calculateRateEndDatesForDisplay_whenNoRates_thenEmptyMapReturned() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    when(licenceScheduleRateService.getLicenceScheduleRates(licenceScheduleDetail)).thenReturn(List.of());

    var result = licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail);

    assertThat(result).isEmpty();
  }

  @Test
  void calculateRateEndDatesForDisplay_whenSingleTermRate_thenStartDateFromTermAndEndDateFromFinalTerm() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    var term = new LicenceScheduleTerm();
    term.setTermType(TermType.INITIAL);
    term.setStartDate(LocalDate.of(2025, 1, 1));
    term.setEndDate(LocalDate.of(2025, 12, 31));

    var rate = new LicenceScheduleRate();
    rate.setId(UUID.randomUUID());
    rate.setRateDefinitionOption(RateDefinitionOption.TERM);
    rate.setLicenceScheduleTerm(term);

    when(licenceScheduleRateService.getLicenceScheduleRates(licenceScheduleDetail)).thenReturn(List.of(rate));
    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(new ArrayList<>(List.of(term)));

    var result = licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail);

    assertThat(result).containsOnlyKeys(rate.getId());
    assertThat(result.get(rate.getId())).extracting(
        StartEndDates::startDate,
        StartEndDates::endDate
    ).containsExactly(
        LocalDate.of(2025, 1, 1),
        LocalDate.of(2025, 12, 31)
    );
  }

  @Test
  void calculateRateEndDatesForDisplay_whenSinglePhaseRate_thenStartDateFromPhaseAndEndDateFromFinalTerm() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    var term = new LicenceScheduleTerm();
    term.setTermType(TermType.INITIAL);
    term.setEndDate(LocalDate.of(2025, 12, 31));

    var phase = new LicenceSchedulePhase();
    phase.setStartDate(LocalDate.of(2025, 1, 1));
    phase.setEndDate(LocalDate.of(2025, 6, 30));

    var rate = new LicenceScheduleRate();
    rate.setId(UUID.randomUUID());
    rate.setRateDefinitionOption(RateDefinitionOption.PHASE);
    rate.setLicenceSchedulePhase(phase);

    when(licenceScheduleRateService.getLicenceScheduleRates(licenceScheduleDetail)).thenReturn(List.of(rate));
    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(new ArrayList<>(List.of(term)));

    var result = licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail);

    assertThat(result).containsOnlyKeys(rate.getId());
    assertThat(result.get(rate.getId())).extracting(
        StartEndDates::startDate,
        StartEndDates::endDate
    ).containsExactly(
        LocalDate.of(2025, 1, 1),
        LocalDate.of(2025, 12, 31)
    );
  }

  @Test
  void calculateRateEndDatesForDisplay_whenSingleCustomPeriodRate_thenStartDateFromRateAndEndDateFromFinalTerm() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    var term = new LicenceScheduleTerm();
    term.setTermType(TermType.INITIAL);
    term.setEndDate(LocalDate.of(2025, 12, 31));

    var rate = new LicenceScheduleRate();
    rate.setId(UUID.randomUUID());
    rate.setRateDefinitionOption(RateDefinitionOption.CUSTOM_PERIOD);
    rate.setStartDate(LocalDate.of(2025, 3, 1));

    when(licenceScheduleRateService.getLicenceScheduleRates(licenceScheduleDetail)).thenReturn(List.of(rate));
    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(new ArrayList<>(List.of(term)));

    var result = licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail);

    assertThat(result).containsOnlyKeys(rate.getId());
    assertThat(result.get(rate.getId())).extracting(
        StartEndDates::startDate,
        StartEndDates::endDate
    ).containsExactly(
        LocalDate.of(2025, 3, 1),
        LocalDate.of(2025, 12, 31)
    );
  }

  @Test
  void calculateRateEndDatesForDisplay_whenMultipleTermRates_thenNonFinalEndDateIsTermEndDate() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    var term1 = new LicenceScheduleTerm();
    term1.setTermType(TermType.INITIAL);
    term1.setStartDate(LocalDate.of(2025, 1, 1));
    term1.setEndDate(LocalDate.of(2025, 12, 31));

    var term2 = new LicenceScheduleTerm();
    term2.setTermType(TermType.SECOND);
    term2.setStartDate(LocalDate.of(2026, 1, 1));
    term2.setEndDate(LocalDate.of(2026, 12, 31));

    var rate1 = new LicenceScheduleRate();
    rate1.setId(UUID.randomUUID());
    rate1.setRateDefinitionOption(RateDefinitionOption.TERM);
    rate1.setLicenceScheduleTerm(term1);

    var rate2 = new LicenceScheduleRate();
    rate2.setId(UUID.randomUUID());
    rate2.setRateDefinitionOption(RateDefinitionOption.TERM);
    rate2.setLicenceScheduleTerm(term2);

    when(licenceScheduleRateService.getLicenceScheduleRates(licenceScheduleDetail)).thenReturn(List.of(rate1, rate2));
    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(new ArrayList<>(List.of(term1, term2)));

    var result = licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail);

    assertThat(result.get(rate1.getId())).extracting(
        StartEndDates::startDate,
        StartEndDates::endDate
    ).containsExactly(
        LocalDate.of(2025, 1, 1),
        LocalDate.of(2025, 12, 31)
    );
    assertThat(result.get(rate2.getId())).extracting(
        StartEndDates::startDate,
        StartEndDates::endDate
    ).containsExactly(
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 12, 31)
    );
  }

  @Test
  void calculateRateEndDatesForDisplay_whenMultiplePhaseRates_thenNonFinalEndDateIsPhaseEndDate() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    var term = new LicenceScheduleTerm();
    term.setTermType(TermType.INITIAL);
    term.setEndDate(LocalDate.of(2025, 12, 31));

    var phase1 = new LicenceSchedulePhase();
    phase1.setStartDate(LocalDate.of(2025, 1, 1));
    phase1.setEndDate(LocalDate.of(2025, 6, 30));

    var phase2 = new LicenceSchedulePhase();
    phase2.setStartDate(LocalDate.of(2025, 7, 1));
    phase2.setEndDate(LocalDate.of(2025, 12, 31));

    var rate1 = new LicenceScheduleRate();
    rate1.setId(UUID.randomUUID());
    rate1.setRateDefinitionOption(RateDefinitionOption.PHASE);
    rate1.setLicenceSchedulePhase(phase1);

    var rate2 = new LicenceScheduleRate();
    rate2.setId(UUID.randomUUID());
    rate2.setRateDefinitionOption(RateDefinitionOption.PHASE);
    rate2.setLicenceSchedulePhase(phase2);

    when(licenceScheduleRateService.getLicenceScheduleRates(licenceScheduleDetail)).thenReturn(List.of(rate1, rate2));
    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(new ArrayList<>(List.of(term)));

    var result = licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail);

    assertThat(result.get(rate1.getId())).extracting(
        StartEndDates::startDate,
        StartEndDates::endDate
    ).containsExactly(
        LocalDate.of(2025, 1, 1),
        LocalDate.of(2025, 6, 30)
    );
    assertThat(result.get(rate2.getId())).extracting(
        StartEndDates::startDate,
        StartEndDates::endDate
    ).containsExactly(
        LocalDate.of(2025, 7, 1),
        LocalDate.of(2025, 12, 31)
    );
  }

  @Test
  void calculateRateEndDatesForDisplay_whenNonFinalCustomPeriodRate_thenEndDateIsDayBeforeNextRateStartDate() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    var term = new LicenceScheduleTerm();
    term.setTermType(TermType.INITIAL);
    term.setStartDate(LocalDate.of(2025, 7, 1));
    term.setEndDate(LocalDate.of(2025, 12, 31));

    var customRate = new LicenceScheduleRate();
    customRate.setId(UUID.randomUUID());
    customRate.setRateDefinitionOption(RateDefinitionOption.CUSTOM_PERIOD);
    customRate.setStartDate(LocalDate.of(2025, 1, 1));

    var termRate = new LicenceScheduleRate();
    termRate.setId(UUID.randomUUID());
    termRate.setRateDefinitionOption(RateDefinitionOption.TERM);
    termRate.setLicenceScheduleTerm(term);

    when(licenceScheduleRateService.getLicenceScheduleRates(licenceScheduleDetail))
        .thenReturn(List.of(customRate, termRate));
    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(new ArrayList<>(List.of(term)));

    var result = licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail);

    assertThat(result.get(customRate.getId())).extracting(
        StartEndDates::startDate,
        StartEndDates::endDate
    ).containsExactly(
        LocalDate.of(2025, 1, 1),
        LocalDate.of(2025, 6, 30)
    );
  }

  @Test
  void calculateRateEndDatesForDisplay_whenRatesReturnedOutOfOrder_thenResultSortedByStartDate() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    var term1 = new LicenceScheduleTerm();
    term1.setTermType(TermType.INITIAL);
    term1.setStartDate(LocalDate.of(2025, 1, 1));
    term1.setEndDate(LocalDate.of(2025, 12, 31));

    var term2 = new LicenceScheduleTerm();
    term2.setTermType(TermType.SECOND);
    term2.setStartDate(LocalDate.of(2026, 1, 1));
    term2.setEndDate(LocalDate.of(2026, 12, 31));

    var rate1 = new LicenceScheduleRate();
    rate1.setId(UUID.randomUUID());
    rate1.setRateDefinitionOption(RateDefinitionOption.TERM);
    rate1.setLicenceScheduleTerm(term1);

    var rate2 = new LicenceScheduleRate();
    rate2.setId(UUID.randomUUID());
    rate2.setRateDefinitionOption(RateDefinitionOption.TERM);
    rate2.setLicenceScheduleTerm(term2);

    when(licenceScheduleRateService.getLicenceScheduleRates(licenceScheduleDetail))
        .thenReturn(List.of(rate2, rate1));
    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(new ArrayList<>(List.of(term1, term2)));

    var result = licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail);

    assertThat(result.keySet()).containsExactly(rate1.getId(), rate2.getId());
  }

}