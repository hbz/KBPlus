<%@ page import="com.k_int.kbplus.Org; com.k_int.kbplus.OrgSettings; com.k_int.properties.PropertyDefinition" %>
<%@ page import="grails.plugin.springsecurity.SpringSecurityUtils" %>
<laser:serviceInjection />

<!doctype html>
<html>
    <head>
        <meta name="layout" content="semanticUI">
            <title>${message(code:'laser', default:'LAS:eR')} : ${message(code:'org.confProperties')} &amp; ${message(code:'org.orgSettings')}</title>
            <g:javascript src="properties.js"/>
    </head>
    <body>

        <g:render template="breadcrumb" model="${[ orgInstance:orgInstance, params:params ]}"/>

        <%--<semui:controlButtons>
            <g:render template="actions" model="${[ org:orgInstance, user:user ]}"/>
        </semui:controlButtons>--%>

        <h1 class="ui left aligned icon header"><semui:headerIcon />${orgInstance.name} - ${message(code:'org.nav.options')}</h1>

        <semui:objectStatus object="${orgInstance}" status="${orgInstance.status}" />

        <g:render template="nav" model="${[orgInstance: orgInstance, inContextOrg: orgInstance.id == contextService.getOrg().id]}"/>

        <semui:messages data="${flash}" />


        <div class="ui stackable grid">
            <div class="sixteen wide column">

                <div class="la-inline-lists">

                    <div class="ui card la-dl-no-table la-js-hideable">
                        <div class="content">
                            <h5 class="ui header">
                                ${message(code:'org.confProperties')}
                            </h5>

                            <div id="custom_props_div_1">
                                <g:render template="/templates/properties/custom" model="${[
                                        prop_desc: PropertyDefinition.ORG_CONF,
                                        ownobj: orgInstance,
                                        orphanedProperties: orgInstance.customProperties,
                                        custom_props_div: "custom_props_div_1" ]}"/>
                            </div>
                        </div><!-- .content -->
                    </div><!-- .card -->

                    <r:script language="JavaScript">
                        $(document).ready(function(){
                            c3po.initProperties("<g:createLink controller='ajax' action='lookup'/>", "#custom_props_div_1");
                        });
                    </r:script>

                    <div class="ui card la-dl-no-table la-js-hideable">
                        <div class="content">
                            <h5 class="ui header">
                                ${message(code:'org.orgSettings')}
                            </h5>

                            <table class="ui la-table table">
                                <thead>
                                <tr>
                                    <th>Merkmal</th>
                                    <th>Wert</th>
                                </tr>
                                </thead>
                                <tbody>
                                <g:each in="${settings}" var="os">
                                    <tr>
                                        <td>
                                            ${message(code:"org.setting.${os.key}", default: "${os.key}")}

                                            <g:if test="${'OAMONITOR_SERVER_ACCESS'.equals(os.key.toString())}">
                                                <span class="la-popup-tooltip la-delay" data-content="${message(code:'org.setting.OAMONITOR_SERVER_ACCESS.tooltip')}">
                                                    <i class="question circle icon"></i>
                                                </span>
                                            </g:if>
                                        </td>
                                        <td>
                                            <g:if test="${os.key in OrgSettings.getEditableSettings()}">
                                                <g:if test="${os.rdValue}">
                                                    <semui:xEditableRefData owner="${os}" field="rdValue" config="${os.key.rdc}" />
                                                </g:if>
                                                <g:elseif test="${os.roleValue}">
                                                    ${os.getValue()?.getI10n('authority')} (Editierfunktion deaktiviert) <%-- TODO --%>
                                                </g:elseif>
                                                <g:else>
                                                    <semui:xEditable owner="${os}" field="strValue" />
                                                </g:else>
                                            </g:if>
                                            <g:else>
                                                <g:if test="${os.rdValue}">
                                                    ${os.getValue()?.getI10n('value')}
                                                </g:if>
                                                <g:elseif test="${os.roleValue}">
                                                    ${os.getValue()?.getI10n('authority')}
                                                </g:elseif>
                                                <g:else>
                                                    ${os.getValue()}
                                                </g:else>
                                            </g:else>
                                        </td>
                                    </tr>
                                </g:each>
                                </tbody>
                        </table>
                        </div><!-- .content -->
                    </div>

                </div><!-- .la-inline-lists -->

            </div><!-- .twelve -->
        </div><!-- .grid -->

    </body>
</html>
