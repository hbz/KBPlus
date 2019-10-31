<%@ page import="com.k_int.kbplus.OrgAccessPoint" %>
<!doctype html>
<html>
<head>
    <meta name="layout" content="semanticUI">
    <g:set var="entityName" value="${message(code: 'accessPoint.label', default: 'Access Point')}" />
    <title><g:message code="default.edit.label" args="[entityName]" /></title>
    <g:javascript>
        $(function() {
            $('body').attr('class', 'organisation_accessPoint_edit_'+accessPoint.accessMethod);
        });
    </g:javascript>
</head>

	<body>
            <div>
                <g:render template="breadcrumb" model="${[ accessPoint:accessPoint, params:params ]}"/>

                %{--<semui:controlButtons>
                  <g:render template="actions" />
                </semui:controlButtons>--}%

              <h1 class="ui left aligned icon header"><semui:headerIcon />
              ${orgInstance.name}
              </h1>

              <g:render template="/organisation/nav" model="${[orgInstance: accessPoint.org, inContextOrg: inContextOrg]}"/>
                <h1 class="ui header"><g:message code="default.edit.label" args="[entityName]" /></h1>
                <semui:messages data="${flash}" />

                <g:form class="ui form" url="[controller: 'accessPoint', action: 'edit_'+accessPoint.accessMethod ]" id="${accessPoint.id}" method="GET">
                    <g:hiddenField name="id" value="${accessPoint?.id}" />
                    <div class="la-inline-lists">
                        <div class="ui card">
                            <div class="content">
                                <dl>
                                    <dt><g:message code="org.name.label" default="Name" /></dt>
                                    <dd><semui:xEditable owner="${accessPoint}" field="name"/></dd>
                                </dl>
                                <dl>
                                    <dt><g:message code="accessMethod.label" default="Access Method" /></dt>
                                    <dd>
                                        ${accessPoint.accessMethod.getI10n('value')}
                                        <g:hiddenField name="accessMethod" value="${accessPoint.accessMethod.id}"/>
                                    </dd>
                                </dl>
                                <g:if test="${accessPoint.hasProperty('url')}">
                                <dl>
                                    <dt><g:message code="accessPoint.url" default="Proxy URL:" /></dt>
                                    <dd><semui:xEditable owner="${accessPoint}" field="url"/></dd>
                                </dl>
                                </g:if>

                                <g:if test="${accessPoint.hasProperty('entityId')}">
                                    <dl>
                                        <dt><g:message code="accessPoint.entityId" default="EntityId:" /></dt>
                                        <dd><semui:xEditable owner="${accessPoint}" field="entityId"/></dd>
                                    </dl>
                                </g:if>

                  <g:if test="${ !accessPoint.hasProperty('entityId') }">
            <dl >
                <dt><g:message code="accessPoint.range.plural" default="Addressbereiche" /></dt>
                <dd class="la-full-width">
                    <div class="two fields">
                        <div class="field">
                            <laser:select class="ui fluid icon  dropdown"  id="ipv4Format"
                                          name="ipv4Format"
                                          from="${com.k_int.kbplus.OrgAccessPoint.getAllRefdataValues('IPv4 Address Format')}"
                                          value="${ipv4Format}"
                                          optionKey="value"
                                          optionValue="value"
                                          onchange="submit()"
                            />
                            <div class="ui bulleted list">
                                    <g:each in="${ipv4Ranges}" var="ipv4Range">
                                        <div class="item">${ipv4Range}</div>
                                    </g:each>
                            </div>
                        </div>
                        <div class="field">
                            <laser:select class="ui fluid icon  dropdown"  id="ipv6Format"
                                          name="ipv6Format"
                                          from="${com.k_int.kbplus.OrgAccessPoint.getAllRefdataValues('IPv6 Address Format')}"
                                          value="${ipv6Format}"
                                          optionKey="value"
                                          optionValue="value"
                                          onchange="submit()"
                            />
                            <div class="ui bulleted list">
                                <g:each in="${ipv6Ranges}" var="ipv4Range">
                                    <div class="item" >${ipv6Range}</div>
                                </g:each>
                            </div>
                        </div>
                    </div>
                </dd>
        </dl>
                  </g:if>
    </div>
</div>
</g:form>
<g:if test="${ !accessPoint.hasProperty('entityId') }">
<g:form class="ui form" url="[controller: 'accessPoint', action: 'addIpRange']" id="${accessPoint.id}" method="POST">
<g:hiddenField name="id" value="${accessPoint?.id}" />
<g:hiddenField name="ipv4Format" value="${ipv4Format}" />
<g:hiddenField name="ipv6Format" value="${ipv6Format}" />
<g:hiddenField name="accessMethod" value="${accessPoint.accessMethod}" />

<h5>${message(code: 'accessPoint.ip.configuration', default: 'IP Configuration')}
<span class="la-long-tooltip la-popup-tooltip la-delay" data-html='${message(code:'accessPoint.permittedIpRanges')}'>
    <i class="question circle icon la-popup"></i>
</span>
    </h5>

<table  class="ui celled la-table table very compact">
    <thead>
    <tr>
        <g:sortableColumn property="ipData" title="${message(code: 'accessPoint.ip.data', default: 'IP or IP Range')} "  />
        <th>${message(code: 'accessPoint.actions', default: 'Actions')}</th>
    </tr>
    </thead>
    <tbody>
    <g:each in="${accessPointDataList}" var="accessPointData">
        <tr>
            <td>${accessPointData.getInputStr()}</td>
            <td class="center aligned">
                <g:link action="deleteIpRange" controller="accessPoint" id="${accessPointData.id}" class="ui negative icon mini button">
                    <i class="trash very alternate icon"></i>
                </g:link>
            </td>
        </tr>
    </g:each>

    <tr>
        <td>
            <div class="${hasErrors(bean: accessPoint, field: 'name', 'error')} ui form">
                <g:if test="${autofocus == true}">
                    <g:field type="text" name="ip" value="${ip}" autofocus=""/>
                </g:if>
                <g:else>
                    <g:field type="text" name="ip" value="${ip}"/>
                </g:else>
            </div>
        </td>
        <td class="center aligned">
            <input type="Submit" class="ui tiny button" value="${message(code:'accessPoint.button.add', default:'Add')}" onClick="this.form.submit()" class="ui button"/>
        </td>
    </tr>
    </tbody>
</table>

</g:form>
</g:if>
<g:render template="link" model="${[accessPoint:accessPoint, params:params, linkedPlatformsMap: linkedPlatformsMap, linkedSubscriptionsMap: linkedSubscriptionsMap]}"/>

%{--

                <g:form class="ui form" url="[controller: 'accessPoint', action: 'linkPlatform']" id="linkPlatform" method="POST">
                    <g:hiddenField name="id" value="${accessPoint?.id}" />
                    <table class="ui celled la-table table compact collapsing ignore-floatThead">
                        <thead>
                        <tr>
                            <g:sortableColumn property="platform" title="${message(code: "platform.label", default: "Platform")}" />
                            <th>${message(code: 'accessPoint.actions', default: 'Actions')}</th>
                        </tr>
                        </thead>
                        <tbody>
                        <g:each in="${linkedPlatformsMap}" var="linkedPlatform">
                            <tr>
                                <td><g:link controller="platform" action="show" id="${linkedPlatform.platform.id}">${linkedPlatform.platform.name}</g:link></td>
                                <td class="center aligned">
                                    <g:link class="ui negative icon button" controller="accessPoint" action="unlinkPlatform" id="${linkedPlatform.aplink.id}" onclick="return confirm('${message(code: "accessPoint.link.delete.confirm", default: "Remove Access Config?")}')">
                                        <i class="trash alternate icon"></i>
                                    </g:link>
                                </td>
                            </tr>
                        </g:each>
                        <tr>
                            <td>
                                <g:select id="platforms" class="ui dropdown" name="platforms"
                                          from="${platformList}"
                                          optionKey="id"
                                          optionValue="name"
                                          noSelection="${[null: message(code: 'default.select.choose.label')]}"/>
                            </td>
                            <td class="center aligned">
                                <input type="Submit" class="ui tiny button" value="${message(code:'accessPoint.button.linkPlatform', default:'Create link')}" onClick="this.form.submit()" class="ui button"/>
                            </td>
                        </tr>
                        </tbody>
                    </table>
                </g:form>


--}%



    </body>
</html>