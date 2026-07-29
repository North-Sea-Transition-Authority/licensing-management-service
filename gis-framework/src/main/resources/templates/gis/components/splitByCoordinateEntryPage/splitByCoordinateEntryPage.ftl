<#import "/spring.ftl" as spring>

<#macro splitByCoordinateEntryPage featureIds srsWkid precision=4 includeNstaQuadrants=true includeNstaBlocks=true>
  <div
      data-gis-component="gis-split-by-coordinate-entry"
      data-gis-srs-wkid="${srsWkid?c}"
      data-gis-precision="${precision?c}"
      data-gis-include-nsta-quadrants="${includeNstaQuadrants?c}"
      data-gis-include-nsta-blocks="${includeNstaBlocks?c}"
      data-gis-features-url="<@spring.url '/api/gis-framework/features'/>?<#list featureIds as featureId>featureIds=${featureId?url('UTF-8')}<#sep>&</#sep></#list>"
      data-gis-outline-nodes-url="<@spring.url '/api/gis-framework/outline-nodes'/>?<#list featureIds as featureId>featureIds=${featureId?url('UTF-8')}<#sep>&</#sep></#list>"
   >
  </div>
</#macro>
