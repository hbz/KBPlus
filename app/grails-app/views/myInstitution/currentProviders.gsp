<%@ page import="com.k_int.kbplus.RefdataValue" %>
<!doctype html>

<html>
    <head>
        <meta name="layout" content="semanticUI" />
        <title>${message(code:'laser', default:'LAS:eR')} : ${message(code:'menu.my.providers')}</title>
    </head>
    <body>

        <semui:breadcrumbs>
            <semui:crumb controller="myInstitution" action="dashboard" text="${institution?.getDesignation()}" />
            <semui:crumb message="menu.my.providers" class="active" />
        </semui:breadcrumbs>

        <semui:controlButtons>
            <semui:exportDropdown>
                <g:if test="${filterSet}">
                    <semui:exportDropdownItem>
                        <g:link class="item js-open-confirm-modal"
                                data-confirm-term-content = "${message(code: 'confirmation.content.exportPartial')}"
                                data-confirm-term-how="ok" controller="myInstitution" action="currentProviders"
                                params="${params+[exportXLS:true]}">
                            ${message(code:'default.button.exports.xls')}
                        </g:link>
                    </semui:exportDropdownItem>
                    <semui:exportDropdownItem>
                        <g:link class="item js-open-confirm-modal"
                                data-confirm-term-content = "${message(code: 'confirmation.content.exportPartial')}"
                                data-confirm-term-how="ok" controller="myInstitution" action="currentProviders"
                                params="${params+[format:'csv']}">
                            ${message(code:'default.button.exports.csv')}
                        </g:link>
                    </semui:exportDropdownItem>
                </g:if>
                <g:else>
                    <semui:exportDropdownItem>
                        <g:link class="item" action="currentProviders" params="${params+[exportXLS:true]}">${message(code:'default.button.exports.xls')}</g:link>
                    </semui:exportDropdownItem>
                    <semui:exportDropdownItem>
                        <g:link class="item" action="currentProviders" params="${params+[format:'csv']}">${message(code:'default.button.exports.csv')}</g:link>
                    </semui:exportDropdownItem>
                </g:else>
            </semui:exportDropdown>
            <semui:actionsDropdown>

                <semui:actionsDropdownItem data-semui="modal" href="#copyEmailaddresses_ajaxModal" message="menu.institutions.copy_emailaddresses.button"/>

            </semui:actionsDropdown>

        </semui:controlButtons>


    <h1 class="ui left aligned icon header"><semui:headerIcon /><g:message code="menu.my.providers" />
        <semui:totalNumber total="${orgListTotal}"/>
    </h1>

    <semui:messages data="${flash}" />
    <semui:filter>
        <g:form action="currentProviders" method="get" class="ui form">
            <g:render template="/templates/filter/orgFilter"
                      model="[
                              propList: propList,
                              orgRoles: orgRoles,
                              tmplConfigShow: [['name', 'role'], ['country', 'property']],
                              tmplConfigFormFilter: true,
                              useNewLayouter: true
                      ]"/>
        </g:form>
    </semui:filter>

    <g:if test="${orgList}">
    <g:render template="/templates/filter/orgFilterTable"
              model="[orgList: orgList,
                      tmplShowCheckbox: false,
                      tmplConfigShow: ['lineNumber', 'shortname', 'name', 'privateContacts', 'numberOfSubscriptions']
              ]"/>
    </g:if>
    <g:else>
        <g:if test="${filterSet}">
            <br><strong><g:message code="filter.result.empty.object" args="${[message(code:"default.ProviderAgency.label")]}"/></strong>
        </g:if>
        <g:else>
            <br><strong><g:message code="result.empty.object" args="${message(code:"default.ProviderAgency.label")}"/></strong>
        </g:else>
    </g:else>

    <g:render template="../templates/copyEmailaddresses" model="[orgList: orgList]"/>

    <semui:paginate total="${orgListTotal}" params="${params}" max="${max}" offset="${offset}" />

  </body>
</html>
