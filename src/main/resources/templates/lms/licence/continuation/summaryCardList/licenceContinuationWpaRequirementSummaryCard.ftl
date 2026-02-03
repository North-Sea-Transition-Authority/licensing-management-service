<#include '../../../layout/layout.ftl'>

<#macro workProgrammeActivities workProgrammeActivity withResultNumber=true>
  <@fdsSummaryList.summaryListCard
    headingText="${workProgrammeActivity.category()}"
    summaryListId="licenceworkprogrammeamendmentvaluesresult">
      <@fdsSummaryList.summaryListRowNoAction keyText="Description">
          ${workProgrammeActivity.description()}
      </@fdsSummaryList.summaryListRowNoAction>
          <@fdsSummaryList.summaryListRowNoAction keyText="Commitment">
              ${workProgrammeActivity.commitment()}
          </@fdsSummaryList.summaryListRowNoAction>
  </@fdsSummaryList.summaryListCard>
</#macro>