<#import "/spring.ftl" as spring>

<#macro mapWithTextualDescription featureIds srsWkid layout="horizontal" includeNstaQuadrants=true includeNstaBlocks=true selectionInputName="" describedFeatureId="" toggle=false>
  <#-- With a described feature, node numbers cover only it; the other features are dashed outlines. -->
  <#assign describedFeatureIds = describedFeatureId?has_content?then([describedFeatureId], featureIds)>
  <div
      data-gis-component="gis-map-with-textual-description"
      <#if selectionInputName?has_content>data-gis-selection-input-name="${selectionInputName}"</#if>
      <#if describedFeatureId?has_content>data-gis-described-feature-id="${describedFeatureId}"</#if>
      data-gis-layout="${layout}"
      data-gis-toggle="${toggle?c}"
      data-gis-include-nsta-quadrants="${includeNstaQuadrants?c}"
      data-gis-include-nsta-blocks="${includeNstaBlocks?c}"
      data-gis-features-url="<@spring.url '/api/gis-framework/features'/>?<#list featureIds as featureId>featureIds=${featureId?url('UTF-8')}<#sep>&</#sep></#list>"
      data-gis-outline-nodes-url="<@spring.url '/api/gis-framework/outline-nodes'/>?<#list describedFeatureIds as featureId>featureIds=${featureId?url('UTF-8')}<#sep>&</#sep></#list>"
      data-gis-textual-description-url="<@spring.url '/api/gis-framework/textual-description'/>?<#list describedFeatureIds as featureId>featureId=${featureId?url('UTF-8')}<#sep>&</#sep></#list>"
      data-gis-srs-wkid="${srsWkid?c}"
   >
  </div>
</#macro>
