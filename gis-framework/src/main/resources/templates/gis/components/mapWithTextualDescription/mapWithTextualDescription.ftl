<#import "/spring.ftl" as spring>

<#macro mapWithTextualDescription featureIds srsWkid layout="horizontal" includeNstaQuadrants=true includeNstaBlocks=true>
  <div
      data-gis-component="gis-map-with-textual-description"
      data-gis-layout="${layout}"
      data-gis-include-nsta-quadrants="${includeNstaQuadrants?c}"
      data-gis-include-nsta-blocks="${includeNstaBlocks?c}"
      data-gis-features-url="<@spring.url '/api/gis-framework/features'/>?<#list featureIds as featureId>featureIds=${featureId?url('UTF-8')}<#sep>&</#sep></#list>"
      data-gis-outline-nodes-url="<@spring.url '/api/gis-framework/outline-nodes'/>?<#list featureIds as featureId>featureIds=${featureId?url('UTF-8')}<#sep>&</#sep></#list>"
      data-gis-textual-description-url="<@spring.url '/api/gis-framework/textual-description'/>?<#list featureIds as featureId>featureId=${featureId?url('UTF-8')}<#sep>&</#sep></#list>"
      data-gis-srs-wkid="${srsWkid}"
   >
  </div>
</#macro>
