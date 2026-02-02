package uk.co.nstauthority.licensingmanagementservice.licence.overview.responsibleteam;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceRepository;

@Service
public class LicenceResponsibleTeamService {

  private final LicenceRepository licenceRepository;

  public LicenceResponsibleTeamService(LicenceRepository licenceRepository) {
    this.licenceRepository = licenceRepository;
  }

  LicenceResponsibleTeamForm getLicenceResponsibleTeamForm(Licence licence) {
    var licenceResponsibleTeamForm = new LicenceResponsibleTeamForm();
    licenceResponsibleTeamForm.setResponsibleTeam(licence.getResponsibleTeam());

    return licenceResponsibleTeamForm;
  }

  @Transactional
  public void saveLicenceResponsibleTeam(Licence licence, LicenceTeam responsibleTeam) {
    licence.setResponsibleTeam(responsibleTeam);
    licenceRepository.save(licence);
  }
}
