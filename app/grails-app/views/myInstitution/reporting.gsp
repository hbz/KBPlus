<!doctype html>
<html>
    <head>
        <meta name="layout" content="semanticUI"/>
        <title>${message(code:'laser')} : ${message(code:'myinst.reporting')}</title>
    </head>

    <p>
        <semui:breadcrumbs>
            <semui:crumb controller="myInstitution" action="dashboard" text="${institution.getDesignation()}"/>
            <semui:crumb text="${message(code:'myinst.reporting')}" class="active" />
        </semui:breadcrumbs>

        <g:if test="${params.subscription || params.package}">
            <%
                Map<String,Object> exportParams = [format:'xls']
                if(params.subscription)
                    exportParams.subscription = params.subscription
                else if(params.package)
                    exportParams.package = params.package
            %>
            <semui:controlButtons>
                <semui:exportDropdown>
                    <semui:exportDropdownItem>
                        <g:link class="item" action="reporting" params="${exportParams}">${message(code: 'default.button.export.xls')}</g:link>
                    </semui:exportDropdownItem>
                </semui:exportDropdown>
            </semui:controlButtons>
        </g:if>

        <p>
            <h1 class="ui icon header la-clear-before la-noMargin-top"><semui:headerIcon /><g:message code="myinst.reporting"/></h1>
        </p>

        <semui:filter>
            <g:form name="startingPoint" class="ui form" action="reporting">
                <div class="three fields">
                    <div class="field">
                        <label for="subscription">
                            <g:message code="subscription"/>
                        </label>
                        <g:select name="subscription" from="${subscriptions}" value="${params.subscription}"
                                  optionKey="${{it.id}}" optionValue="${{it.dropdownNamingConvention(institution)}}"
                                  noSelection="['':message(code:'default.select.choose.label')]"
                                  class="ui search selection dropdown"/>
                    </div>
                    <div class="field">
                        <label for="package">
                            <g:message code="package"/>
                        </label>
                        <g:select name="package" from="${packages}" value="${params.package}"
                                  optionKey="${{it.id}}" optionValue="${{it.name}}"
                                  noSelection="['':message(code:'default.select.choose.label')]"
                                  class="ui search selection dropdown"/>
                    </div>
                    <div class="field la-field-right-aligned">
                        <g:hiddenField name="formSubmit" value="true"/>
                        <a href="${createLink(action:'reporting')}" class="ui reset primary button">${message(code:'default.button.reset.label')}</a>
                        <input type="submit" class="ui secondary button" value="${message(code:'default.button.submit.label')}">
                    </div>
                </div>
            </g:form>
        </semui:filter>

        <g:if test="${formSubmit}">
            <%-- this is just for that we see something. Micha surely has concrete ideas which cause refactoring. --%>
            <g:if test="${costItems}">
                <table class="ui celled la-table table">
                    <thead>
                        <tr>
                            <th colspan="3"><g:message code="myinst.reporting.costItems"/></th>
                        </tr>
                    </thead>
                    <tbody>
                        <g:each in="${costItems}" var="row">
                            <tr>
                                <td>${row.getKey().getI10n("value")}</td>
                                <td>
                                    <ul>
                                        <g:each in="${row.getValue()}" var="ci">
                                            <li><g:formatNumber number="${ci.costInBillingCurrency}" type="currency" currencySymbol=""/> ${ci.billingCurrency ?: 'EUR'}</li>
                                        </g:each>
                                    </ul>
                                </td>
                            </tr>
                        </g:each>
                    </tbody>
                </table>
            </g:if>

        </g:if>
    </body>
</html>