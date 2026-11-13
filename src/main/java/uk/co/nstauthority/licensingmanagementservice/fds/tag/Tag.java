package uk.co.nstauthority.licensingmanagementservice.fds.tag;

public record Tag(String tagText, TagColour tagColour) {

  public String tagClass() {
    return "govuk-tag--%s".formatted(tagColour.name().toLowerCase());
  }
}
