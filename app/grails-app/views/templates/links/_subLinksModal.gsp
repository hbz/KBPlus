<%@ page import="com.k_int.kbplus.*;de.laser.helper.RDStore;de.laser.interfaces.CalculatedType" %>
<g:if test="${editmode}">
    <a role="button" class="ui button ${tmplCss}" data-semui="modal" href="#${tmplModalID}">
        <g:if test="${tmplIcon}">
            <i class="${tmplIcon} icon"></i>
        </g:if>
        <g:if test="${tmplButtonText}">
            ${tmplButtonText}
        </g:if>
    </a>
</g:if>

<%
    String header, thisString, urlLookup
    switch(objectType) {
        case Subscription.class.name: header = message(code:"subscription.linking.header")
            thisString = message(code:"subscription.linking.this")
            urlLookup = "lookupSubscriptions"
            break
        case License.class.name: header = message(code:"license.linking.header")
            thisString = message(code:"license.linking.this")
            urlLookup = "lookupLicenses"
            break
    }
%>

<semui:modal id="${tmplModalID}" text="${tmplText}">
    <g:form id="link_${tmplModalID}" class="ui form" url="[controller: 'ajax', action: 'linkSubscriptions']" method="post">
        <input type="hidden" name="context" value="${context}"/>
        <%
            List<RefdataValue> refdataValues = RefdataCategory.getAllRefdataValues(de.laser.helper.RDConstants.LINK_TYPE)
            LinkedHashMap linkTypes = [:]
            refdataValues.each { rv ->
                String[] linkArray = rv.getI10n("value").split("\\|")
                linkArray.eachWithIndex { l, int perspective ->
                    linkTypes.put(rv.class.name+":"+rv.id+"§"+perspective,l)
                }
                if(link && link.linkType == rv) {
                    int perspIndex
                    if(context == objectType+":"+link.source) {
                        perspIndex = 0
                    }
                    else if(context == objectType+":"+link.destination) {
                        perspIndex = 1
                    }
                    else {
                        perspIndex = 0
                    }
                    linkType = "${rv.class.name}:${rv.id}§${perspIndex}"
                }
            }
        %>
        <g:if test="${link}">
            <g:set var="pair" value="${link.getOther(context)}"/>
            <g:set var="comment" value="${DocContext.findByLink(link)}"/>
            <g:set var="selectPair" value="pair_${link.id}"/>
            <g:set var="selectLink" value="linkType_${link.id}"/>
            <g:set var="linkComment" value="linkComment_${link.id}"/>
            <input type="hidden" name="link" value="${link.class.name}:${link.id}" />
            <g:if test="${comment}">
                <input type="hidden" name="commentID" value="${comment.owner.class.name}:${comment.owner.id}" />
            </g:if>
        </g:if>
        <g:else>
            <g:set var="selectPair" value="pair_new"/>
            <g:set var="selectLink" value="linkType_new"/>
            <g:set var="linkComment" value="linkComment_new"/>
        </g:else>
        <div class="field">
            <div id="sub_role_tab_${tmplModalID}" class="ui grid">
                <div class="row">
                    <div class="column">
                        ${header}
                    </div>
                </div>
                <div class="row">
                    <div class="four wide column">
                        ${thisString}
                    </div>
                    <div class="twelve wide column">
                        <g:select class="ui dropdown select la-full-width" name="${selectLink}" id="${selectLink}" from="${linkTypes}" optionKey="${{it.key}}"
                                  optionValue="${{it.value}}" value="${linkType ?: null}" noSelection="${['' : message(code:'default.select.choose.label')]}"/>
                    </div>
                </div>
                <div class="row">
                    <div class="four wide column">
                        <g:message code="${controllerName}" />
                    </div>
                    <div class="twelve wide column">
                        <div class="ui search selection dropdown la-full-width" id="${selectPair}">
                            <input type="hidden" name="${selectPair}" value="${pair?.class?.name}:${pair?.id}"/>
                            <i class="dropdown icon"></i>
                            <input type="text" class="search"/>
                            <div class="default text"></div>
                        </div>
                    </div>
                </div>
                <div class="row">
                    <div class="four wide column">
                        <g:message code="default.linking.comment" />
                    </div>
                    <div class="twelve wide column">
                        <g:textArea class="ui" name="${linkComment}" id="${linkComment}" value="${comment?.owner?.content}"/>
                    </div>
                </div>
            </div>
        </div>
    </g:form>
</semui:modal>
<%-- for that one day, we may move away from that ... --%>
<r:script>
    $(document).ready(function(){
       console.log("${urlLookup}");
        $("#${selectPair}").dropdown({
            apiSettings: {
                url: "<g:createLink controller="ajax" action="${urlLookup}"/>?status=FETCH_ALL&query={query}&filterMembers=true&ctx=${context}",
                cache: false
            },
            clearable: true,
            minCharacters: 0
        });
    });
</r:script>