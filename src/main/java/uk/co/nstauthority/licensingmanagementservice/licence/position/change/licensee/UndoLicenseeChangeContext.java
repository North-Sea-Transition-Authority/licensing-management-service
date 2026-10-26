package uk.co.nstauthority.licensingmanagementservice.licence.position.change.licensee;

import java.util.List;

public record UndoLicenseeChangeContext(
    List<String> currentWithdrawingLicenseeNames,
    List<String> currentJoiningLicenseeNames,
    List<String> restoredWithdrawingLicenseeNames,
    List<String> restoredJoiningLicenseeNames,
    List<String> resultingLicenseeNames
) {
}

