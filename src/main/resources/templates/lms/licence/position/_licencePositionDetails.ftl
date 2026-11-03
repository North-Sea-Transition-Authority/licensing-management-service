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
    <#if licencePositionState.licenseeStateView().licensees()?size != 0>
      <h3 class="govuk-heading-s govuk-!-margin-bottom-0">Licensees</h3>
      <ul class="govuk-list">
        <#list licencePositionState.licenseeStateView().licensees() as licensee>
          <li>${licensee.organisationName()} <@organisationMarkerTag organisation=licensee/></li>
        </#list>
      </ul>
    </#if>
    <#if isCarbonStorage>
      <#assign storageOperatorName>
        <#if licencePositionState.storageOperatorName()?has_content>
          ${licencePositionState.storageOperatorName()}
        <#else>
          None
        </#if>
      </#assign>
      <@fdsDataItems.dataItem>
          <@fdsDataItems.dataValues key="Storage operator" value=storageOperatorName/>
      </@fdsDataItems.dataItem>
    <#else>
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
        <@beneficialInterestRow beneficialInterest=beneficialInterest/>
      </#list>
    </@fdsSummaryList.summaryListCard>
  </#if>
  <#if licencePositionState.subareaBeneficialInterests()?has_content>
    <h2 class="govuk-heading-s">Subarea beneficial interests</h2>
    <#list licencePositionState.subareaBeneficialInterests() as subareaBeneficialInterest>
      <@fdsSummaryList.summaryListCard
        headingText=subareaBeneficialInterest.subareaLabel()
        headingSize="h3"
        summaryListId="subarea-beneficial-interests-${subareaBeneficialInterest?index}"
      >
        <#list subareaBeneficialInterest.beneficialInterests() as beneficialInterest>
          <@beneficialInterestRow beneficialInterest=beneficialInterest/>
        </#list>
      </@fdsSummaryList.summaryListCard>
    </#list>
  </#if>
  <#if licencePositionState.organisationNameHistories()?has_content>
    <@fdsDetails.summaryDetails summaryTitle="Organisation name history">
      <#list licencePositionState.organisationNameHistories() as organisationNameHistory>
        <h3 class="govuk-heading-s">${organisationNameHistory.organisationName()}</h3>
        <#if organisationNameHistory.previousNames()?has_content>
          <p class="govuk-body govuk-!-margin-bottom-1">Previously known as</p>
          <ul class="govuk-list govuk-list--bullet">
            <#list organisationNameHistory.previousNames() as previousName>
              <li>${previousName.name()} <span class="govuk-hint govuk-!-display-inline">${previousName.dateText()}</span></li>
            </#list>
          </ul>
        </#if>
        <#if organisationNameHistory.laterNames()?has_content>
          <p class="govuk-body govuk-!-margin-bottom-1">Later known as</p>
          <ul class="govuk-list govuk-list--bullet">
            <#list organisationNameHistory.laterNames() as laterName>
              <li>${laterName.name()} <span class="govuk-hint govuk-!-display-inline">${laterName.dateText()}</span></li>
            </#list>
          </ul>
        </#if>
      </#list>
    </@fdsDetails.summaryDetails>
  </#if>
  <#list licencePositionChanges?reverse as change>
    <@positionChanges.changeCard
      change=change
      summaryListId="${change.type()}-${change?index}"
      isCarbonStorage=isCarbonStorage
    />
  </#list>
</#macro>

<#macro beneficialInterestRow beneficialInterest>
  <@fdsSummaryList.summaryListRowNoAction
    keyText=beneficialInterest.organisationName()
    rowClass="lms-summary-list__row--value-with-tag"
  >
    <div class="lms-value-with-tag">
      ${percentage.format(beneficialInterest.equity())}
      <@organisationMarkerTag organisation=beneficialInterest/>
    </div>
  </@fdsSummaryList.summaryListRowNoAction>
</#macro>

<#macro organisationMarkerTag organisation>
  <#if organisation.marker()??>
    <@fdsTag.tag tagClass="${organisation.marker().tagClass} lms-tag--single-line">${organisation.marker().label}</@fdsTag.tag>
  </#if>
</#macro>
