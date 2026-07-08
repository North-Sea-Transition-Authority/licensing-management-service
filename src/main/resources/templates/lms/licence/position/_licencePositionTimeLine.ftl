<#include '../../layout/layout.ftl'>

<#macro timeline licencePositionTimelineViews selectedPositionId>
  <@fdsTimeline.timeline>
      <@fdsTimeline.timelineSection>
          <#list licencePositionTimelineViews as licencePositionTimelineViewEntry>
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
              <#if licencePositionTimelineViewEntry.positionId() == selectedPositionId>
                  <#assign timeStampClasses += ["fds-timeline__time-stamp--selected"]/>
              </#if>
              <@fdsTimeline.timelineTimeStamp
                timeStampHeading=positionUrl
                timeStampHeadingHint=licencePositionTimelineViewEntry.regulatorReference()
                timeStampClass=timeStampClasses?join(" ")
              >
              <#if licencePositionTimelineViewEntry.addedInThisCorrection()>
                <p class="govuk-body">
                    <@fdsTag.tag tagClass="govuk-tag--green">Added position</@fdsTag.tag>
                </p>
                  <@fdsAction.link
                  linkText="Undo"
                  linkUrl=springUrl(licencePositionTimelineViewEntry.undoUrl())
                  linkScreenReaderText=licencePositionTimelineViewEntry.formattedPositionDate()
                  />
              </#if>
              </@fdsTimeline.timelineTimeStamp>
          </#list>
      </@fdsTimeline.timelineSection>
  </@fdsTimeline.timeline>
</#macro>