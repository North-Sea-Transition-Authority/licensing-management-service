package uk.co.nstauthority.licensingmanagementservice.authentication;

public class InvalidAuthenticationException extends RuntimeException {

  public InvalidAuthenticationException(String message) {
    super(message);
  }
}
