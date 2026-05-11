package uk.co.fivium.gisframework.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import uk.co.fivium.gisframework.migration.configuration.BrokenBlockConfigurationProperties;

@AutoConfiguration
@ComponentScan("uk.co.fivium.gisframework")
@EnableConfigurationProperties({BrokenBlockConfigurationProperties.class})
public class GisFrameworkAutoConfiguration {

}
