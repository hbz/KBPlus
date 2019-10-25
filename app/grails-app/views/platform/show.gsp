<%@ page import="com.k_int.kbplus.ApiSource; com.k_int.kbplus.Platform" %>
<!doctype html>
<html>
<head>
    <meta name="layout" content="semanticUI">
    <g:set var="entityName" value="${message(code: 'platform.label', default: 'Platform')}"/>
    <title>${message(code: 'laser', default: 'LAS:eR')} : <g:message code="default.show.label"
                                                                     args="[entityName]"/></title>
    <g:javascript src="properties.js"/>
</head>

<body>

<semui:breadcrumbs>
    <semui:crumb controller="platform" action="index" message="platform.show.all"/>
    <semui:crumb class="active" id="${platformInstance.id}" text="${platformInstance.name}"/>
</semui:breadcrumbs>

<semui:modeSwitch controller="platform" action="show" params="${params}"/>

<h1 class="ui left aligned icon header"><semui:headerIcon/>

    <g:if test="${editable}"><span id="platformNameEdit"
                                   class="xEditableValue"
                                   data-type="textarea"
                                   data-pk="${platformInstance.class.name}:${platformInstance.id}"
                                   data-name="name"
                                   data-url='<g:createLink controller="ajax"
                                                           action="editableSetValue"/>'>${platformInstance.name}</span>
    </g:if>
    <g:else>${platformInstance.name}</g:else>
</h1>

<g:render template="nav"/>

<semui:messages data="${flash}"/>
<div id="collapseableSubDetails" class="ui stackable grid">
    <div class="twelve wide column">
        <div class="la-inline-lists">
          <div class="ui two stackable cards">
            <div class="ui card la-time-card">
              <div class="content">
                <dl>
                  <dt>${message(code: 'platform.name', default: 'Platform Name')}</dt>
                  <dd><semui:xEditable owner="${platformInstance}" field="name"/></dd>
                </dl>
                <dl>
                  <dt>GOKb ID</dt>
                  <dd>
                    ${platformInstance?.gokbId}
                    <g:each in="${com.k_int.kbplus.ApiSource.findAllByTypAndActive(ApiSource.ApiTyp.GOKBAPI, true)}"
                            var="gokbAPI">
                      <g:if test="${platformInstance?.gokbId}">
                        <a target="_blank"
                           href="${gokbAPI.editUrl ? gokbAPI.editUrl + '/gokb/resource/show/' + platformInstance?.gokbId : '#'}"><i
                            title="${gokbAPI.name} Link" class="external alternate icon"></i></a>
                      </g:if>
                    </g:each>
                  </dd>
                </dl>
                <dl>
                  <dt>${message(code: 'platform.org', default: 'Platform Provider')}</dt>
                  <dd>
                    <g:if test="${platformInstance.org}">
                      <g:link controller="organisation" action="show"
                              id="${platformInstance.org.id}">${platformInstance.org.name}</g:link>
                    </g:if>
                  </dd>
                </dl>
              </div>
            </div>
            <div class="ui card">
              <div class="content">
                <dl>
                  <dt>${message(code: 'platform.primaryUrl', default: 'Primary URL')}</dt>
                  <dd>
                    <semui:xEditable owner="${platformInstance}" field="primaryUrl"/>
                    <g:if test="${platformInstance?.primaryUrl}">
                      <a class="ui icon mini blue button la-js-dont-hide-button la-popup-tooltip la-delay"
                         data-content="${message(code: 'tipp.tooltip.callUrl')}"
                         href="${platformInstance?.primaryUrl?.contains('http') ? platformInstance?.primaryUrl : 'http://' + platformInstance?.primaryUrl}"
                         target="_blank"><i class="share square icon"></i></a>
                    </g:if>
                  </dd>
                </dl>
                <dl>
                  <dt>${message(code: 'platform.serviceProvider', default: 'Service Provider')}</dt>
                  <dd><semui:xEditableRefData owner="${platformInstance}" field="serviceProvider" config="YN"/></dd>
                </dl>
                <dl>
                  <dt>${message(code: 'platform.softwareProvider', default: 'Software Provider')}</dt>
                  <dd><semui:xEditableRefData owner="${platformInstance}" field="softwareProvider" config="YN"/></dd>
                </dl>
                <g:if test="${params.mode == 'advanced'}">
                  <dl>
                    <dt>${message(code: 'platform.type', default: 'Type')}</dt>
                    <dd><semui:xEditableRefData owner="${platformInstance}" field="type" config="YNO"/></dd>
                  </dl>
                  <dl>
                    <dt>${message(code: 'platform.status', default: 'Status')}</dt>
                    <dd><semui:xEditableRefData owner="${platformInstance}" field="status"
                                                config="UsageStatus"/></dd>
                  </dl>
                  <dl>
                    <dt><g:message code="platform.globalUID.label" default="Global UID"/></dt>
                    <dd><g:fieldValue bean="${platformInstance}" field="globalUID"/></dd>
                  </dl>
                </g:if>
              </div>
            </div>
          </div>
            <div id="new-dynamic-properties-block">

                <g:render template="properties" model="${[
                    platform: platformInstance,
                    /*authorizedOrgs: authorizedOrgs*/
                ]}"/>

            </div><!-- #new-dynamic-properties-block -->

            <div class="ui card">
                <div class="content">
                    <table class="ui three column table">
                        <g:each in="${orgAccessPointList}" var="orgAccessPoint">
                            <tr>
                                <th scope="row" class="control-label la-js-dont-hide-this-card">${message(code: 'platform.accessPoint', default: 'Access Configuration')}</th>
                                <td>
                                    <g:link controller="accessPoint" action="edit_${orgAccessPoint.oap.accessMethod}"  id="${orgAccessPoint.oap.id}">
                                        ${orgAccessPoint.oap.name}  (${orgAccessPoint.oap.accessMethod.getI10n('value')})
                                    </g:link>
                                </td>
                                <td class="right aligned">
                                <g:if test="${editable}">
                                    <g:link class="ui negative icon button button js-open-confirm-modal" controller="accessPoint" action="unlinkPlatform" id="${orgAccessPoint.id}"
                                            data-confirm-tokenMsg="${message(code: 'confirm.dialog.unlink.accessPoint.platform', args: [orgAccessPoint.oap.name, platformInstance.name])}"
                                            data-confirm-term-how="unlink"
                                    >
                                        <i class="unlink icon"></i>
                                    </g:link>
                                </g:if>

                                </td>
                            </tr>
                        </g:each>
                    </table>


                    <div class="ui la-vertical buttons">
                        <g:render template="/templates/links/accessPointLinksModal"
                                  model="${[tmplText:message(code:'platform.link.accessPoint.button.label'),
                                            tmplID:'addLink',
                                            tmplButtonText:message(code:'platform.link.accessPoint.button.label'),
                                            tmplModalID:'platf_link_ap',
                                            editmode: editable,
                                            accessPointList: accessPointList,
                                            institution:institution,
                                            selectedInstitution:selectedInstitution
                                  ]}" />
                    </div>
                </div>
            </div>

            <div class="clearfix"></div>
        </div>
    </div>
</div>

</body>
</html>
