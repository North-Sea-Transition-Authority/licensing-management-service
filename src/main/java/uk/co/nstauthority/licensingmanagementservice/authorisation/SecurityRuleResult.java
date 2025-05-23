package uk.co.nstauthority.licensingmanagementservice.authorisation;

import org.springframework.http.HttpStatus;

public record SecurityRuleResult(
    boolean hasRulePassed, // controls whether the for-loop keeps going
    HttpStatus failureStatus,
    String failureMessage
) {

  /**
   * Indicates that the security rule has passed all its checks and the process flow should continue as normal.
   *
   * @return security rule result that doesn't alter the handler interceptors normal operating behaviour
   */
  public static SecurityRuleResult continueAsNormal() {
    return new SecurityRuleResult(true, null, null);
  }

  public static SecurityRuleResult checkFailedWithStatus(HttpStatus httpStatus) {
    return new SecurityRuleResult(false, httpStatus,  null);
  }

  public static SecurityRuleResult checkFailedWithStatusAndMessage(HttpStatus httpStatus, String failureMessage) {
    return new SecurityRuleResult(false, httpStatus, failureMessage);
  }
}
