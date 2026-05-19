package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleTermServiceTest {

  @Mock
  private LicenceScheduleTermRepository licenceScheduleTermRepository;

  @InjectMocks
  private LicenceScheduleTermService licenceScheduleTermService;

  @Test
  void getActiveTermsByLicenceScheduleDetail() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail);

    verify(licenceScheduleTermRepository).findAllByLicenceScheduleDetail(licenceScheduleDetail);
  }

  @Test
  void saveTerms() {
    var terms = List.of(new LicenceScheduleTerm());

    licenceScheduleTermService.saveTerms(terms);

    verify(licenceScheduleTermRepository).saveAll(terms);
  }

  @Test
  void getTermByIdOrThrow() {
    var licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setId(UUID.randomUUID());

    when(licenceScheduleTermRepository.findById(licenceScheduleTerm.getId())).thenReturn(Optional.of(licenceScheduleTerm));

    assertThat(licenceScheduleTermService.getTermByIdOrThrow(licenceScheduleTerm.getId())).isEqualTo(licenceScheduleTerm);
  }

  @Test
  void getTermByIdOrThrow_termNotFound() {
    when(licenceScheduleTermRepository.findById(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> licenceScheduleTermService.getTermByIdOrThrow(UUID.randomUUID()))
        .isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void deleteTerm() {
    var licenceScheduleTerm = new LicenceScheduleTerm();

    licenceScheduleTermService.deleteTerm(licenceScheduleTerm);

    verify(licenceScheduleTermRepository).delete(licenceScheduleTerm);
  }
}
