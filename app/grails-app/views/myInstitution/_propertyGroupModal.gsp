<%@ page import="com.k_int.kbplus.License; com.k_int.kbplus.Org; com.k_int.kbplus.Subscription; com.k_int.properties.PropertyDefinition"%>

<semui:modal id="propDefGroupModal" message="propertyDefinitionGroup.create_new.label">

    <g:form class="ui form" url="[controller: 'myInstitution', action: 'managePropertyGroups']" method="POST">
        <input type="hidden" name="cmd" value="processing"/>
        <g:if test="${pdGroup}">
            <input type="hidden" name="oid" value="${pdGroup.class.name}:${pdGroup.id}"/>
        </g:if>

        <div class="ui two column grid">
            <div class="column">

                <div class="field">
                    <label>Kategorie</label>
                    <select name="prop_descr" id="prop_descr_selector" class="ui dropdown">
                        <g:each in="${PropertyDefinition.AVAILABLE_GROUPS_DESCR}" var="pdDescr">
                            <%-- TODO: REFACTORING --%>
                            <g:if test="${pdDescr == PropertyDefinition.LIC_PROP && pdGroup?.ownerType == License.class.name}">
                                <option selected="selected" value="${pdDescr}"><g:message code="propertyDefinition.${pdDescr}.label" default="${pdDescr}"/></option>
                            </g:if>
                            <g:elseif test="${pdDescr == PropertyDefinition.ORG_PROP && pdGroup?.ownerType == Org.class.name}">
                                <option selected="selected" value="${pdDescr}"><g:message code="propertyDefinition.${pdDescr}.label" default="${pdDescr}"/></option>
                            </g:elseif>
                            <g:elseif test="${pdDescr == PropertyDefinition.SUB_PROP && pdGroup?.ownerType == Subscription.class.name}">
                                <option selected="selected" value="${pdDescr}"><g:message code="propertyDefinition.${pdDescr}.label" default="${pdDescr}"/></option>
                            </g:elseif>
                            <g:else>
                                <option value="${pdDescr}"><g:message code="propertyDefinition.${pdDescr}.label" default="${pdDescr}"/></option>
                            </g:else>
                            <%-- TODO: REFACTORING --%>
                        </g:each>
                    </select>
                </div>

                <div class="field">
                    <label>Name</label>
                    <input type="text" name="name" value="${pdGroup?.name}"/>
                </div>

                <div class="field">
                    <label>Beschreibung</label>
                    <textarea name="description">${pdGroup?.description}</textarea>
                </div>
            </div>

            <div class="column">
                <div class="field">
                    <label>Merkmale</label>

                    <g:each in="${PropertyDefinition.AVAILABLE_GROUPS_DESCR}" var="pdDescr">
                        <table class="ui table la-table-small hidden" data-propDefTable="${pdDescr}">
                            <tbody>
                            <g:each in="${PropertyDefinition.findAllWhere(tenant:null, descr:pdDescr, [sort: 'name'])}" var="pd">
                                <tr>
                                    <td>
                                        ${pd.getI10n('name')}
                                    </td>
                                    <td>
                                        <input type="checkbox" disabled="disabled" name="propertyDefinition" value="${pd.id}" />
                                    </td>
                                </tr>
                            </g:each>
                            </tbody>
                        </table>
                    </g:each>
                </div>
            </div>
        </div>

    </g:form>
</semui:modal>

<script>
    var ajaxPostFunc = function() {

        var prop_descr_selector_controller = {
            init: function () {
                $('#propDefGroupModal #prop_descr_selector').on('change', function () {
                    prop_descr_selector_controller.changeTable($(this).val())
                })

                $('#propDefGroupModal #prop_descr_selector').trigger('change')
            },
            changeTable: function (target) {
                $('#propDefGroupModal .table').addClass('hidden')
                $('#propDefGroupModal .table input').attr('disabled', 'disabled')

                $('#propDefGroupModal .table[data-propDefTable="' + target + '"]').removeClass('hidden')
                $('#propDefGroupModal .table[data-propDefTable="' + target + '"] input').removeAttr('disabled')
            }
        }
        prop_descr_selector_controller.init()
        setTimeout( function(){ $(window).trigger('resize')}, 500)
    }
</script>