package uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateService;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleCalculationServiceTest {

  @Mock
  private LicenceStartDateService licenceStartDateService;

  @Mock
  private LicenceScheduleTermService licenceScheduleTermService;

  @InjectMocks
  private LicenceScheduleCalculationService licenceScheduleCalculationService;

  @Captor
  private ArgumentCaptor<List<LicenceScheduleTerm>> licenceScheduleTermArgumentCaptor;

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

    when(licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail)).thenReturn(licenceStartDate);

    ArrayList<LicenceScheduleTerm> licenceScheduleTerms = new ArrayList<>();
    licenceScheduleTerms.add(licenceScheduleTerm);
    licenceScheduleTerms.add(licenceScheduleTerm2);
    licenceScheduleTerms.add(licenceScheduleTerm3);

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(licenceScheduleTerms);

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
  }

  @Test
  void calculateEndDate_yearDuration() {
    var startDate = LocalDate.of(2025, 1, 1);

    var duration = new ThreeFieldDuration(1, 0, 0);

    assertThat(licenceScheduleCalculationService.calculateEndDate(startDate, duration)).isEqualTo(LocalDate.of(2025, 12, 31));
  }

  @Test
  void calculateEndDate_monthDuration() {
    var startDate = LocalDate.of(2025, 1, 1);

    var duration = new ThreeFieldDuration(0, 1, 0);

    assertThat(licenceScheduleCalculationService.calculateEndDate(startDate, duration)).isEqualTo(LocalDate.of(2025, 1, 31));
  }

  @Test
  void calculateEndDate_dayDuration() {
    var startDate = LocalDate.of(2025, 1, 1);

    var duration = new ThreeFieldDuration(0, 0, 1);

    assertThat(licenceScheduleCalculationService.calculateEndDate(startDate, duration)).isEqualTo(LocalDate.of(2025, 1, 2));
  }

}