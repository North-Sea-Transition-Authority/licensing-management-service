package uk.co.fivium.gisframework.configuration;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

@AutoConfiguration
@AutoConfigureBefore(JpaRepositoriesAutoConfiguration.class)
public class EntityAutoConfiguration implements ImportBeanDefinitionRegistrar {

  private static final String BASE_PACKAGE = "uk.co.fivium.gisframework";
  private static final String EXCLUDED_PACKAGE = "uk.co.fivium.gisframework.migration";

  @Override
  public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
    var scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter((metadataReader, metadataReaderFactory) -> true);

    Set<String> packages = scanner
        .findCandidateComponents(BASE_PACKAGE)
        .stream()
        .map(BeanDefinition::getBeanClassName)
        .filter(Objects::nonNull)
        .map(className -> className.substring(0, className.lastIndexOf('.')))
        .filter(pkg -> !pkg.startsWith(EXCLUDED_PACKAGE))
        .collect(Collectors.toSet());

    AutoConfigurationPackages.register(registry, packages.toArray(String[]::new));
  }
}
