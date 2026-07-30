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
        <#assign positionActions>
        <#if licencePositionTimelineViewEntry.addedInThisCorrection()>
            <@fdsAction.link
              linkText="Undo"
              linkUrl=springUrl(licencePositionTimelineViewEntry.undoUrl())
              linkScreenReaderText=licencePositionTimelineViewEntry.formattedPositionDate()
            />
        </#if>
         <#if licencePositionTimelineViewEntry.removeUrl()??>
           <@fdsAction.link
             linkText="Remove"
             linkUrl=springUrl(licencePositionTimelineViewEntry.removeUrl())
             linkScreenReaderText=licencePositionTimelineViewEntry.formattedPositionDate()
           />
         </#if>
         <#if licencePositionTimelineViewEntry.reinstateUrl()??>
           <@fdsAction.link
             linkText="Reinstate"
             linkUrl=springUrl(licencePositionTimelineViewEntry.reinstateUrl())
             linkScreenReaderText=licencePositionTimelineViewEntry.formattedPositionDate()
           />
          </#if>
         <#if licencePositionTimelineViewEntry.correctDateUrl()??>
           <@fdsAction.link
             linkText="Correct&nbsp;date"?no_esc
             linkUrl=springUrl(licencePositionTimelineViewEntry.correctDateUrl())
             linkScreenReaderText=licencePositionTimelineViewEntry.formattedPositionDate()
           />
         </#if>
    </#assign>
    <@fdsTimeline.timelineTimeStamp
    timeStampHeading=positionUrl
    timeStampHeadingHint=licencePositionTimelineViewEntry.regulatorReference()
    timeStampClass=timeStampClasses?join(" ")
    timelineActionContent=positionActions
    >
        <#if licencePositionTimelineViewEntry.addedInThisCorrection()>
          <p class="govuk-body">
              <@fdsTag.tag tagClass="govuk-tag--green">New position</@fdsTag.tag>
          </p>
        </#if>
        <#if licencePositionTimelineViewEntry.removedInThisCorrection()>
          <p class="govuk-body">
            <@fdsTag.tag tagClass="govuk-tag--red">Removed</@fdsTag.tag>
          </p>
        </#if>
        <#if licencePositionTimelineViewEntry.correctedInThisCorrection()>
          <p class="govuk-body">
            <@fdsTag.tag tagClass="govuk-tag--yellow">Corrected</@fdsTag.tag>
          </p>
        </#if>
        <#if licencePositionTimelineViewEntry.correctOrderUrl()??>
            <@fdsAction.link
            linkText="Change&nbsp;order"?no_esc
            linkUrl=springUrl(licencePositionTimelineViewEntry.correctOrderUrl())
            linkScreenReaderText=licencePositionTimelineViewEntry.formattedPositionDate()
            />
        </#if>
          </@fdsTimeline.timelineTimeStamp>
        </#list>
      </@fdsTimeline.timelineSection>
  </@fdsTimeline.timeline>
</#macro>