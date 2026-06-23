<#include '../../layout/layout.ftl'>
<#import '_licencePositionTimeLine.ftl' as licencePositionTimeLine>
<#import '_licencePositionDetails.ftl' as licencePositionDetails>

<#assign pageTitle>
    ${licencePosition.getFormattedPositionDate()} (${licencePosition.getLicenceTransaction().getRegulatorReference()})
</#assign>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=pageCaption
  pageSize=PageSize.FULL_COLUMN
>

    <#if licencePositionTimelineView?has_content>
      <@grid.gridRow>
        <@grid.threeQuarterColumn>
          <@licencePositionDetails.details licencePosition=licencePosition canEdit=true/>
        </@grid.threeQuarterColumn>
        <@grid.oneQuarterColumn>
          <@licencePositionTimeLine.timeline licencePositionTimelineView=licencePositionTimelineView licencePosition=licencePosition/>
        </@grid.oneQuarterColumn>
      </@grid.gridRow>
    <#else>
      <@fdsInsetText.insetText>No executed licence positions for this licence.</@fdsInsetText.insetText>
    </#if>

</@defaultPage>