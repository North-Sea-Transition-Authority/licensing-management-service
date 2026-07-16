package uk.co.fivium.gisframework.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * This advice is only active under the opt-in {@code localdev-vue-hmr} profile, where the
 * {@code gis-framework.vite-dev-server-url} property is set. Every other profile (including
 * {@code development} and {@code production}) has no {@code gisViteDevServerUrl} attribute, so
 * {@code gisAssets.ftl} renders the prebuilt {@code /gis/dist/} bundle and never references the dev
 * server.
 */
@Profile("localdev-vue-hmr")
@ControllerAdvice
public class GisFrontendDevControllerAdvice {

  private final String viteDevServerUrl;

  GisFrontendDevControllerAdvice(
      @Value("${gis-framework.vite-dev-server-url:}") String viteDevServerUrl) {
    this.viteDevServerUrl = viteDevServerUrl;
  }

  @ModelAttribute("gisViteDevServerUrl")
  String gisViteDevServerUrl() {
    return viteDevServerUrl;
  }
}
