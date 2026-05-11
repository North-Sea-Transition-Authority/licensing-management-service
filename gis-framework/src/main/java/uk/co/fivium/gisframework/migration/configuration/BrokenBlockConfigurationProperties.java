package uk.co.fivium.gisframework.migration.configuration;


import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;

/**
 * This configuration is to be used for the GIS migration only.
 *
 * @param refBlockNameToLicenseBlocks is a map of reference block names to a list of licence block names. The Map only contains
 *                                    entries for reference blocks who have legacy licence blocks, which start in one reference
 *                                    block and end in another reference block. These reference blocks are handled differently in
 *                                    the migration.
 */
@ConfigurationProperties("gis-framework.migration.ref-block-name-to-license-blocks")
public record BrokenBlockConfigurationProperties(
    @Name("") Map<String, List<String>> refBlockNameToLicenseBlocks
) {

  /**
   * This method gets a list of all the license blocks that span multiple reference blocks, where one of which is the given
   * reference block. It will return an empty list if the given reference block has no license blocks that match that criteria.
   *
   * <p>In migration reference blocks that have these old licence blocks need to make sure they migrate as usual with the dense
   * points, but do not change the reference block line such that it would overlap a different reference block.</p>
   *
   * <p>In validation, we should not check that all licence blocks are contained by the given reference block if the reference
   * block has some of these special blocks.</p>
   *
   * @param refBlockName The name of the reference block.
   * @return A list of names of licence blocks which span multiple reference blocks, including given reference block.
   */
  public List<String> getBrokenLicenseBlockNames(String refBlockName) {
    return refBlockNameToLicenseBlocks.getOrDefault(refBlockName, List.of());
  }
}
