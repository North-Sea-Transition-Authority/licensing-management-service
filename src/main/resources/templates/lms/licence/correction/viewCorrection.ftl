<#include '../../layout/layout.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
>
  <@fdsSummaryList.summaryListCard summaryListId="correction-details" headingText="Correction details">
   <@fdsSummaryList.summaryListRowNoAction keyText="Correction reference">
            ${correctionReference}
        </@fdsSummaryList.summaryListRowNoAction>
        <@fdsSummaryList.summaryListRowNoAction keyText="Reason for correction">
            ${reason}
        </@fdsSummaryList.summaryListRowNoAction>
    </@fdsSummaryList.summaryListCard>

    <@fdsAction.link linkText="Add position" linkUrl=springUrl(addPositionUrl) linkClass="govuk-button"/>

  <h2 class="govuk-heading-m">Executed timeline</h2>

    <#if licencePositionTimelineView?has_content>
        <@fdsTimeline.timeline>
            <@fdsTimeline.timelineSection>
                <#list licencePositionTimelineView as licencePositionTimelineEntry>
                    <@fdsTimeline.timelineTimeStamp
                    timeStampHeading=licencePositionTimelineEntry.formattedPositionDate()
                    timeStampHeadingHint=licencePositionTimelineEntry.regulatorReference()
                    timeStampClass=licencePositionTimelineEntry?is_last?then("fds-timeline__time-stamp--no-border", "")
                    >
                        <@fdsTimeline.timelineEvent>
                          <p class="govuk-body">[POSITION STATE HERE]</p>
                        </@fdsTimeline.timelineEvent>
                    </@fdsTimeline.timelineTimeStamp>
                </#list>
            </@fdsTimeline.timelineSection>
        </@fdsTimeline.timeline>
    <#else>
      <p class="govuk-body">No executed licence positions for this licence.</p>
    </#if>
</@defaultPage>