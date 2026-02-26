package uk.co.nstauthority.licensingmanagementservice.document;

import java.util.EnumSet;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AddSectionOptionConverter implements Converter<String, AddSectionOption> {

  @Override
  public AddSectionOption convert(String enumName) {
    return EnumSet.allOf(AddSectionOption.class).stream()
        .filter(sectionOption -> sectionOption.name().equals(enumName))
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required request parameter 'section'"));
  }
}
