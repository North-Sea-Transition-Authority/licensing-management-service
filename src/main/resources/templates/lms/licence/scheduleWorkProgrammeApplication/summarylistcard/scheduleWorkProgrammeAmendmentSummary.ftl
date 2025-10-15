<#include '../../../layout/layout.ftl'>
<#import 'scheduleLicenceWorkProgrammeAmendmentSummaryCard.ftl' as licenceWorkProgrammeAmendmentCard>

<#macro licenceWorkProgrammeAmendments licenceWorkProgrammeAmendment>
    <@fdsSummaryList.summaryListCard headingText="Work programme amendments" summaryListId="licenceworkprogrammeamendmentlist">
      <#list licenceWorkProgrammeAmendment.licenceWorkProgrammeAmendmentSummaryViews() as licenceWorkProgrammeAmendmentsView>
        <@licenceWorkProgrammeAmendmentCard.licenceWorkProgrammeAmendments licenceWorkProgrammeAmendment=licenceWorkProgrammeAmendmentsView withResultNumber=false/>
      </#list>
    </@fdsSummaryList.summaryListCard>
</#macro>