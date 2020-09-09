<%@page import="com.k_int.properties.PropertyDefinition;de.laser.helper.RDStore;de.laser.helper.RDConstants;com.k_int.kbplus.*;de.laser.*" %>
<%
    String unknownString = g.message(code:"default.compare.propertyNotSet")
%>
<laser:serviceInjection/>
<tr>
    <th>${key}</th>
    <g:each in="${licenses}" var="l">
        <g:if test="${propBinding && propBinding.get(l)?.isVisibleForConsortiaMembers}">
            <th>${l.reference}<span class="ui blue tag label">${message(code:'financials.isVisibleForSubscriber')}</span></th>
        </g:if>
        <g:else>
            <th>${l.reference}</th>
        </g:else>
    </g:each>
</tr>
<g:each in="${group}" var="prop">
    <%-- leave it for debugging
    <tr>
        <td colspan="999">${prop.getValue()}</td>
    </tr>--%>
    <%
        PropertyDefinition propKey = (PropertyDefinition) genericOIDService.resolveOID(prop.getKey())
    %>
    <tr>
        <td>${propKey.getI10n("name")}</td>
        <g:each in="${licenses}" var="l">
            <g:set var="propValues" value="${prop.getValue()}" />
            <g:if test="${propValues.containsKey(l)}">
                <td>
                    <g:each var="propValue" in="${propValues.get(l)}">
                        <%
                            String value
                            if(propValue.value) {
                                switch(propValue.type.type) {
                                    case "class ${RefdataValue.class.name}":
                                        String spanOpen = '<span data-content="'+propValue.refValue.getI10n("value")+'">'
                                        switch(propValue.refValue.owner) {
                                            case RefdataCategory.getByDesc(RDConstants.Y_N):
                                            case RefdataCategory.getByDesc(RDConstants.Y_N_O):
                                                switch(propValue.refValue) {
                                                    case RDStore.YN_YES:
                                                    case RDStore.YNO_YES: value = raw(spanOpen+'<i class="green thumbs up icon huge"></i></span>')
                                                        break
                                                    case RDStore.YN_NO:
                                                    case RDStore.YNO_NO: value = raw(spanOpen+'<i class="red thumbs down icon huge"></i></span>')
                                                        break
                                                    case RDStore.YNO_OTHER: value = raw(spanOpen+'<i class="yellow dot circle huge"></i></span>')
                                                        break
                                                }
                                                break
                                            case RefdataCategory.getByDesc(RDConstants.PERMISSIONS):
                                                switch(propValue.refValue){
                                                    case RDStore.PERM_PERM_EXPL: value = raw(spanOpen+'<i class="green check circle icon huge"></i></span>')
                                                        break
                                                    case RDStore.PERM_PERM_INTERP: value = raw(spanOpen+'<i class="green check circle outline icon huge"></i></span>')
                                                        break
                                                    case RDStore.PERM_PROH_EXPL: value = raw(spanOpen+'<i class="red times circle icon huge"></i></span>')
                                                        break
                                                    case RDStore.PERM_PROH_INTERP: value = raw(spanOpen+'<i class="red times circle outline icon huge"></i></span>')
                                                        break
                                                    case RDStore.PERM_SILENT: value = raw(spanOpen+'<i class="hand point up icon huge"></i></span>')
                                                        break
                                                    case RDStore.PERM_NOT_APPLICABLE: value = raw(spanOpen+'<i class="exclamation icon huge"></i></span>')
                                                        break
                                                    case RDStore.PERM_UNKNOWN: value = raw(spanOpen+'<i class="question circle icon huge"></i></span>')
                                                        break
                                                }
                                                break
                                            default: value = propValue.refValue.getI10n("value")
                                                break
                                        }
                                        break
                                    default: value = propValue.value
                                        break
                                }
                            }
                            else value = unknownString
                        %>
                        ${value}
                        <g:if test="${propValue?.paragraph}">
                            <div class="ui circular huge label la-long-tooltip la-popup-tooltip la-delay" data-content="${propValue?.paragraph}">§</div><br>
                        </g:if>
                    </g:each>
                </td>
            </g:if>
            <g:else>
                <td>
                    ${unknownString}
                </td>
            </g:else>
        </g:each>
    </tr>
</g:each>