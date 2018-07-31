<%@ page import="com.k_int.kbplus.RefdataValue;com.k_int.kbplus.auth.Role;com.k_int.kbplus.auth.UserOrg" %>
<% def contextService = grailsApplication.mainContext.getBean("contextService") %>
<% def securityService = grailsApplication.mainContext.getBean("springSecurityService") %>

<!doctype html>
<html>
<head>
    <meta name="layout" content="semanticUI"/>
    <title>${message(code:'laser', default:'LAS:eR')} : ${message(code: 'menu.user.errorReport')}</title>
</head>

<body>

<semui:breadcrumbs>
    <semui:crumb message="menu.institutions.help" class="active"/>
</semui:breadcrumbs>

<h1 class="ui header"><semui:headerIcon />${message(code: 'menu.user.errorReport')}</h1>

<semui:subNav actionName="${actionName}">
    <semui:subNavItem controller="profile" action="errorReport" message="profile.errorReport.label" />
    <semui:subNavItem controller="profile" action="errorReportOverview" message="profile.errorReportOverview.label" />
</semui:subNav>

<div class="ui grid">
    <div class="sixteen wide column">

        <table class="ui celled la-table table">
            <thead>
                <tr>
                    <th class="header"><g:message code="ticket.created.label" default="Created" /></th>
                    <th class="header"><g:message code="ticket.title.label" default="Title" /></th>
                    <th class="header"><g:message code="ticket.author.label" default="Author" /></th>
                    <th></th>
                </tr>
            </thead>
            <tbody>
                <g:each in="${tickets}" var="ticket">
                    <tr class="ticket-row-${ticket.id}">

                        <td>
                            <g:formatDate date="${ticket.dateCreated}" format="${message(code: 'default.date.format.notime')}"/>

                            &nbsp;

                            <g:if test="${ticket.status.value == 'New'}">
                                <div class="ui label">${ticket.status.getI10n('value')}</div>
                            </g:if>
                            <g:if test="${ticket.status.value == 'Open'}">
                                <div class="ui blue label">${ticket.status.getI10n('value')}</div>
                            </g:if>
                            <g:if test="${ticket.status.value == 'In Progress'}">
                                <div class="ui yellow label">${ticket.status.getI10n('value')}</div>
                            </g:if>
                            <g:if test="${ticket.status.value == 'Done'}">
                                <div class="ui olive label">${ticket.status.getI10n('value')}</div>
                            </g:if>
                            <g:if test="${ticket.status.value == 'Deferred'}">
                                <div class="ui grey label">${ticket.status.getI10n('value')}</div>
                            </g:if>

                        </td>
                        <td>
                            ${fieldValue(bean: ticket, field: "title")}
                        </td>
                        <td>
                            ${fieldValue(bean: ticket, field: "author")}
                        </td>

                        <td class="x">
                            <button class="ui icon button" data-target="ticket-content-${ticket.id}">
                                <i class="info icon"></i>
                            </button>
                        </td>
                    </tr>

                    <tr class="ticket-content-${ticket.id}" style="display:none">
                        <td colspan="5">
                            <div class="segment">
                                <h4 class="ui header">${ticket.title}</h4>

                                <p>
                                    Status: <semui:xEditableRefData owner="${ticket}" field="status" config='Ticket.Status'/>
                                </p>
                                <p>
                                    ${ticket.described}
                                </p>
                                <p>
                                    ${ticket.expected}
                                </p>
                                <p>
                                    ${ticket.info}
                                </p>
                                <p>
                                    Zuletzt bearbeitet: <g:formatDate date="${ticket.lastUpdated}" format="${message(code: 'default.date.format.noZ')}"/>
                                </p>
                                <p>
                                    Meta: ${ticket.meta}
                                </p>
                            </div>
                        </td>
                    </tr>

                </g:each>
            </tbody>
        </table>

        <r:script>
            $(function(){
                $('tr[class*=ticket-row] .button').click( function(){
                    $('.' + $(this).attr('data-target')).toggle()
                })
            })
        </r:script>
    </div>
</div>

</body>
</html>
