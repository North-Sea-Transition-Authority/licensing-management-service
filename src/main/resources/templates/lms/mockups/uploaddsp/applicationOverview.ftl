<#include '../../layout/layout.ftl'>
<#include '_mockupNav.ftl'>
<#import '../../licence/scheduleWorkProgrammeApplication/_applicationContext.ftl' as applicationContextInfo>
<#import '../../component/actions/actionItems.ftl' as actionItems>
<#import '../../licence/scheduleWorkProgrammeApplication/scheduleApplicationSummary.ftl' as scheduleApplicationSummary>
<#import '../../summary/_summaryDetails.ftl' as summaryDetails>
<#import '../../macros/caseprocessingtabs/caseProccessingTabs.ftl' as caseProccessingTabs>

<#-- Mirrors the real overview template, with room for the mockup-only user switching links -->
<#macro overviewContent>
  <#if licenseeNotice??>
    <@fdsWarning.warning>
      ${licenseeNotice}
    </@fdsWarning.warning>
  </#if>
  <@applicationContextInfo.applicationContextInfo applicationContext=applicationContext/>
  <@actionItems.actionItems actionItems=applicationActions screenReaderText=applicationContext.reference()/>
  <@scheduleApplicationSummary.scheduleApplicationSummary accordionId=accordionId summarySections=summarySections/>
</#macro>

<@defaultPage
htmlTitle="Application overview"
pageHeading=""
pageSize=PageSize.FULL_COLUMN
extendContainerWidth=true
>
  <@mockupNavigation/>

  <@caseProccessingTabs.caseProcessingTabsWithContent
  tabs=availableTabs
  selectedTab={"value": selectedTab.value()}
  controllerUrl=controllerUrl>
    <#if selectedTab.value() == "overview">
      <@overviewContent/>
    </#if>

    <#if selectedTab.value() == "letter">
      <@fdsAccordion.accordion accordionId="summaryaccordion-${accordionId}">
        <#list issuedLetters as letter>
          <@fdsAccordion.accordionSection sectionHeading="${letter.name()} details" openSection=(letter?index == 0)>
            <@fdsSummaryList.summaryList>
              <@fdsSummaryList.summaryListRowNoAction keyText="Issued by">
                ${letter.issuedBy()}
              </@fdsSummaryList.summaryListRowNoAction>
              <@fdsSummaryList.summaryListRowNoAction keyText="Issued date">
                ${letter.issuedDate()}
              </@fdsSummaryList.summaryListRowNoAction>
              <@fdsSummaryList.summaryListRowNoAction keyText=letter.name()>
                <a class="govuk-link" href="#">${letter.fileName()}</a>
              </@fdsSummaryList.summaryListRowNoAction>
            </@fdsSummaryList.summaryList>
          </@fdsAccordion.accordionSection>
        </#list>
      </@fdsAccordion.accordion>
    </#if>

    <#if selectedTab.value() == "dpea">
      <#list dpeaSections as section>
        <#if section.heading()??>
          <h3 class="govuk-heading-m">${section.heading()}</h3>
        </#if>
        <@fdsSummaryList.summaryList>
          <#list section.rows() as row>
            <@fdsSummaryList.summaryListRowNoAction keyText=row.label()>
              <#list row.values() as value>
                <div>
                  <#if row.links()>
                    <a class="govuk-link" href="#">${value}</a>
                  <#else>
                    ${value}
                  </#if>
                </div>
              </#list>
            </@fdsSummaryList.summaryListRowNoAction>
          </#list>
        </@fdsSummaryList.summaryList>
      </#list>
    </#if>

    <#if selectedTab.value() == "case-events">
      <@summaryDetails.summaryDetails summaryItem=caseEventsSummaryItem/>
    </#if>
  </@caseProccessingTabs.caseProcessingTabsWithContent>
</@defaultPage>
