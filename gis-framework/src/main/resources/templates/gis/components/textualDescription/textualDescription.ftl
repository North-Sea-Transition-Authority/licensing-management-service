<#import "/spring.ftl" as spring>

<#macro textualDescription featureIds>
  <div
      data-gis-component="gis-textual-description"
      data-gis-textual-description-url="<@spring.url '/api/gis-framework/textual-description'/>?<#list featureIds?split(',') as featureId>featureId=${featureId?url('UTF-8')}<#sep>&</#sep></#list>"
   >
  </div>
</#macro>
