package uk.co.nstauthority.licensingmanagementservice.licence.position;

import java.util.List;

public record LicenseeChangeContext(
    List<Integer> currentJoiningLicenseeIds,
    List<Integer> currentWithdrawingLicenseeIds,
    List<Integer> previousLicenseeIds,
    List<String> previousLicenseeNames
) {
}
