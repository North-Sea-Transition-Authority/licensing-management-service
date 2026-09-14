<#include '../../layout/layout.ftl'>
<#import '../../../gis/components/mapWithTextualDescription/mapWithTextualDescription.ftl' as gisMap>

<#-- @ftlvariable name="mapValues" type="java.util.Collection<uk.co.nstauthority.licensingmanagementservice.summary.SummaryMapView>" -->

<#macro mapValueDisplay mapValues>
  <#list mapValues as mapValue>
    <@gisMap.mapWithTextualDescription
      featureIds=mapValue.featureIds()
      srsWkid=mapValue.srsWkid()
      layout="horizontal"/>
  </#list>
</#macro>
