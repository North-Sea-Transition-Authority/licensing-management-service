package migration.carbonstorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateService;
import uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage.CarbonStorageLicenceMigrationExtract;
import uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage.CarbonStorageLicenceMigrationExtractRepository;
import uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage.CarbonStorageLicenceMigrationService;
import uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage.CarbonStorageLicenceOrgMapping;
import uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage.CarbonStorageLicenceOrgMappingRepository;
import uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage.CarbonStorageStartDateMigrationRepository;
import uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage.CarbonStorageTermMigrationRepository;

@ExtendWith(MockitoExtension.class)
class CarbonStorageLicenceMigrationServiceTest {

  @Mock
  private LicenceService licenceService;

  @Mock
  private LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  @Mock
  private LicenceScheduleService licenceScheduleService;

  @Mock
  private LicenceScheduleDetailService licenceScheduleDetailService;

  @Mock
  private LicenceStartDateService licenceStartDateService;

  @Mock
  private LicenceScheduleTermService licenceScheduleTermService;

  @Mock
  private LicenceScheduleCalculationService licenceScheduleCalculationService;

  @Mock
  private CarbonStorageLicenceMigrationExtractRepository carbonStorageLicenceMigrationExtractRepository;

  @Mock
  private CarbonStorageLicenceOrgMappingRepository carbonStorageLicenceOrgMappingRepository;

  @Mock
  private CarbonStorageStartDateMigrationRepository carbonStorageStartDateMigrationRepository;

  @Mock
  private CarbonStorageTermMigrationRepository carbonStorageTermMigrationRepository;

  @Captor
  private ArgumentCaptor<Collection<Licence>> licenceArgumentCaptor;

  @Captor
  private ArgumentCaptor<List<LicenceResponsibleOrganisation>> licenceResponsibleOrganisationArgumentCaptor;

  @Captor
  private ArgumentCaptor<List<LicenceSchedule>> licenceScheduleArgumentCaptor;

  @Captor
  private ArgumentCaptor<List<LicenceScheduleDetail>> licenceScheduleDetailArgumentCaptor;

  @Captor
  private ArgumentCaptor<List<LicenceStartDate>> licenceStartDateArgumentCaptor;

  @Captor
  private ArgumentCaptor<List<LicenceScheduleTerm>> licenceScheduleTermArgumentCaptor;

  @InjectMocks
  private CarbonStorageLicenceMigrationService carbonStorageLicenceMigrationService;

  @Test
  void testMigrate() {
    CarbonStorageLicenceMigrationExtract extract = new CarbonStorageLicenceMigrationExtract();
    extract.setResponsibleOrgs("org1,org2");
    extract.setLicenceNumber("123");
    extract.setLicenceRef("CS123");

    when(carbonStorageLicenceMigrationExtractRepository.findAll()).thenReturn(List.of(extract));

    CarbonStorageLicenceOrgMapping mapping1 = new CarbonStorageLicenceOrgMapping();
    mapping1.setOrganisationUnitId(1);
    CarbonStorageLicenceOrgMapping mapping2 = new CarbonStorageLicenceOrgMapping();
    mapping2.setOrganisationUnitId(2);

    when(carbonStorageLicenceOrgMappingRepository.findByCsExtractResponsibleOrganisation("org1"))
        .thenReturn(mapping1);
    when(carbonStorageLicenceOrgMappingRepository.findByCsExtractResponsibleOrganisation("org2"))
        .thenReturn(mapping2);

//    CarbonStorageStartDateMigrationExtract startDateExtract = new CarbonStorageStartDateMigrationExtract();
//    startDateExtract.setLicenceRef("CS123");
//    var startDateString = "01/01/2020";
//    startDateExtract.setStartDate(startDateString);
//
//    when(carbonStorageStartDateMigrationRepository.findAll()).thenReturn(List.of(startDateExtract));
//
//    CarbonStorageTermMigrationExtract term1 = new CarbonStorageTermMigrationExtract();
//    term1.setLicenceRef("CS123");
//    term1.setTerm("Appraisal");
//    term1.setDays(0);
//    term1.setMonths(6);
//    term1.setYears(1);
//
//    CarbonStorageTermMigrationExtract term2 = new CarbonStorageTermMigrationExtract();
//    term2.setLicenceRef("CS123");
//    term2.setTerm("Operational");
//    term2.setDays(1);
//    term2.setMonths(6);
//    term2.setYears(2);
//
//    var terms = List.of(term1, term2);
//
//    when(carbonStorageTermMigrationRepository.findAll()).thenReturn(terms);

    carbonStorageLicenceMigrationService.migrate();

    verify(licenceService, times(1))
        .saveLicences(licenceArgumentCaptor.capture());
    verify(licenceResponsibleOrganisationService, times(1))
        .saveLicensees(licenceResponsibleOrganisationArgumentCaptor.capture());
//    verify(licenceScheduleService, times(1))
//        .saveLicenceSchedules(licenceScheduleArgumentCaptor.capture());
//    verify(licenceScheduleDetailService, times(1))
//        .saveLicenceScheduleDetails(licenceScheduleDetailArgumentCaptor.capture());
//    verify(licenceStartDateService, times(1))
//        .saveLicenceStartDates(licenceStartDateArgumentCaptor.capture());
//    verify(licenceScheduleTermService, times(1))
//        .saveTerms(licenceScheduleTermArgumentCaptor.capture());

    Collection<Licence> licences = licenceArgumentCaptor.getValue();
    List<LicenceResponsibleOrganisation> responsibleOrgs = licenceResponsibleOrganisationArgumentCaptor.getValue();
//    List<LicenceSchedule> licenceSchedules = licenceScheduleArgumentCaptor.getValue();
//    List<LicenceScheduleDetail> licenceScheduleDetails = licenceScheduleDetailArgumentCaptor.getValue();
//    List<LicenceStartDate> licenceStartDates = licenceStartDateArgumentCaptor.getValue();
//    List<LicenceScheduleTerm> licenceScheduleTerms = licenceScheduleTermArgumentCaptor.getValue();

//    verify(licenceScheduleCalculationService, times(terms.size()));

    assertEquals(1, licences.size());
    var licence1 = licences.iterator().next();
    assertEquals("CS", licence1.getPrefix());
    assertEquals("123", licence1.getLicenceNumber());
    assertEquals("CS123", licence1.getLicenceReference());
    assertEquals(LicenceType.CARBON_STORAGE, licence1.getType());

    assertEquals(2, responsibleOrgs.size());
    assertThat(responsibleOrgs)
        .extracting(
            LicenceResponsibleOrganisation::getResponsibleOrganisationId,
            LicenceResponsibleOrganisation::getLicence,
            LicenceResponsibleOrganisation::getManagedByLms
        ).containsExactlyInAnyOrder(
            tuple(1, licence1, true),
            tuple(2, licence1, true)
        );

//    assertEquals(1, licenceSchedules.size());
//    assertThat(licenceSchedules)
//        .extracting(
//            LicenceSchedule::getLicence
//        ).containsExactlyInAnyOrder(
//            licence1
//        );
//
//    assertEquals(1, licenceScheduleDetails.size());
//    assertThat(licenceScheduleDetails)
//        .extracting(
//            LicenceScheduleDetail::getLicenceSchedule
//        ).containsExactlyInAnyOrder(
//            licenceSchedules.getFirst()
//        );
//
//    assertEquals(1, licenceStartDates.size());
//    assertThat(licenceStartDates)
//        .extracting(
//            LicenceStartDate::getLicenceScheduleDetail,
//            LicenceStartDate::getStartDate
//        ).containsExactlyInAnyOrder(
//            tuple(licenceScheduleDetails.getFirst(),
//                LocalDate.parse(startDateString, DateTimeFormatter.ofPattern("dd/MM/yyyy")))
//        );
//
//    assertEquals(2, licenceScheduleTerms.size());
//    assertThat(licenceScheduleTerms)
//        .extracting(
//            LicenceScheduleTerm::getLicenceScheduleDetail,
//            LicenceScheduleTerm::getTermType,
//            licenceScheduleTerm -> licenceScheduleTerm.getTermDuration().days(),
//            licenceScheduleTerm -> licenceScheduleTerm.getTermDuration().months(),
//            licenceScheduleTerm -> licenceScheduleTerm.getTermDuration().years()
//        ).containsExactlyInAnyOrder(
//            tuple(licenceScheduleDetails.getFirst(), TermType.APPRAISAL, 0, 6, 1),
//            tuple(licenceScheduleDetails.getFirst(), TermType.OPERATIONAL, 1, 6, 2)
//        );


  }
}
