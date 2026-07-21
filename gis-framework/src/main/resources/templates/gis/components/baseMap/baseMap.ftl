<#import "/spring.ftl" as spring>

<#macro baseMap featureIds srsWkid includeNstaQuadrants=true includeNstaBlocks=true includeSnapPoints=true>
  <div
      data-gis-component="gis-base-map"
      data-gis-include-nsta-quadrants="${includeNstaQuadrants?c}"
      data-gis-include-nsta-blocks="${includeNstaBlocks?c}"
      data-gis-include-snap-points="${includeSnapPoints?c}"
      data-gis-features-url="<@spring.url '/api/gis-framework/features'/>?<#list featureIds as featureId>featureIds=${featureId?url('UTF-8')}<#sep>&</#sep></#list>"
      data-gis-outline-nodes-url="<@spring.url '/api/gis-framework/outline-nodes'/>?<#list featureIds as featureId>featureIds=${featureId?url('UTF-8')}<#sep>&</#sep></#list>"
      data-gis-srs-wkid="${srsWkid}"
   >
  </div>
</#macro>
