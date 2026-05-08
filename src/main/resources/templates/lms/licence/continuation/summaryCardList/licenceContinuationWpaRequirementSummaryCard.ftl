<#include '../../../layout/layout.ftl'>

<#macro workProgrammeActivities workProgrammeActivity withResultNumber=true isReviewer=false>
  <@fdsSummaryList.summaryListCard
    headingText="${workProgrammeActivity.category()}"
    summaryListId="licenceworkprogrammeamendmentvaluesresult">
      <@fdsSummaryList.summaryListRowNoAction keyText="Description">
          ${workProgrammeActivity.description()}
      </@fdsSummaryList.summaryListRowNoAction>
      <@fdsSummaryList.summaryListRowNoAction keyText="Commitment">
          ${workProgrammeActivity.commitment()}
      </@fdsSummaryList.summaryListRowNoAction>
      <#if isReviewer>
          <@fdsSummaryList.summaryListRowNoAction keyText="Status">
              <#if workProgrammeActivity.status()?has_content>
                  <@fdsTag.tag tagClass=workProgrammeActivity.status().getTagDisplayClass()>
                      ${workProgrammeActivity.status().getDisplayName()}
                  </@fdsTag.tag>
              </#if>
          </@fdsSummaryList.summaryListRowNoAction>
      </#if>
  </@fdsSummaryList.summaryListCard>
</#macro>