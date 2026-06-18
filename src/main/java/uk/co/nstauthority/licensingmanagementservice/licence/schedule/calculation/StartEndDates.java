package uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation;

import java.time.LocalDate;

public record StartEndDates(
    LocalDate startDate,
    LocalDate endDate
) {}
