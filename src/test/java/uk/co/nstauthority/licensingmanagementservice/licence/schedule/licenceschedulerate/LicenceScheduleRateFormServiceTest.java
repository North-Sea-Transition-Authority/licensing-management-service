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
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleRateFormServiceTest {

  @Mock
  private LicenceScheduleRateRepository licenceScheduleRateRepository;

  @Mock
  private LicenceScheduleTermService licenceScheduleTermService;

  @Mock
  private LicenceSchedulePhaseService licenceSchedulePhaseService;

  @Mock
  private LicenceTypeRulesResolver licenceTypeRulesResolver;

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

    licenceScheduleRateFormService.saveRateFromForm(form, licenceScheduleDetail);

    verify(licenceScheduleRateRepository).save(licenceScheduleRateArgumentCaptor.capture());

    assertThat(licenceScheduleRateArgumentCaptor.getValue()).extracting(
        LicenceScheduleRate::getLicenceScheduleDetail,
        LicenceScheduleRate::getRateDefinitionOption,
        LicenceScheduleRate::getLicenceScheduleTerm,
        LicenceScheduleRate::getLicenceSchedulePhase,
        LicenceScheduleRate::getStartDate,
        LicenceScheduleRate::getRentalRate
    ).containsExactly(
        licenceScheduleDetail,
        RateDefinitionOption.TERM,
        term,
        null,
        null,
        BigDecimal.ONE
    );
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

    licenceScheduleRateFormService.saveRateFromForm(form, licenceScheduleDetail);

    verify(licenceScheduleRateRepository).save(licenceScheduleRateArgumentCaptor.capture());

    assertThat(licenceScheduleRateArgumentCaptor.getValue()).extracting(
        LicenceScheduleRate::getLicenceScheduleDetail,
        LicenceScheduleRate::getRateDefinitionOption,
        LicenceScheduleRate::getLicenceScheduleTerm,
        LicenceScheduleRate::getLicenceSchedulePhase,
        LicenceScheduleRate::getStartDate,
        LicenceScheduleRate::getRentalRate
    ).containsExactly(
        licenceScheduleDetail,
        RateDefinitionOption.PHASE,
        null,
        phase,
        null,
        BigDecimal.ONE
    );
  }

  @Test
  void saveRateFromForm_customPeriodOption() {
    var form = new LicenceScheduleRateForm();
    form.setRateDefinitionOption(RateDefinitionOption.CUSTOM_PERIOD);
    form.getRentalRate().setInputValue("1");

    var testDate = LocalDate.of(2025, 1, 1);
    form.getStartDate().setDate(testDate);

    licenceScheduleRateFormService.saveRateFromForm(form, licenceScheduleDetail);

    verify(licenceScheduleRateRepository).save(licenceScheduleRateArgumentCaptor.capture());

    assertThat(licenceScheduleRateArgumentCaptor.getValue()).extracting(
        LicenceScheduleRate::getLicenceScheduleDetail,
        LicenceScheduleRate::getRateDefinitionOption,
        LicenceScheduleRate::getLicenceScheduleTerm,
        LicenceScheduleRate::getLicenceSchedulePhase,
        LicenceScheduleRate::getStartDate,
        LicenceScheduleRate::getRentalRate
    ).containsExactly(
        licenceScheduleDetail,
        RateDefinitionOption.CUSTOM_PERIOD,
        null,
        null,
        testDate,
        BigDecimal.ONE
    );
  }

  @Test
  void getScheduleTermOptions() {
    var term = new LicenceScheduleTerm();
    term.setId(UUID.randomUUID());
    term.setTermType(TermType.INITIAL);

    var term2 = new LicenceScheduleTerm();
    term2.setId(UUID.randomUUID());
    term2.setTermType(TermType.SECOND);

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term, term2));

    var expectedResult = Map.of(
        term.getId().toString(), term.getTermType().getDisplayName(),
        term2.getId().toString(), term2.getTermType().getDisplayName()
    );

    assertThat(licenceScheduleRateFormService.getScheduleTermOptions(licenceScheduleDetail)).isEqualTo(expectedResult);
  }

  @Test
  void getSchedulePhaseOptions() {
    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());
    phase.setPhaseType(PhaseType.PHASE_A);

    var phase2 = new LicenceSchedulePhase();
    phase2.setId(UUID.randomUUID());
    phase2.setPhaseType(PhaseType.PHASE_B);

    when(licenceSchedulePhaseService.getActivePhasesByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(phase, phase2));

    var expectedResult = Map.of(
        phase.getId().toString(), phase.getPhaseType().getDisplayName(),
        phase2.getId().toString(), phase2.getPhaseType().getDisplayName()
    );

    assertThat(licenceScheduleRateFormService.getSchedulePhaseOptions(licenceScheduleDetail)).isEqualTo(expectedResult);
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
    when(licenceSchedulePhaseService.getActivePhasesByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of());

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

    when(licenceSchedulePhaseService.getActivePhasesByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(phase));

    var expectedResult = Map.of(
        RateDefinitionOption.TERM.getEnumName(), RateDefinitionOption.TERM.getDisplayName(),
        RateDefinitionOption.PHASE.getEnumName(), RateDefinitionOption.PHASE.getDisplayName(),
        RateDefinitionOption.CUSTOM_PERIOD.getEnumName(), RateDefinitionOption.CUSTOM_PERIOD.getDisplayName()
    );

    assertThat(licenceScheduleRateFormService.getRateDefinitionOptions(licenceScheduleDetail)).isEqualTo(expectedResult);
  }
}