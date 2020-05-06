<%@ page import="de.laser.helper.RDStore; com.k_int.kbplus.License; com.k_int.kbplus.RefdataValue" %>
<laser:serviceInjection />

<!doctype html>
<html>
<head>
  <meta name="layout" content="semanticUI"/>
  <title>${message(code:'laser')} : ${message(code:'license.details.incoming.childs',args:[message(code:'consortium.subscriber')])}</title>
</head>
<body>

    <g:render template="breadcrumb" model="${[ license:license, params:params ]}"/>

    <semui:controlButtons>
        <g:render template="actions" />
    </semui:controlButtons>

    <h1 class="ui icon header la-clear-before la-noMargin-top"><semui:headerIcon />
        <semui:xEditable owner="${license}" field="reference" id="reference"/>
    </h1>

<g:render template="nav" />

<table class="ui celled la-table table">
    <thead>
        <tr>
            <th>${message(code:'sidewide.number')}</th>
            <th>${message(code:'license.label')}</th>
            <th>${message(code:'subscriptionDetails.members.members')}</th>
            <th>${message(code:'default.status.label')}</th>
            <th class="la-action-info">${message(code:'default.actions.label')}</th>
        </tr>
    </thead>
    <tbody>

        <g:each in="${validMemberLicenses}" status="i" var="lic">
            <tr>
                <td>${i + 1}</td>
                <td>
                    <g:link controller="license" action="show" id="${lic.id}">${lic.genericLabel}</g:link>

                    <g:if test="${lic.isSlaved}">
                        <span class="la-popup-tooltip la-delay" data-position="top right" data-content="${message(code:'license.details.isSlaved.tooltip')}">
                            <i class="thumbtack blue icon"></i>
                        </span>
                    </g:if>
                </td>
                <td>
                    <g:each in="${lic.orgLinks}" var="orgRole">
                        <g:if test="${accessService.checkPerm("ORG_CONSORTIUM") && orgRole?.roleType.id in [RDStore.OR_LICENSEE_CONS.id, RDStore.OR_LICENSEE.id]}">
                            <g:link controller="organisation" action="show" id="${orgRole?.org.id}">
                                ${orgRole?.org.getDesignation()}
                            </g:link>
                            , ${orgRole?.roleType.getI10n('value')} <br />
                        </g:if>
                        <g:elseif test="${accessService.checkPerm("ORG_INST_COLLECTIVE") && orgRole?.roleType.id in [RDStore.OR_LICENSEE_COLL.id, RDStore.OR_LICENSEE.id]}">
                            <g:link controller="organisation" action="show" id="${orgRole?.org.id}">
                                ${orgRole?.org.getDesignation()}
                            </g:link>
                            , ${orgRole?.roleType.getI10n('value')} <br />
                        </g:elseif>

                        <%-- <g:if test="${license.isTemplate() && orgRole?.roleType.id in [RDStore.OR_LICENSING_CONSORTIUM.id]}">
                            <g:link controller="organisation" action="show" id="${orgRole?.org.id}">
                                ${orgRole?.org.getDesignation()}
                            </g:link>
                            , ${orgRole?.roleType.getI10n('value')} <br />
                        </g:if> --%>
                    </g:each>
                </td>
                <td>
                    ${lic.status.getI10n('value')}
                </td>
                <td class="x">
                    <g:if test="${editable}">
                        <g:link class="ui icon negative button" controller="license" action="delete" params="${[id:lic.id]}">
                            <i class="trash alternate icon"></i>
                        </g:link>
                    </g:if>
                </td>
            </tr>
        </g:each>

    </tbody>
</table>

</body>
</html>
