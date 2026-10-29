<#include '../../../layout/layout.ftl'>
<#import '_operatorCorrectionPlayback.ftl' as operatorPlayback>

<#assign positions = [
  {"date": "27 May 2026", "reference": "REF-4A652C2B"},
  {"date": "16 February 2026", "reference": "REF-674C56EB"},
  {"date": "15 June 2025", "reference": "REF-1234FE2A"}
]/>

<#assign selectedPosition = positions[1]/>

<@defaultPage
  htmlTitle=operatorType.licenceReference
  pageHeading=operatorType.licenceReference
  pageSize=PageSize.FULL_COLUMN
  caption=operatorType.licenceType.displayName
>

  <h2 class="govuk-heading-m">${selectedPosition.date} (${selectedPosition.reference})</h2>

  <@grid.gridRow>
    <@grid.threeQuarterColumn>

      <@fdsAction.buttonGroup>
        <@fdsAction.link
          linkText="Add change"
          linkUrl=springUrl(addChangeUrl)
          linkClass="govuk-button"
        />
      </@fdsAction.buttonGroup>

      <#if !operatorType.subareaScoped>
        <@fdsDataItems.dataItem>
          <@fdsDataItems.dataValues key=operatorType.displayName value="SHELL U.K. LIMITED"/>
        </@fdsDataItems.dataItem>
      </#if>

      <@operatorPlayback.operatorChangeCard
        operatorType=operatorType
        subarea=operatorType.subareaScoped?then("14/29b", "")
        withdrawingOperator="BP EXPLORATION (ALPHA) LIMITED"
        joiningOperator="SHELL U.K. LIMITED"
        marker=correctedMarker
        correctUrl=springUrl(correctUrl)
        removeUrl=springUrl(removeUrl)
        summaryListId="corrected-operator-change"
      />

      <#if operatorType.subareaScoped>
        <@operatorPlayback.operatorChangeCard
          operatorType=operatorType
          subarea="14/29a"
          withdrawingOperator="BP EXPLORATION LIMITED"
          joiningOperator="TOTAL E&P UK LIMITED"
          marker=addedMarker
          correctUrl=springUrl(correctUrl)
          removeUrl=springUrl(removeUrl)
          summaryListId="added-operator-change"
        />
      </#if>

    </@grid.threeQuarterColumn>

    <@grid.oneQuarterColumn>
      <@fdsTimeline.timeline>
        <@fdsTimeline.timelineSection>
          <#list positions as position>
            <#assign timeStampClasses = []/>
            <#if position?is_last>
              <#assign timeStampClasses += ["fds-timeline__time-stamp--no-border"]/>
            </#if>
            <#if position.reference == selectedPosition.reference>
              <#assign timeStampClasses += ["fds-timeline__time-stamp--selected"]/>
            </#if>
            <#assign positionUrl>
              <@fdsAction.link
                linkText=position.date
                linkUrl="#"
                linkClass="govuk-link govuk-link--no-visited-state"
              />
            </#assign>
            <@fdsTimeline.timelineTimeStamp
              timeStampHeading=positionUrl
              timeStampHeadingHint=position.reference
              timeStampClass=timeStampClasses?join(" ")
            />
          </#list>
        </@fdsTimeline.timelineSection>
      </@fdsTimeline.timeline>
    </@grid.oneQuarterColumn>
  </@grid.gridRow>

</@defaultPage>