package uk.co.nstauthority.licensingmanagementservice.components.duration;

import jakarta.persistence.Embeddable;
import java.time.Period;
import java.util.Collection;

@Embeddable
public record ThreeFieldDuration(Integer years, Integer months, Integer days) {

  public static ThreeFieldDuration total(Collection<ThreeFieldDuration> durations) {
    var total = durations.stream()
        .map(duration -> Period.of(duration.years(), duration.months(), duration.days()))
        .reduce(Period.ZERO, Period::plus)
        .normalized();

    return new ThreeFieldDuration(total.getYears(), total.getMonths(), total.getDays());
  }
}
