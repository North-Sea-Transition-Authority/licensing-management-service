package uk.co.nstauthority.licensingmanagementservice.licence.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.energyportalmessagequeue.message.pears.PearsLicenceContacts;
import uk.co.fivium.energyportalmessagequeue.message.pears.PearsLicenceContacts.JoiningLicenseeContact;
import uk.co.fivium.energyportalmessagequeue.message.pears.PearsLicenceContactsEpmqMessage;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.PearsLicenceRefreshService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationRepository;

@ExtendWith(MockitoExtension.class)
class PearsLicenceContactsUpdateServiceTest {

  private static final Integer LICENCE_ID = 1;
  private static final Integer ORG_UNIT_ID = 10;
  private static final Integer OTHER_ORG_UNIT_ID = 20;
  private static final String CONTACT_EMAIL = "some.person@example.com";
  private static final String OTHER_CONTACT_EMAIL = "another.person@example.com";
  private static final String CORRELATION_ID = "correlation-id";

  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withId(LICENCE_ID)
      .withLicenceReference("P 123")
      .build();

  @Mock
  private PearsLicenceRefreshService pearsLicenceRefreshService;

  @Mock
  private LicenceResponsibleOrganisationRepository licenceResponsibleOrganisationRepository;

  @Mock
  private LicenceContactRepository licenceContactRepository;

  @InjectMocks
  private PearsLicenceContactsUpdateService pearsLicenceContactsUpdateService;

  @Captor
  private ArgumentCaptor<LicenceContact> licenceContactCaptor;

  @Test
  void updateContacts_whenLicenseeHasNoContact_thenContactCreated() {
    var licensee = licensee(ORG_UNIT_ID);

    when(pearsLicenceRefreshService.refreshLicence(LICENCE_ID)).thenReturn(Optional.of(LICENCE));
    when(licenceResponsibleOrganisationRepository.findByLicence_IdAndResponsibleOrganisationId(LICENCE_ID, ORG_UNIT_ID))
        .thenReturn(Optional.of(licensee));
    when(licenceContactRepository.findByLicensee(licensee)).thenReturn(Optional.empty());

    pearsLicenceContactsUpdateService.updateContacts(message(contact(ORG_UNIT_ID.toString(), CONTACT_EMAIL)));

    verify(licenceContactRepository).save(licenceContactCaptor.capture());
    assertThat(licenceContactCaptor.getValue())
        .extracting(LicenceContact::getLicensee, LicenceContact::getContactEmail)
        .containsExactly(licensee, CONTACT_EMAIL);
  }

  @Test
  void updateContacts_whenLicenseeAlreadyHasContact_thenContactEmailOverwritten() {
    var licensee = licensee(ORG_UNIT_ID);

    var existingContact = new LicenceContact();
    existingContact.setLicensee(licensee);
    existingContact.setContactEmail("previous.person@example.com");

    when(pearsLicenceRefreshService.refreshLicence(LICENCE_ID)).thenReturn(Optional.of(LICENCE));
    when(licenceResponsibleOrganisationRepository.findByLicence_IdAndResponsibleOrganisationId(LICENCE_ID, ORG_UNIT_ID))
        .thenReturn(Optional.of(licensee));
    when(licenceContactRepository.findByLicensee(licensee)).thenReturn(Optional.of(existingContact));

    pearsLicenceContactsUpdateService.updateContacts(message(contact(ORG_UNIT_ID.toString(), CONTACT_EMAIL)));

    verify(licenceContactRepository).save(existingContact);
    assertThat(existingContact.getContactEmail()).isEqualTo(CONTACT_EMAIL);
  }

  @Test
  void updateContacts_whenMessageNamesSeveralLicensees_thenEachContactUpdated() {
    var licensee = licensee(ORG_UNIT_ID);
    var otherLicensee = licensee(OTHER_ORG_UNIT_ID);

    when(pearsLicenceRefreshService.refreshLicence(LICENCE_ID)).thenReturn(Optional.of(LICENCE));
    when(licenceResponsibleOrganisationRepository.findByLicence_IdAndResponsibleOrganisationId(LICENCE_ID, ORG_UNIT_ID))
        .thenReturn(Optional.of(licensee));
    when(licenceResponsibleOrganisationRepository
        .findByLicence_IdAndResponsibleOrganisationId(LICENCE_ID, OTHER_ORG_UNIT_ID))
        .thenReturn(Optional.of(otherLicensee));
    when(licenceContactRepository.findByLicensee(licensee)).thenReturn(Optional.empty());
    when(licenceContactRepository.findByLicensee(otherLicensee)).thenReturn(Optional.empty());

    pearsLicenceContactsUpdateService.updateContacts(message(
        contact(ORG_UNIT_ID.toString(), CONTACT_EMAIL),
        contact(OTHER_ORG_UNIT_ID.toString(), OTHER_CONTACT_EMAIL)
    ));

    verify(licenceContactRepository, times(2)).save(licenceContactCaptor.capture());
    assertThat(licenceContactCaptor.getAllValues())
        .extracting(LicenceContact::getLicensee, LicenceContact::getContactEmail)
        .containsExactly(
            tuple(licensee, CONTACT_EMAIL),
            tuple(otherLicensee, OTHER_CONTACT_EMAIL)
        );
  }

  @Test
  void updateContacts_whenLicenceHeldButLicenseeNotYetRecorded_thenLicenseeCreatedUnmanaged() {
    var licensee = licensee(ORG_UNIT_ID);

    when(pearsLicenceRefreshService.refreshLicence(LICENCE_ID)).thenReturn(Optional.of(LICENCE));
    when(licenceResponsibleOrganisationRepository.findByLicence_IdAndResponsibleOrganisationId(LICENCE_ID, ORG_UNIT_ID))
        .thenReturn(Optional.empty());
    when(licenceResponsibleOrganisationRepository.save(licensee)).thenReturn(licensee);
    when(licenceContactRepository.findByLicensee(licensee)).thenReturn(Optional.empty());

    pearsLicenceContactsUpdateService.updateContacts(message(contact(ORG_UNIT_ID.toString(), CONTACT_EMAIL)));

    verify(licenceResponsibleOrganisationRepository).save(licensee);
    verify(licenceContactRepository).save(licenceContactCaptor.capture());
    assertThat(licenceContactCaptor.getValue())
        .extracting(LicenceContact::getLicensee, LicenceContact::getContactEmail)
        .containsExactly(licensee, CONTACT_EMAIL);
  }

  @Test
  void updateContacts_whenLicenceNotHeld_thenNothingUpdated() {
    when(pearsLicenceRefreshService.refreshLicence(LICENCE_ID)).thenReturn(Optional.empty());

    pearsLicenceContactsUpdateService.updateContacts(message(contact(ORG_UNIT_ID.toString(), CONTACT_EMAIL)));

    verifyNoInteractions(licenceResponsibleOrganisationRepository, licenceContactRepository);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", " ", "not-a-number"})
  void updateContacts_whenLicenceIdUnusable_thenNothingUpdated(String licenceId) {
    pearsLicenceContactsUpdateService.updateContacts(new PearsLicenceContactsEpmqMessage(
        licenceId,
        new PearsLicenceContacts(List.of(contact(ORG_UNIT_ID.toString(), CONTACT_EMAIL))),
        CORRELATION_ID,
        Instant.now()
    ));

    verifyNoInteractions(pearsLicenceRefreshService, licenceResponsibleOrganisationRepository, licenceContactRepository);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", " ", "not-a-number"})
  void updateContacts_whenOrganisationUnitIdUnusable_thenContactSkipped(String organisationUnitId) {
    when(pearsLicenceRefreshService.refreshLicence(LICENCE_ID)).thenReturn(Optional.of(LICENCE));

    pearsLicenceContactsUpdateService.updateContacts(message(contact(organisationUnitId, CONTACT_EMAIL)));

    verifyNoInteractions(licenceResponsibleOrganisationRepository, licenceContactRepository);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", " "})
  void updateContacts_whenContactEmailMissing_thenExistingContactLeftAlone(String contactEmail) {
    when(pearsLicenceRefreshService.refreshLicence(LICENCE_ID)).thenReturn(Optional.of(LICENCE));

    pearsLicenceContactsUpdateService.updateContacts(message(contact(ORG_UNIT_ID.toString(), contactEmail)));

    verifyNoInteractions(licenceResponsibleOrganisationRepository, licenceContactRepository);
  }

  @Test
  void updateContacts_whenMessageHasNoContacts_thenNothingUpdated() {
    when(pearsLicenceRefreshService.refreshLicence(LICENCE_ID)).thenReturn(Optional.of(LICENCE));

    pearsLicenceContactsUpdateService.updateContacts(
        new PearsLicenceContactsEpmqMessage(LICENCE_ID.toString(), null, CORRELATION_ID, Instant.now()));

    verifyNoInteractions(licenceResponsibleOrganisationRepository, licenceContactRepository);
  }

  @Test
  void updateContacts_whenMessageHasNoJoiningLicenseeContacts_thenNothingUpdated() {
    when(pearsLicenceRefreshService.refreshLicence(LICENCE_ID)).thenReturn(Optional.of(LICENCE));

    pearsLicenceContactsUpdateService.updateContacts(new PearsLicenceContactsEpmqMessage(
        LICENCE_ID.toString(), new PearsLicenceContacts(null), CORRELATION_ID, Instant.now()));

    verifyNoInteractions(licenceResponsibleOrganisationRepository, licenceContactRepository);
  }

  private LicenceResponsibleOrganisation licensee(Integer organisationUnitId) {
    var licensee = new LicenceResponsibleOrganisation();
    licensee.setLicence(LICENCE);
    licensee.setResponsibleOrganisationId(organisationUnitId);
    licensee.setManagedByLms(false);
    return licensee;
  }

  private JoiningLicenseeContact contact(String organisationUnitId, String contactEmail) {
    return new JoiningLicenseeContact("P", "123", organisationUnitId, contactEmail);
  }

  private PearsLicenceContactsEpmqMessage message(JoiningLicenseeContact... contacts) {
    return new PearsLicenceContactsEpmqMessage(
        LICENCE_ID.toString(),
        new PearsLicenceContacts(List.of(contacts)),
        CORRELATION_ID,
        Instant.now()
    );
  }
}
