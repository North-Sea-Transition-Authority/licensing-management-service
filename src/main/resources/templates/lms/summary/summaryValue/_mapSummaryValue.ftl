<#include '../../layout/layout.ftl'>
<#import '../../../gis/components/mapWithTextualDescription/mapWithTextualDescription.ftl' as gisMap>

<#-- @ftlvariable name="mapValues" type="java.util.Collection<uk.co.nstauthority.licensingmanagementservice.summary.SummaryMapView>" -->

<#macro mapValueDisplay mapValues layout="horizontal">
  <#list mapValues as mapValue>
    <@gisMap.mapWithTextualDescription
      featureIds=mapValue.featureIds()
      srsWkid=mapValue.srsWkid()
      describedFeatureId=mapValue.describedFeatureId()!""
      layout=layout
      toggle=true/>
  </#list>
</#macro>
