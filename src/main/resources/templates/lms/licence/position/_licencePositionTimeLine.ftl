<#include '../../layout/layout.ftl'>

<#macro timeline licencePositionTimelineView licencePosition>
  <@fdsTimeline.timeline>
      <@fdsTimeline.timelineSection>
          <#list licencePositionTimelineView as licencePositionTimelineViewEntry>
              <#assign positionUrl>
                  <@fdsAction.link
                    linkText=licencePositionTimelineViewEntry.formattedPositionDate()
                    linkUrl=springUrl(licencePositionTimelineViewEntry.url())
                    linkClass="govuk-link govuk-link--no-visited-state"
                  />
              </#assign>
              <#assign timeStampClasses = []/>
              <#if licencePositionTimelineViewEntry?is_last>
                  <#assign timeStampClasses += ["fds-timeline__time-stamp--no-border"]/>
              </#if>
              <#if licencePositionTimelineViewEntry.positionId() == licencePosition.id>
                  <#assign timeStampClasses += ["fds-timeline__time-stamp--selected"]/>
              </#if>
              <@fdsTimeline.timelineTimeStamp
                timeStampHeading=positionUrl
                timeStampHeadingHint=licencePositionTimelineViewEntry.regulatorReference()
                timeStampClass=timeStampClasses?join(" ")
              >
              </@fdsTimeline.timelineTimeStamp>
          </#list>
      </@fdsTimeline.timelineSection>
  </@fdsTimeline.timeline>
</#macro>