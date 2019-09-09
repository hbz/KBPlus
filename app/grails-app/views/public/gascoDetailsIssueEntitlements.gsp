<%@ page import="com.k_int.kbplus.IssueEntitlement; com.k_int.kbplus.TitleInstancePackagePlatform; com.k_int.kbplus.OrgRole; com.k_int.kbplus.RefdataValue" %>
<%@ page import="com.k_int.kbplus.OrgRole;com.k_int.kbplus.RefdataCategory;com.k_int.kbplus.RefdataValue;com.k_int.properties.PropertyDefinition" %>

<!doctype html>
<html>
<head>
    <meta name="layout" content="semanticUI">
    <title>${message(code: 'laser', default: 'LAS:eR')} : ${message(code: 'gasco.title')}</title>
</head>

<body>

    <br />
    <br />

    <h2 class="ui title">
        ${subscription}
        <g:if test="${issueEntitlementsCount}">
            &nbsp;&nbsp;
            (${issueEntitlements?.size()} von ${issueEntitlementsCount})
        </g:if>
    </h2>

    <semui:filter>
        <form class="ui form">
            <div class="fields">

                <div class="field">
                    <label for="q">Suche nach Name</label>
                    <input type="text" id="q" name="q" placeholder="${message(code:'default.search.ph', default:'enter search term...')}" value="${params.q}" />
                </div>

                <div class="field">
                    <label for="idns">Suche nach Identifikatoren</label>
                    <g:select id="idns" name="idns"
                              from="${idnsPreset}" optionKey="id" optionValue="ns"
                              value="${params.idns}"
                              class="ui dropdown"
                              noSelection="['':'']"
                    />
                </div>

                <div class="field">
                    <label>&nbsp;</label>
                    <input type="text" name="idv" placeholder="${message(code:'default.search.ph', default:'enter search term...')}" value="${params.idv}" />
                </div>

                <div class="field">
                    <label>&nbsp;</label>
                    <a href="${request.forwardURI}" class="ui reset primary primary button">${message(code:'default.button.reset.label')}</a>

                    <input type="submit" class="ui secondary button" value="${message(code:'default.button.filter.label', default:'Filter')}" />
                </div>

            </div>
        </form>
    </semui:filter>

    <table class="ui celled la-table table">
        <thead>
            <th>${message(code:'sidewide.number')}</th>
            <th>${message(code:'issueEntitlement.label')}</th>
            <th>${message(code:'default.identifiers.label')}</th>
        </thead>
        <tbody>

            <g:each in="${issueEntitlements}" var="issueEntitlement" status="counter">
                <g:set var="tipp" value="${issueEntitlement.tipp}"/>
                <tr>
                    <td>${counter + 1}</td>
                    <td>
                        <semui:listIcon type="${tipp.title?.type?.value}"/>
                        <strong>${tipp.title.title}</strong>
                        <br />

                        <g:if test="${tipp.hostPlatformURL}">
                            <a href="${tipp.hostPlatformURL}" title="${tipp.hostPlatformURL}" target="_blank">
                                ${tipp.hostPlatformURL}
                                <i class="ui icon share square"></i>
                            </a>
                        </g:if>
                        <br />

                        ${message(code:'tipp.platform', default:'Platform')}:
                        <g:if test="${tipp.platform.name}">
                            ${tipp.platform.name}
                        </g:if>
                        <g:else>
                            ${message(code:'default.unknown')}
                        </g:else>
                        <br />

                        ${message(code:'tipp.package', default:'Package')}:
                        <g:if test="${tipp.pkg}"><!-- TODO: show all packages -->
                            ${tipp.pkg}
                        </g:if>
                        <g:else>
                            ${message(code:'default.unknown')}
                        </g:else>
                        <br />
                    </td>

                    <td>
                        <g:each in="${tipp.title?.ids?.sort{it?.identifier?.ns?.ns}}" var="title_id">
                            <g:if test="${title_id.identifier.ns.ns.toLowerCase() != 'originediturl'}">
                                ${title_id.identifier.ns.ns}: <strong>${title_id.identifier.value}</strong>
                                <br />
                            </g:if>
                        </g:each>
                    </td>
                </tr>
            </g:each>

        </tbody>
    </table>

<%--
<table class="ui celled la-table table">
    <thead>
    <th>${message(code:'sidewide.number')}</th>
    <th>${message(code:'issueEntitlement.label')}</th>
    <th>${message(code:'default.identifiers.label')}</th>
    </thead>
    <tbody>

    <g:each in="${subscription.issueEntitlements}" var="ie" status="counter">
        <tr>
            <td>${counter + 1}</td>
            <td>
                <semui:listIcon type="${ie.tipp?.title?.type?.value}"/>
                <strong>${ie.tipp.title.title}</strong>
                <br />

                <g:if test="${ie.tipp?.hostPlatformURL}">
                    <a href="${ie.tipp?.hostPlatformURL}" title="${ie.tipp?.hostPlatformURL}" target="_blank">
                        ${ie.tipp?.hostPlatformURL}
                        <i class="ui icon share square"></i>
                    </a>
                </g:if>
                <br />

                <g:if test="${ie.medium}">
                    ${ie.medium}
                    <br />
                </g:if>

                ${message(code:'tipp.platform', default:'Platform')}:
                <g:if test="${ie.tipp?.platform.name}">
                    ${ie.tipp?.platform.name}
                </g:if>
                <g:else>
                    ${message(code:'default.unknown')}
                </g:else>
                <br />

                ${message(code:'tipp.package', default:'Package')}:
                <g:if test="${ie.tipp?.pkg}">
                    ${ie.tipp?.pkg}
                </g:if>
                <g:else>
                    ${message(code:'default.unknown')}
                </g:else>
                <br />

                <g:if test="${ie.availabilityStatus?.value=='Expected'}">
                    ${message(code:'default.on', default:'on')} <g:formatDate format="${session.sessionPreferences?.globalDateFormat}" date="${ie.accessStartDate}"/>
                </g:if>

                <g:if test="${ie.availabilityStatus?.value=='Expired'}">
                    ${message(code:'default.on', default:'on')} <g:formatDate format="${session.sessionPreferences?.globalDateFormat}" date="${ie.accessEndDate}"/>
                </g:if>

            </td>

            <td>
                <g:each in="${ie?.tipp?.title?.ids?.sort{it?.identifier?.ns?.ns}}" var="title_id">
                    <g:if test="${title_id.identifier.ns.ns.toLowerCase() != 'originediturl'}">
                        ${title_id.identifier.ns.ns}: <strong>${title_id.identifier.value}</strong>
                        <br />
                    </g:if>
                </g:each>
            </td>
        </tr>
    </g:each>

    </tbody>
</table>
--%>
<sec:ifAnyGranted roles="ROLE_USER">
    <style>
    .ui.table thead tr:first-child>th {
        top: 90px!important;
    }
    </style>
</sec:ifAnyGranted>

<style>
.ui.table thead tr:first-child>th {
    top: 48px!important;
}
</style>
</body>