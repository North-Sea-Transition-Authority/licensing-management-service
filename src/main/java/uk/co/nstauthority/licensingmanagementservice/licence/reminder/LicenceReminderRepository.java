package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;

@Repository
public interface LicenceReminderRepository extends JpaRepository<LicenceReminder, UUID>, NotDuplicationSource {

  List<LicenceReminder> findAllByLicenceIn(Collection<Licence> licences);

  @Query("""
      SELECT reminder.notificationBatchReference AS notificationBatchReference,
             reminder.licence.licenceReference AS licenceReference,
             reminder.responsibleOrganisationId AS responsibleOrganisationId,
             reminder.reminderType AS reminderType,
             reminder.deadlineDate AS deadlineDate,
             notification.recipient AS recipient,
             notification.failureReason AS failureReason
      FROM LicenceReminder reminder
      JOIN Notification notification
        ON notification.domainReferenceId = CAST(reminder.notificationBatchReference AS String)
        AND notification.domainReferenceType = 'LICENCE_REMINDER'
      WHERE notification.status = uk.co.fivium.digitalnotificationlibrary.core.notification.NotificationStatus.FAILED_NOT_SENT
      AND NOT EXISTS (
          SELECT bounce FROM LicenceReminderBounce bounce
          WHERE bounce.notificationBatchReference = reminder.notificationBatchReference
      )
      """
  )
  List<BouncedReminder> findAllBouncedAndUnreported();
}
