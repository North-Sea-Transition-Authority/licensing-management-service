package uk.co.nstauthority.licensingmanagementservice.licence.overview.responsibleteam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceRepository;

@ExtendWith(MockitoExtension.class)
class LicenceResponsibleTeamServiceTest {

  @Mock
  private LicenceRepository licenceRepository;

  @InjectMocks
  private LicenceResponsibleTeamService licenceResponsibleTeamService;

  @Captor
  private ArgumentCaptor<Licence> licenceCaptor;

  @Test
  void getLicenceResponsibleTeamForm() {
    var licence = new Licence();
    licence.setResponsibleTeam(LicenceTeam.CS_NEW_VENTURES);

    var form = licenceResponsibleTeamService.getLicenceResponsibleTeamForm(licence);

    assertThat(form.getResponsibleTeam()).isEqualTo(licence.getResponsibleTeam());
  }

  @Test
  void saveLicenceResponsibleTeam() {
    var licenceTeam = LicenceTeam.CS_NEW_VENTURES;

    licenceResponsibleTeamService.saveLicenceResponsibleTeam(new Licence(), licenceTeam);

    verify(licenceRepository).save(licenceCaptor.capture());

    assertThat(licenceCaptor.getValue().getResponsibleTeam()).isEqualTo(licenceTeam);
  }
}