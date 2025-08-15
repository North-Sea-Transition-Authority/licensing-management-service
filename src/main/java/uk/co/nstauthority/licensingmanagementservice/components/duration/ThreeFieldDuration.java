package uk.co.nstauthority.licensingmanagementservice.components.duration;

import jakarta.persistence.Embeddable;

@Embeddable
public record ThreeFieldDuration(Integer years, Integer months, Integer days) {
}
