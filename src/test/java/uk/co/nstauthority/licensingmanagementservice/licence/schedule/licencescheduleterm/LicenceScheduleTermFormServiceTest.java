package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleTermFormServiceTest {

  @Mock
  private LicenceScheduleTermRepository licenceScheduleTermRepository;

  @Mock
  private LicenceScheduleCalculationService licenceScheduleCalculationService;

  @InjectMocks
  private LicenceScheduleTermFormService licenceScheduleTermFormService;

  @Captor
  private ArgumentCaptor<LicenceScheduleTerm> licenceScheduleTermArgumentCaptor;

  @Test
  void saveTermFromForm() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    var form = new LicenceScheduleTermForm();
    form.setTermType(TermType.INITIAL);
    form.getTermDuration().setYears("1");
    form.getTermDuration().setMonths("0");
    form.getTermDuration().setDays("0");

    licenceScheduleTermFormService.saveTermFromForm(form, licenceScheduleDetail, new LicenceScheduleTerm());

    verify(licenceScheduleTermRepository).save(licenceScheduleTermArgumentCaptor.capture());

    var result = licenceScheduleTermArgumentCaptor.getValue();

    assertThat(result).extracting(
        LicenceScheduleTerm::getLicenceScheduleDetail,
        LicenceScheduleTerm::getTermType,
        LicenceScheduleTerm::getTermDuration
    ).containsExactly(
        licenceScheduleDetail,
        TermType.INITIAL,
        form.getTermDuration().toThreeFieldDuration()
    );

    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  @Test
  void saveTermFromForm_existingTerm_doesntOverwriteEventReference() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    var form = new LicenceScheduleTermForm();
    form.setTermType(TermType.INITIAL);
    form.getTermDuration().setYears("1");
    form.getTermDuration().setMonths("0");
    form.getTermDuration().setDays("0");

    var term = new LicenceScheduleTerm();
    term.setEventReference(UUID.randomUUID());

    licenceScheduleTermFormService.saveTermFromForm(form, licenceScheduleDetail, term);

    verify(licenceScheduleTermRepository).save(licenceScheduleTermArgumentCaptor.capture());

    var result = licenceScheduleTermArgumentCaptor.getValue();

    assertThat(result).extracting(
        LicenceScheduleTerm::getLicenceScheduleDetail,
        LicenceScheduleTerm::getTermType,
        LicenceScheduleTerm::getTermDuration,
        LicenceScheduleTerm::getEventReference
    ).containsExactly(
        licenceScheduleDetail,
        TermType.INITIAL,
        form.getTermDuration().toThreeFieldDuration(),
        term.getEventReference()
    );

    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }
}
