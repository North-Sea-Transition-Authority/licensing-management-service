<#include '../../layout/layout.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=pageCaption
>

    <#if licencePositionTimelineView?has_content>
      <@fdsTimeline.timeline>
        <@fdsTimeline.timelineSection>
          <#list licencePositionTimelineView as licencePositionTimelineEntry>
            <#assign editPosition>
                <@fdsAction.link linkText="Edit" linkUrl="#" linkClass="govuk-link"/>
            </#assign>
            <@fdsTimeline.timelineTimeStamp
              timeStampHeading=licencePositionTimelineEntry.getFormattedDate()
              timeStampHeadingHint=licencePositionTimelineEntry.regulatorReference()
              timeStampClass=licencePositionTimelineEntry?is_last?then("fds-timeline__time-stamp--no-border", "")
              timelineActionContent=editPosition
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