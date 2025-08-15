package uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDate;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

@Transactional
@IntegrationTest
class LicenceScheduleCalculationServiceIntegrationTest {

  @Autowired
  private EntityManager em;

  @Autowired
  private LicenceScheduleTermRepository licenceScheduleTermRepository;

  @Autowired
  private LicenceScheduleCalculationService licenceScheduleCalculationService;

  private LicenceScheduleDetail licenceScheduleDetail;

  private LicenceStartDate licenceStartDate;

  private LicenceScheduleTerm licenceScheduleTerm;

  private LicenceScheduleTerm licenceScheduleTerm2;

  private LicenceScheduleTerm licenceScheduleTerm3;

  @Test
  void calculateAndSaveLicenceScheduleDates() {
    createDbBaseline();

    licenceScheduleCalculationService.calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);

    licenceScheduleTerm.setStartDate(licenceStartDate.getStartDate());
    licenceScheduleTerm.setEndDate(LocalDate.of(2025, 12, 31));

    licenceScheduleTerm2.setStartDate(licenceScheduleTerm.getEndDate().plusDays(1));
    licenceScheduleTerm2.setEndDate(licenceScheduleTerm.getEndDate().plusYears(1));

    licenceScheduleTerm3.setStartDate(licenceScheduleTerm2.getEndDate().plusDays(1));
    licenceScheduleTerm3.setEndDate(licenceScheduleTerm2.getEndDate().plusYears(1));

    licenceScheduleCalculationService.calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);

    var expectedTermResult = List.of(licenceScheduleTerm, licenceScheduleTerm2, licenceScheduleTerm3);

    var actualTermResult = licenceScheduleTermRepository.findAll();

    assertThat(actualTermResult)
        .usingRecursiveComparison()
        .ignoringFields("id")
        .isEqualTo(expectedTermResult);
  }

  private void createDbBaseline() {
    licenceScheduleDetail = new LicenceScheduleDetail();

    em.persist(licenceScheduleDetail);

    licenceStartDate = new LicenceStartDate();
    licenceStartDate.setLicenceScheduleDetail(licenceScheduleDetail);
    licenceStartDate.setStartDate(LocalDate.of(2025, 1, 1));

    em.persist(licenceStartDate);

    licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setLicenceScheduleDetail(licenceScheduleDetail);
    licenceScheduleTerm.setTermType(TermType.INITIAL);
    licenceScheduleTerm.setTermDuration(new ThreeFieldDuration(1, 0, 0));

    em.persist(licenceScheduleTerm);

    licenceScheduleTerm2 = new LicenceScheduleTerm();
    licenceScheduleTerm2.setLicenceScheduleDetail(licenceScheduleDetail);
    licenceScheduleTerm2.setTermType(TermType.SECOND);
    licenceScheduleTerm2.setTermDuration(new ThreeFieldDuration(1, 0, 0));

    em.persist(licenceScheduleTerm2);

    licenceScheduleTerm3 = new LicenceScheduleTerm();
    licenceScheduleTerm3.setLicenceScheduleDetail(licenceScheduleDetail);
    licenceScheduleTerm3.setTermType(TermType.THIRD);
    licenceScheduleTerm3.setTermDuration(new ThreeFieldDuration(1, 0, 0));

    em.persist(licenceScheduleTerm3);
    em.flush();
  }
}