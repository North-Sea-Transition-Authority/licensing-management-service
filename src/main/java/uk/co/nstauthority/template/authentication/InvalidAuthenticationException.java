package uk.co.nstauthority.template.authentication;

public class InvalidAuthenticationException extends RuntimeException {

  public InvalidAuthenticationException(String message) {
    super(message);
  }
}
