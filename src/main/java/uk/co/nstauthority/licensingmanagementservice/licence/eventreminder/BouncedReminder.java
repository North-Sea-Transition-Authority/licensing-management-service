package uk.co.nstauthority.licensingmanagementservice.licence.eventreminder;

import java.time.LocalDate;
import java.util.UUID;

public interface BouncedReminder {

  UUID getNotificationBatchReference();

  String getLicenceReference();

  Integer getResponsibleOrganisationId();

  ReminderType getReminderType();

  LocalDate getDeadlineDate();

  String getRecipient();

  String getFailureReason();
}
