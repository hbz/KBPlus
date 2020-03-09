<%@ page import="de.laser.domain.I10nTranslation; com.k_int.properties.PropertyDefinition" %>
<%@ page import="grails.plugin.springsecurity.SpringSecurityUtils" %>
<!doctype html>
<html>
    <head>
        <meta name="layout" content="semanticUI">
        <g:set var="entityName" value="${message(code: 'org.label')}" />
        <title>${message(code:'laser')} : ${message(code: 'menu.institutions.prop_defs')}</title>
    </head>
    <body>

    <semui:breadcrumbs>
        <semui:crumb message="menu.institutions.manage_props" class="active" />
    </semui:breadcrumbs>
    <br>
    <h1 class="ui icon header la-clear-before la-noMargin-top"><semui:headerIcon />${message(code: 'menu.institutions.manage_props')}</h1>

    <g:render template="nav" />

    <semui:messages data="${flash}" />

    <g:if test="${false}">
        <div class="content ui form">
            <div class="fields">
                <div class="field">
                    <button class="ui button" value="" data-href="#addPropertyDefinitionModal" data-semui="modal" >${message(code:'propertyDefinition.create_new.label')}</button>
                </div>
            </div>
        </div>
    </g:if>
    <g:else>
        <br />
        <br />
    </g:else>
		<div class="ui styled fluid accordion">
			<g:each in="${propertyDefinitions}" var="entry">
                <div class="title">
                    <i class="dropdown icon"></i>
                    <g:message code="propertyDefinitions.${entry.key}.label" default="${entry.key}" />
                </div>
                <div class="content">
                    <table class="ui celled la-table la-table-small table">
                        <thead>
                        <tr>
                            <th></th>
                            <th>${message(code:'propertyDefinition.key.label')}</th>

                            <g:if test="${language?.toLowerCase() in ['de_de', 'de']}">
                                <g:set var="SUBSTITUTE" value="de" />
                                <th>${message(code:'default.name.label')}</th>
                                <th>${message(code:'propertyDefinition.expl.label')}</th>
                            </g:if>
                            <g:else>
                                <g:set var="SUBSTITUTE" value="en" />
                                <th>${message(code:'default.name.label')}</th>
                                <th>${message(code:'propertyDefinition.expl.label')}</th>
                            </g:else>
                            <th>${message(code:'default.type.label')}</th>
                            <%--<th class="la-action-info">${message(code:'default.actions.label')}</th>--%>
                        </tr>
                        </thead>
                        <tbody>
                            <g:each in="${entry.value.sort{it."name_${SUBSTITUTE}".toLowerCase()}}" var="pd">
                                <tr>
                                    <td>
                                        <g:if test="${pd.isHardData}">
                                            <span data-position="top left"  class="la-popup-tooltip la-delay" data-content="${message(code:'default.hardData.tooltip')}">
                                                <i class="check circle icon green"></i>
                                            </span>
                                        </g:if>
                                        <g:if test="${pd.multipleOccurrence}">
                                            <span data-position="top right"  class="la-popup-tooltip la-delay" data-content="${message(code:'default.multipleOccurrence.tooltip')}">
                                                <i class="redo icon orange"></i>
                                            </span>
                                        </g:if>

                                        <g:if test="${pd.isUsedForLogic}">
                                            <span data-position="top left"  class="la-popup-tooltip la-delay" data-content="${message(code:'default.isUsedForLogic.tooltip')}">
                                                <i class="ui icon orange cube"></i>
                                            </span>
                                        </g:if>
                                    </td>
                                    <td>
                                        <g:if test="${pd.isUsedForLogic}">
                                            <span style="color:orange">${fieldValue(bean: pd, field: "name")}</span>
                                        </g:if>
                                        <g:else>
                                            ${fieldValue(bean: pd, field: "name")}
                                        </g:else>
                                    </td>
                                    <td>
                                        <g:if test="${!pd.isHardData && SpringSecurityUtils.ifAnyGranted('ROLE_YODA')}">
                                            <semui:xEditable owner="${pd}" field="name_${SUBSTITUTE}" />
                                        </g:if>
                                        <g:else>
                                            ${pd.getI10n('name')}
                                        </g:else>
                                    </td>
                                    <td>
                                        <g:if test="${!pd.isHardData && SpringSecurityUtils.ifAnyGranted('ROLE_YODA')}">
                                            <semui:xEditable owner="${pd}" field="expl_${SUBSTITUTE}" type="textarea" />
                                        </g:if>
                                        <g:else>
                                            ${pd.getI10n('expl')}
                                        </g:else>
                                    </td>
                                    <td>
                                        ${PropertyDefinition.getLocalizedValue(pd?.type)}
                                        <g:if test="${pd?.type == 'class com.k_int.kbplus.RefdataValue'}">
                                            <g:set var="refdataValues" value="${[]}"/>
                                            <g:each in="${com.k_int.kbplus.RefdataCategory.getAllRefdataValues(pd.refdataCategory)}" var="refdataValue">
                                                <g:set var="refdataValues" value="${refdataValues + refdataValue?.getI10n('value')}"/>
                                            </g:each>
                                            <br>
                                            (${refdataValues.join('/')})
                                        </g:if>
                                    </td>
                                    <%--<td class="x">

                                        <g:if test="${false}">
                                        <sec:ifAnyGranted roles="ROLE_YODA">
                                            <g:if test="${usedPdList?.contains(pd.id)}">
                                                <span class="la-popup-tooltip la-delay" data-position="top right" data-content="${message(code:'propertyDefinition.exchange.label')}">
                                                    <button class="ui icon button" data-href="#replacePropertyDefinitionModal" data-semui="modal"
                                                            data-xcg-pd="${pd.class.name}:${pd.id}"
                                                            data-xcg-type="${pd.type}"
                                                            data-xcg-rdc="${pd.refdataCategory}"
                                                            data-xcg-debug="${pd.getI10n('name')} (${pd.name})"
                                                    ><i class="exchange icon"></i></button>
                                                </span>
                                            </g:if>
                                        </sec:ifAnyGranted>

                                        <g:if test="${! pd.isHardData && ! usedPdList?.contains(pd.id)}">
                                            <g:link controller="admin" action="managePropertyDefinitions"
                                                    params="${[cmd: 'deletePropertyDefinition', pd: 'com.k_int.properties.PropertyDefinition:' + pd.id]}" class="ui icon negative button">
                                                <i class="trash alternate icon"></i>
                                            </g:link>
                                        </g:if>
                                        </g:if>
                                    </td>--%>

                                </tr>
                            </g:each>

                        </tbody>
                    </table>
                </div>
			</g:each>
        </div>

        <g:if test="${false}">
        <semui:modal id="replacePropertyDefinitionModal" message="propertyDefinition.exchange.label" isEditModal="isEditModal">
            <g:form class="ui form" url="[controller: 'admin', action: 'managePropertyDefinitions']">
                <input type="hidden" name="cmd" value="replacePropertyDefinition"/>
                <input type="hidden" name="xcgPdFrom" value=""/>

                <p>
                    <strong>WARNUNG</strong>
                </p>

                <p>
                    Alle Vorkommen von <strong class="xcgInfo"></strong> in der Datenbank durch folgende Eigenschaft ersetzen:
                </p>

                <div class="field">
                    <label for="xcgPdTo">&nbsp;</label>
                    <select id="xcgPdTo"></select>
                </div>

                <p>
                    Die gesetzten Werte bleiben erhalten!
                </p>

            </g:form>

            <r:script>
                        $('button[data-xcg-pd]').on('click', function(){

                            var pd = $(this).attr('data-xcg-pd');
                            //var type = $(this).attr('data-xcg-type');
                            //var rdc = $(this).attr('data-xcg-rdc');

                            $('#replacePropertyDefinitionModal .xcgInfo').text($(this).attr('data-xcg-debug'));
                            $('#replacePropertyDefinitionModal input[name=xcgPdFrom]').attr('value', pd);

                            $.ajax({
                                url: '<g:createLink controller="ajax" action="propertyAlternativesSearchByOID"/>' + '?oid=' + pd + '&format=json',
                                success: function (data) {
                                    var select = '<option></option>';
                                    for (var index = 0; index < data.length; index++) {
                                        var option = data[index];
                                        if (option.value != pd) {
                                            select += '<option value="' + option.value + '">' + option.text + '</option>';
                                        }
                                    }
                                    select = '<select id="xcgPdTo" name="xcgPdTo" class="ui search selection dropdown">' + select + '</select>';

                                    $('label[for=xcgPdTo]').next().replaceWith(select);

                                    $('#xcgPdTo').dropdown({
                                        duration: 150,
                                        transition: 'fade'
                                    });

                                }, async: false
                            });
                        })
            </r:script>

        </semui:modal>


        <semui:modal id="addPropertyDefinitionModal" message="propertyDefinition.create_new.label">

            <g:form class="ui form" id="create_cust_prop" url="[controller: 'ajax', action: 'addCustomPropertyType']" >
                <input type="hidden" name="reloadReferer" value="/admin/managePropertyDefinitions"/>
                <input type="hidden" name="ownerClass" value="${this.class}"/>

				<div class="field">
                	<label class="property-label">Name</label>
                	<input type="text" name="cust_prop_name"/>
                </div>

                <div class="fields">
                    <div class="field five wide">
                        <label class="property-label">Context:</label>
                        <%--<g:select name="cust_prop_desc" from="${PropertyDefinition.AVAILABLE_CUSTOM_DESCR}" />--%>
                        <select name="cust_prop_desc" id="cust_prop_desc" class="ui dropdown">
                            <g:each in="${PropertyDefinition.AVAILABLE_CUSTOM_DESCR}" var="pd">
                                <option value="${pd}"><g:message code="propertyDefinition.${pd}.label" default="${pd}"/></option>
                            </g:each>
                        </select>
                    </div>
                    <div class="field five wide">
                        <label class="property-label"><g:message code="default.type.label" /></label>
                        <g:select class="ui dropdown"
                            from="${PropertyDefinition.validTypes2.entrySet()}"
                            optionKey="key" optionValue="${{PropertyDefinition.getLocalizedValue(it.key)}}"
                            name="cust_prop_type"
                            id="cust_prop_modal_select" />
                    </div>
                    <div class="field five wide">
                        <label class="property-label">${message(code:'propertyDefinition.expl.label', default:'Explanation')}</label>
                        <textarea name="cust_prop_expl" id="eust_prop_expl" class="ui textarea"></textarea>
                    </div>

                    <div class="field six wide hide" id="cust_prop_ref_data_name">
                        <label class="property-label"><g:message code="refdataCategory.label" /></label>
                        <input type="hidden" name="refdatacategory" id="cust_prop_refdatacatsearch"/>
                    </div>
                </div>

                <div class="fields">
                    <div class="field five wide">
                        <label class="property-label">${message(code:'default.multipleOccurrence.tooltip')}</label>
                        <g:checkBox type="text" name="cust_prop_multiple_occurence" />
                    </div>
                </div>

            </g:form>

        </semui:modal>

		<g:javascript>

			   if( $( "#cust_prop_modal_select option:selected" ).val() == "class com.k_int.kbplus.RefdataValue") {
					$("#cust_prop_ref_data_name").show();
			   } else {
                     $("#cust_prop_ref_data_name").hide();
                }

			$('#cust_prop_modal_select').change(function() {
				var selectedText = $( "#cust_prop_modal_select option:selected" ).val();
				if( selectedText == "class com.k_int.kbplus.RefdataValue") {
					$("#cust_prop_ref_data_name").show();
				}else{
					$("#cust_prop_ref_data_name").hide();
				}
			});

			$("#cust_prop_refdatacatsearch").select2({
				placeholder: "Kategorie eintippen...",
                minimumInputLength: 1,

                formatInputTooShort: function () {
                    return "${message(code:'select2.minChars.note', default:'Please enter 1 or more character')}";
                },
                formatNoMatches: function() {
                    return "${message(code:'select2.noMatchesFound')}";
                },
                formatSearching:  function() {
                    return "${message(code:'select2.formatSearching')}";
                },
				ajax: { // instead of writing the function to execute the request we use Select2's convenient helper
					url: '${createLink(controller:'ajax', action:'lookup')}',
					dataType: 'json',
					data: function (term, page) {
						return {
							q: term, // search term
							page_limit: 10,
							baseClass:'com.k_int.kbplus.RefdataCategory'
						};
					},
					results: function (data, page) {
						return {results: data.values};
					}
				}
			});

		</g:javascript>
        </g:if>
	</body>
</html>
