package uk.co.nstauthority.licensingmanagementservice.licence.position.change.storageoperator;

public record StorageOperatorChangeContext(
    Integer currentOperatorId,
    Integer previousOperatorId,
    String currentOperatorName,
    String previousOperatorName
) {

}
