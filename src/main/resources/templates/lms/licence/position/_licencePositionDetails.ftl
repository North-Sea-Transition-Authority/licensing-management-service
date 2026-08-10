<#include '../../layout/layout.ftl'>
<#import '_positionChanges.ftl' as positionChanges>

<#macro details licencePositionChanges licencePositionState actions={} canEdit=false>
    <#if canEdit>
        <@fdsAction.buttonGroup>
            <#if actions.addChangeUrl()??>
                <@fdsAction.link linkText="Add change" linkUrl=springUrl(actions.addChangeUrl()) linkClass="govuk-button"/>
            </#if>
        </@fdsAction.buttonGroup>
    </#if>
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
  <#if licencePositionState.beneficialInterests()?has_content>
    <@fdsSummaryList.summaryListCard headingText="Beneficial interests" summaryListId="beneficial-interests">
      <#list licencePositionState.beneficialInterests() as beneficialInterest>
        <@fdsSummaryList.summaryListRowNoAction keyText=beneficialInterest.organisationName()>
          ${beneficialInterest.equity()}%
        </@fdsSummaryList.summaryListRowNoAction>
      </#list>
    </@fdsSummaryList.summaryListCard>
  </#if>
  <#if licencePositionChanges["licence-administrator"]??>
    <@positionChanges.administratorChange change=licencePositionChanges["licence-administrator"]/>
  </#if>
  <#if licencePositionChanges["set-equity"]??>
    <@positionChanges.setEquityChange change=licencePositionChanges["set-equity"]/>
  </#if>
  <#if licencePositionChanges["transfer-equity"]??>
      <@positionChanges.transferEquityChange change=licencePositionChanges["transfer-equity"]/>
  </#if>
</#macro>