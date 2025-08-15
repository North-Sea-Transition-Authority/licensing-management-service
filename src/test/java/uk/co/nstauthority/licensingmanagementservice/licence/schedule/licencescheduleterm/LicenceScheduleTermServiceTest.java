package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleTermServiceTest {

  @Mock
  private LicenceScheduleTermRepository licenceScheduleTermRepository;

  @InjectMocks
  private LicenceScheduleTermService licenceScheduleTermService;

  @Captor
  private ArgumentCaptor<LicenceScheduleTerm> licenceScheduleTermArgumentCaptor;

  @Test
  void getTermsByLicenceScheduleDetail() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail);

    verify(licenceScheduleTermRepository).findByLicenceScheduleDetail(licenceScheduleDetail);
  }

  @Test
  void saveTermFromForm() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    var form = new LicenceScheduleTermForm();
    form.setTermType(TermType.INITIAL);
    form.getTermDuration().setYears("1");
    form.getTermDuration().setMonths("0");
    form.getTermDuration().setDays("0");

    licenceScheduleTermService.saveTermFromForm(form, licenceScheduleDetail);

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
  }
}