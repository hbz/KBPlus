<%@ page import="de.laser.helper.RDStore;de.laser.helper.RDConstants;com.k_int.properties.PropertyDefinition;de.laser.interfaces.TemplateSupport" %>
<!doctype html>
<%-- r:require module="annotations" / --%>
<laser:serviceInjection />

<html>
  <head>
    <meta name="layout" content="semanticUI"/>
     <g:javascript src="properties.js"/>
    <title>${message(code:'laser')} : ${message(code:'license.details.label')}</title>
  </head>

    <body>

        <semui:debugInfo>
            <g:render template="/templates/debug/benchMark" model="[debug: benchMark]" />
            <g:render template="/templates/debug/orgRoles"  model="[debug: license.orgLinks]" />
            <g:render template="/templates/debug/prsRoles"  model="[debug: license.prsLinks]" />
        </semui:debugInfo>

        <g:render template="breadcrumb" model="${[ license:license, params:params ]}"/>

        <semui:controlButtons>
            <g:render template="actions" />
        </semui:controlButtons>

        <h1 class="ui icon header la-clear-before la-noMargin-top"><semui:headerIcon />
            <g:if test="${license.type?.value == 'Template'}">${message(code:'license.label')} (${license.type.getI10n('value')}):</g:if>
            <semui:xEditable owner="${license}" field="reference" id="reference"/>
            <%--<semui:auditButton auditable="[license, 'reference']" />--%>
        </h1>

        <g:render template="nav" />

        <semui:objectStatus object="${license}" status="${license.status}" />

        <g:if test="${! license.hasTemplate() && license.instanceOf && (contextOrg?.id == license.getLicensingConsortium()?.id)}">
            <div class="ui negative message">
                <div class="header"><g:message code="myinst.message.attention" /></div>
                <p>
                    <g:message code="myinst.licenseDetails.message.ChildView" />
                    <g:each in="${license.getAllLicensee()?.collect{itOrg -> itOrg.getDesignation()}}" var="licensee">
                        <span class="ui label">${licensee}</span>,
                    </g:each>

                    <g:message code="myinst.licenseDetails.message.ConsortialView" />
                    <g:link controller="license" action="show" id="${license.instanceOf?.id}">
                        <g:message code="myinst.subscriptionDetails.message.here" />
                    </g:link>.

                </p>
            </div>
        </g:if>

        <g:render template="/templates/meta/identifier" model="${[object: license, editable: editable]}" />

        <semui:messages data="${flash}" />

        <g:if test="${contextOrg.id == license.getLicensingConsortium()?.id || (! license.getLicensingConsortium() && contextOrg.id == license.getLicensee()?.id)}">
            <g:render template="/templates/pendingChanges" model="${['pendingChanges':pendingChanges, 'flash':flash, 'model':license]}"/>
        </g:if>

        <div class="ui stackable grid">

            <div class="twelve wide column">
                <semui:errors bean="${titleInstanceInstance}" />

                <!--<h4 class="ui header">${message(code:'license.details.information')}</h4>-->

                <div class="la-inline-lists">
                    <div class="ui two stackable cards">
                        <div class="ui card ">
                            <div class="content">
                                <dl>
                                    <dt class="control-label">${message(code: 'license.startDate')}</dt>
                                    <dd>
                                        <semui:xEditable owner="${license}" type="date" field="startDate" />
                                    </dd>
                                    <dd class="la-js-editmode-container"><semui:auditButton auditable="[license, 'startDate']" /></dd>
                                </dl>
                                <dl>
                                    <dt class="control-label">${message(code: 'license.endDate')}</dt>
                                    <dd>
                                        <semui:xEditable owner="${license}" type="date" field="endDate" />
                                    </dd>
                                    <dd class="la-js-editmode-container"><semui:auditButton auditable="[license, 'endDate']" /></dd>
                                </dl>
                            </div>
                        </div>
                        <div class="ui card ">
                            <div class="content">
                                <dl>
                                    <dt class="control-label">${message(code: 'default.status.label')}</dt>
                                    <dd>
                                        <semui:xEditableRefData owner="${license}" field="status" config="${RDConstants.LICENSE_STATUS}"/>
                                    </dd>
                                    <dd class="la-js-editmode-container"><semui:auditButton auditable="[license, 'status']" /></dd>
                                </dl>
                                <%--
                                <dl>

                                    <dt><label class="control-label" for="licenseCategory">${message(code:'license.licenseCategory', default:'License Category')}</label></dt>
                                    <dd>
                                        <semui:xEditableRefData owner="${license}" field="licenseCategory" config="${RDConstants.LICENSE_CATEGORY}"/>
                                    </dd>
                                </dl>
                                -->
                                <!--
                                <dl>
                                    <dt><label class="control-label" for="isPublic">${message(code:'license.isPublic', default:'Public?')}</label></dt>
                                    <dd>
                                        <semui:xEditableRefData owner="${license}" field="isPublic" config="${RDConstants.Y_N}" />
                                    </dd>
                                </dl>
                                --%>
                                <dl>
                                    <dt class="control-label">${message(code:'license.linktoLicense', default:'License Template')}</dt>

                                    <g:if test="${license.instanceOf}">
                                        <g:link controller="license" action="show" id="${license.instanceOf.id}">${license.instanceOf}</g:link>
                                    </g:if>
                                </dl>

                                <g:if test="${license.instanceOf}">
                                    <dl>
                                        <dt class="control-label">
                                            ${message(code:'license.details.linktoLicense.pendingChange', default:'Automatically Accept Changes?')}
                                        </dt>
                                        <dd>
                                            <semui:xEditableBoolean owner="${license}" field="isSlaved" />
                                        </dd>
                                    </dl>
                                </g:if>

                                <dl>
                                    <dt class="control-label">${message(code: 'license.isPublicForApi.label')}</dt>
                                    <dd><semui:xEditableBoolean owner="${license}" field="isPublicForApi" /></dd>
                                    <dd class="la-js-editmode-container"><semui:auditButton auditable="[license, 'isPublicForApi']"/></dd>
                                </dl>

                            </div>
                        </div>
                    </div>

                    <div class="ui card la-time-card">
                        <div class="content">

                            <g:if test="${license.subscriptions && ( license.subscriptions.size() > 0 )}">
                                <table class="ui three column table">
                                    <g:each in="${license.subscriptions.sort{it.name}}" status="i" var="sub">
                                        %{--<g:if test="${sub.status == RDStore.SUBSCRIPTION_CURRENT && contextOrg?.id in sub.orgRelations?.org?.id}">--}%
                                        <g:if test="${contextOrg?.id in sub.orgRelations?.org?.id}">
                                            <tr>
                                                <th scope="row">
                                                    <g:if test="${i < 1}">
                                                        ${message(code:'license.linkedSubscriptions')}
                                                    </g:if>
                                                </th>
                                                <td>
                                                    <g:link controller="subscription" action="show" id="${sub.id}">${sub.name }</g:link>
                                                </td>
                                                <td class="right aligned">
                                                    <g:if test="${editable}">
                                                        <div class="ui icon negative buttons">
                                                            <g:link class="ui button la-selectable-button js-open-confirm-modal" name="unlinkSubscription"
                                                                    controller="license" action="unlinkSubscription"
                                                                    params="['license':license.id, 'subscription':sub.id]"
                                                                    data-confirm-tokenMsg="${message(code: "confirm.dialog.unlink.licence.subscription", args:[sub.name])}"
                                                                    data-confirm-term-how="unlink">
                                                                <i class="unlink icon"></i>
                                                            </g:link>
                                                        </div>
                                                    </g:if>
                                                </td>
                                            </tr>
                                        </g:if>
                                    </g:each>
                                </table>
                            </g:if>
                            <g:else>
                                <dl>
                                    <dt class="control-label">${message(code:'default.subscription.label')}</dt>
                                    <dd>
                                        ${message(code:'license.noLinkedSubscriptions')}
                                    </dd>
                                </dl>
                            </g:else>

                            %{--<dl>--}%
                                %{--<dt></dt>--}%
                                %{--<dd>--}%
                                    <g:if test="${editable}">
                                        <g:form id="linkSubscription" class="ui form" name="linkSubscription" action="linkToSubscription">
                                            <input type="hidden" name="license" value="${license.id}"/>
                                            <div class="fields">
                                                <div class="field">
                                                    <div class="ui search selection dropdown" id="subscription">
                                                        <input type="hidden" name="subscription">
                                                        <i class="dropdown icon"></i>
                                                        <input type="text" class="search">
                                                        <div class="default text">${message(code:'subscription')}</div>
                                                    </div>
                                                </div>
                                                <div class="field">
                                                    <input type="submit" class="ui button" value="${message(code:'default.button.link.label')}"/>
                                                </div>
                                            </div>
                                        </g:form>
                                    </g:if>
                                %{--</dd>--}%
                            %{--</dl>--}%
                            <%--
                            <dl>

                                <dt><label class="control-label" for="${license.pkgs}">${message(code:'license.linkedPackages', default:'Linked Packages')}</label></dt>
                                <dd>
                                    <g:if test="${license.pkgs && ( license.pkgs.size() > 0 )}">
                                        <g:each in="${license.pkgs.sort{it.name}}" var="pkg">
                                            <g:link controller="package" action="show" id="${pkg.id}">${pkg.name}</g:link><br/>
                                        </g:each>
                                    </g:if>
                                    <g:else>
                                        ${message(code:'license.noLinkedPackages', default:'No currently linked packages.')}
                                    </g:else>
                                </dd>
                            </dl>
                            --%>
                            <dl>
                                <sec:ifAnyGranted roles="ROLE_ADMIN">

                                    <dt class="control-label">${message(code:'license.ONIX-PL-License')}</dt>
                                    <dd>
                                        <g:if test="${license.onixplLicense}">
                                            <g:link controller="onixplLicense" action="index" id="${license.onixplLicense?.id}">${license.onixplLicense.title}</g:link>
                                            <g:if test="${editable}">

                                                <div class="ui mini icon buttons">
                                                    <g:link class="ui button" controller="license" action="unlinkLicense" params="[license_id: license.id, opl_id: onixplLicense.id]">
                                                        <i class="unlink icon"> </i>${message(code:'default.button.unlink.label')}
                                                    </g:link>
                                                </div>

                                            </g:if>
                                        </g:if>
                                        <g:else>
                                            <g:link class="ui positive button" controller='licenseImport' action='doImport' params='[license_id: license.id]'>${message(code:'license.importONIX-PLlicense', default:'Import an ONIX-PL license')}</g:link>
                                        </g:else>
                                    </dd>

                                </sec:ifAnyGranted>
                            </dl>
                        </div>
                    </div>
                    <div class="ui card">
                        <div class="content">

                        <g:render template="/templates/links/orgLinksAsList"
                                  model="${[roleLinks: visibleOrgLinks,
                                            roleObject: license,
                                            roleRespValue: 'Specific license editor',
                                            editmode: editable,
                                            showPersons: true
                                  ]}" />

                        <g:render template="/templates/links/orgLinksSimpleModal"
                                  model="${[linkType: license?.class?.name,
                                            parent: license.class.name + ':' + license.id,
                                            property: 'orgLinks',
                                            recip_prop: 'lic',
                                            tmplRole: RDStore.OR_LICENSOR,
                                            tmplEntity: message(code:'license.details.tmplEntity'),
                                            tmplText: message(code:'license.details.tmplText'),
                                            tmplButtonText: message(code:'license.details.tmplButtonText'),
                                            tmplModalID:'osel_add_modal_lizenzgeber',
                                            editmode: editable,
                                            orgList: availableLicensorList,
                                            signedIdList: existingLicensorIdList
                                  ]}" />
                        </div>
                    </div>

                    <div id="new-dynamic-properties-block">

                        <g:render template="properties" model="${[
                                license: license,
                                authorizedOrgs: authorizedOrgs
                        ]}" />

                    </div><!-- #new-dynamic-properties-block -->

                </div>

                <div class="clearfix"></div>

            </div><!-- .twelve -->

            <aside class="four wide column la-sidekick">
                <g:render template="/templates/aside1" model="${[ownobj:license, owntp:'license']}" />
            </aside><!-- .four -->


        </div><!-- .grid -->

        <r:script>
            $(document).ready(function() {
              $("#subscription").dropdown({
                apiSettings: {
                    url: "<g:createLink controller="ajax" action="lookupSubscriptions_IndendedAndCurrent"/>" +
                            "?ltype=${license.getCalculatedType()}" +
                            "&query={query}",
                    cache: false
                },
                clearable: true,
                minCharacters: 0
              });
            });
        </r:script>
  </body>
</html>
