<#include '../../layout/layout.ftl'>
<#import '_positionChanges.ftl' as positionChanges>

<#macro details licencePositionChanges licencePositionState actions={} canEdit=false isCarbonStorage=false>
    <#if canEdit>
        <@fdsAction.buttonGroup>
            <#if actions.addChangeUrl()??>
                <@fdsAction.link linkText="Add change" linkUrl=springUrl(actions.addChangeUrl()) linkClass="govuk-button"/>
            </#if>
        </@fdsAction.buttonGroup>
    </#if>
    <#if !isCarbonStorage>
      <#assign adminName>
        <#if licencePositionState.administratorStateView().organisationName()?has_content>
          ${licencePositionState.administratorStateView().organisationName()}
        <#else>
          None
        </#if>
      </#assign>
      <@fdsDataItems.dataItem>
          <@fdsDataItems.dataValues key="Licence administrator" value=adminName/>
      </@fdsDataItems.dataItem>
    </#if>
  <#if licencePositionState.beneficialInterests()?has_content>
    <@fdsSummaryList.summaryListCard headingText="Beneficial interests" summaryListId="beneficial-interests">
      <#list licencePositionState.beneficialInterests() as beneficialInterest>
        <@fdsSummaryList.summaryListRowNoAction keyText=beneficialInterest.organisationName()>
          ${beneficialInterest.equity()}%
        </@fdsSummaryList.summaryListRowNoAction>
      </#list>
    </@fdsSummaryList.summaryListCard>
  </#if>
  <#list licencePositionChanges?keys?reverse as changeType>
    <#local change = licencePositionChanges[changeType]>
    <#if changeType == "licence-administrator" && !isCarbonStorage>
      <@positionChanges.administratorChange change=change/>
    <#elseif changeType == "set-equity">
      <@positionChanges.setEquityChange change=change/>
    <#elseif changeType == "transfer-equity">
      <@positionChanges.transferEquityChange change=change/>
    <#elseif changeType == "partial-surrender">
      <@positionChanges.partialSurrenderChange change=change/>
    <#elseif changeType == "subarea">
      <@positionChanges.subAreaChange change=change/>
    </#if>
  </#list>
</#macro>
