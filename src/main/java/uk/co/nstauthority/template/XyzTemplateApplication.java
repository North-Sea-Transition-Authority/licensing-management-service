package uk.co.nstauthority.template;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class XyzTemplateApplication {

  public static void main(String[] args) {
    SpringApplication.run(XyzTemplateApplication.class, args);
  }

}