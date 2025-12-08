<#-- @ftlvariable name="teamMemberViews" type="java.util.List<uk.co.nstauthority.licensingmanagementservice.teams.management.view.TeamMemberView>" -->
<#include '../layout/layout.ftl'>
<#import 'roleDescriptions.ftl' as roleDescriptions>

<#assign pageTitle=teamName/>

<@defaultPage
    htmlTitle=pageTitle
    pageHeading=pageTitle
    pageSize=PageSize.FULL_COLUMN
>

    <@roleDescriptions.roleDescriptions roles=rolesInTeam/>

    <#if canManageTeam>
        <@fdsAction.link
            linkText="Add user"
            linkUrl=springUrl(addMemberUrl)
            linkClass="govuk-button govuk-button--secondary"
            role=true
        />
    </#if>

    <#if teamMemberViews?has_content>

        <table class="govuk-table">
            <caption class="govuk-table__caption govuk-table__caption--m govuk-visually-hidden">Members of ${teamName}</caption>
            <thead class="govuk-table__head">
            <tr class="govuk-table__row">
                <th scope="col" class="govuk-table__header">Name</th>
                <th scope="col" class="govuk-table__header">Contact details</th>
                <th scope="col" class="govuk-table__header">Roles</th>
                <#if canManageTeam>
                    <th scope="col" class="govuk-table__header">Actions</th>
                </#if>
            </tr>
            </thead>
            <tbody class="govuk-table__body">
                <#list teamMemberViews as member>
                    <tr class="govuk-table__row">
                        <td class="govuk-table__cell">${member.getDisplayName()}</td>
                        <td class="govuk-table__cell">
                            <ul class="govuk-list govuk-!-margin-bottom-0">
                                <#if member.email()?has_content>
                                    <li>
                                        <@fdsAction.link linkText=member.email() linkUrl="mailto:${member.email()}"/>
                                    </li>
                                </#if>
                                <#if member.telNo()?has_content>
                                    <li>${member.telNo()}</li>
                                </#if>
                            </ul>
                        </td>
                        <td class="govuk-table__cell">
                            <ul class="govuk-list govuk-!-margin-bottom-0">
                                <#list member.roles() as role>
                                    <li>${role.getName()}</li>
                                </#list>
                            </ul>
                        </td>
                        <#if canManageTeam>
                            <td class="govuk-table__cell">
                                <ul class="govuk-list govuk-!-margin-bottom-0">
                                    <li>
                                        <@fdsAction.link
                                            linkText="Edit"
                                            linkUrl=springUrl(member.getEditUrl())
                                            linkScreenReaderText=member.getDisplayName()
                                        />
                                    </li>
                                    <li>
                                        <@fdsAction.link
                                            linkText="Remove"
                                            linkUrl=springUrl(member.getRemoveUrl())
                                            linkScreenReaderText=member.getDisplayName()
                                        />
                                    </li>
                                </ul>
                            </td>
                        </#if>
                    </tr>
                </#list>
            </tbody>
        </table>

    <#else>
        <@fdsInsetText.insetText>
            ${teamName} has no members.
        </@fdsInsetText.insetText>
    </#if>
    <@fdsBackLink.backLink backLinkUrl=springUrl(backUrl)/>
</@defaultPage>