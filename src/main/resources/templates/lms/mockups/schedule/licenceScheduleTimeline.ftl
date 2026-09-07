<#-- Mockup copy of lms/licence/schedule/timeline/scheduleTimeline.ftl. Reuses the real schedule macros so the mockup
     matches the production screen, but can be changed without touching the live page. -->
<#include '../../layout/layout.ftl'>
<#import '../../licence/schedule/timeline/scheduleComponents.ftl' as scheduleComponents>

<#--
  Mirrors the eventComments macro in lms/licence/schedule/timeline/scheduleEvents.ftl, with one addition: a service
  generated comment can reference another licence, shown as a card link. The reference and URL are separate fields on
  the view rather than markup embedded in the comment text, so the text below stays escaped like the real macro.
-->
<#macro scheduleCommentsSection comments>
    <#if comments?has_content>
        <@fdsDetails.summaryDetails summaryTitle="Comments">
            <#list comments as comment>
                <@fdsCard.card>
                    <@fdsDataItems.dataItem>
                        <@fdsDataItems.dataValues key="Author" value=comment.author()/>
                        <@fdsDataItems.dataValues key="Posted" value=comment.datetime()/>
                    </@fdsDataItems.dataItem>
                    <@fdsDataItems.dataItem>
                        <@fdsDataItems.dataValues key="Comment" value=comment.comment()/>
                    </@fdsDataItems.dataItem>
                    <#if comment.linkedLicenceReference()?has_content>
                        <p class="govuk-body">
                            <@fdsAction.link
                                linkText="View licence ${comment.linkedLicenceReference()}"
                                linkUrl=springUrl(comment.linkedLicenceUrl())
                            />
                        </p>
                    </#if>

                    <@fdsAction.link linkText="Remove" linkUrl=springUrl(comment.removeCommentUrl())/>
                </@fdsCard.card>
            </#list>
        </@fdsDetails.summaryDetails>
    </#if>
</#macro>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.FULL_COLUMN
extendContainerWidth=true>

    <@scheduleComponents.timelineSummaryCard
        timelineSummaryCardView=timelineSummaryCardView
        updateLicenceStartDateUrl=updateLicenceStartDateUrl
        updateExpiryDateUrl=updateExpiryDateUrl
    />

    <@scheduleCommentsSection comments=scheduleComments/>

    <@fdsActionDropdown.actionDropdown dropdownButtonText="Add an event">
        <#list actions as actionView>
            <@fdsActionDropdown.actionDropdownItem actionText=actionView.action().displayText linkActionUrl=springUrl(actionView.url()) linkAction=true/>
        </#list>
    </@fdsActionDropdown.actionDropdown>

    <br><br>

    <@scheduleComponents.timelineWithFilters
        scheduleEventViews=scheduleEventViews
        timelineFilterOptions=timelineFilterOptions
        clearFilterUrl=clearFilterUrl
    />

    <@fdsAction.buttonGroup>
        <@fdsAction.link linkText="Review and apply" linkUrl=springUrl(reviewAndApplyUrl) linkClass="govuk-button"/>
        <@fdsAction.link linkText="Delete" linkUrl=springUrl(deleteScheduleUrl) linkClass="govuk-button govuk-button--warning"/>
    </@fdsAction.buttonGroup>
</@defaultPage>
