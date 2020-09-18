<%@page import="de.laser.RefdataCategory; com.k_int.kbplus.RefdataValue; de.laser.helper.RDConstants" %>
<%
    List notAvailable = [RefdataValue.getByValueAndCategory('ONIX-PL License', RDConstants.DOCUMENT_TYPE),
                         RefdataValue.getByValueAndCategory('Note', RDConstants.DOCUMENT_TYPE),
                         RefdataValue.getByValueAndCategory('Announcement', RDConstants.DOCUMENT_TYPE)]
    List documentTypes = RefdataCategory.getAllRefdataValues(RDConstants.DOCUMENT_TYPE)-notAvailable
%>
<semui:filter>
    <g:form id="documentFilter" class="ui form" controller="${controllerName}" action="documents">
        <div class="two fields">
            <div class="field">
                <label for="docTitle">${message(code:'license.docs.table.title')}</label>
                <input type="text" id="docTitle" name="docTitle" value="${params.docTitle}">
            </div>
            <div class="field">
                <label for="docFilename">${message(code:'license.docs.table.fileName')}</label>
                <input type="text" id="docFilename" name="docFilename" value="${params.docFilename}">
            </div>
        </div>
        <div class="two fields">
            <div class="field">
                <label for="docCreator">${message(code:'license.docs.table.creator')}</label>
                <g:select class="ui fluid search dropdown" id="docCreator" name="docCreator" from="${availableUsers}" optionKey="id" optionValue="display" value="${params.docCreator}"
                          noSelection="${['': message(code: 'default.select.choose.label')]}" />
            </div>
            <div class="field">
                <label for="docType">${message(code:'license.docs.table.type')}</label>
                <g:select class="ui fluid search dropdown" id="docType" name="docType" from="${documentTypes}" optionKey="id" optionValue="${{it.getI10n("value")}}" value="${params.docType}"
                          noSelection="${['': message(code: 'default.select.choose.label')]}" />
            </div>
        </div>
    <%--
            <div class="two fields">
                <div class="field">
                  <label for="docOwnerOrg">${message(code:'org.docs.table.ownerOrg')}</label>
                  <g:select class="ui fluid search dropdown" name="docOwnerOrg" from="${Org.executeQuery("select o from Org o order by o.name")}" optionKey="id" optionValue="name" value="${params.docOwnerOrg}" noSelection="${['' : message(code:'default.select.choose.label')]}"/>
                </div>--%>
            <%--<div class="field">
              <label for="docShareConf">${message(code:'template.addDocument.shareConf')}</label>
              <laser:select name="docShareConf" class="ui dropdown fluid" value="${params.docShareConf}" noSelection="${['' : message(code:'default.select.choose.label')]}"
                            from="${RefdataValue.executeQuery("select rdv from RefdataValue rdv where rdv.owner.desc = '"+RDConstants.SHARE_CONFIGURATION+"' order by rdv.order asc")}" optionKey="id" optionValue="value"/>
            </div>
        </div>
        --%>

    <%-- taken off for eventual reactivation
            <div class="two fields">
                <div class="field">
                    <div class="ui checkbox targetList" id="noTarget">
                        <g:checkBox name="noTarget" value="${params.noTarget}"/><label for="noTarget">${message(code:'license.document.attachment.noTarget')}</label>
                    </div>
                    <div class="ui checkbox targetList" id="org">
                        <g:checkBox name="org" value="${params.org}"/><label for="org">${message(code:'license.document.attachment.org')}</label>
                    </div>
                    <div class="ui checkbox targetList" id="license">
                        <g:checkBox name="license" value="${params.license}"/><label for="license">${message(code:'license.document.attachment.license')}</label>
                    </div>
                    <div class="ui checkbox targetList" id="subscription">
                        <g:checkBox name="subscription" value="${params.subscription}"/><label for="subscription">${message(code:'license.document.attachment.subscription')}</label>
                    </div>
                    <div class="ui checkbox targetList" id="package">
                        <g:checkBox name="pkg" value="${params.pkg}"/><label for="package">${message(code:'license.document.attachment.package')}</label>
                    </div>

                    <label for="docTarget"></label>
                    <g:select class="ui fluid search dropdown" name="docTarget" id="docTarget" from="" optionKey="id" optionValue="name" value="${params.docTarget}"/>
                    <label>${message(code:'org.docs.table.target')}</label>
                    <div class="ui multiple search selection dropdown" id="docTarget">
                        <input type="hidden" name="docTarget" value="${params.docTarget}">
                        <i class="dropdown icon"></i>
                        <input type="text" class="search">
                        <div class="default text"></div>
                    </div>
                </div>

        </div>
        --%>
        <div class="field la-field-right-aligned">
            <a href="${request.forwardURI}" class="ui reset primary primary button">${message(code:'default.button.reset.label')}</a>

            <input type="submit" class="ui secondary button" value="${message(code:'default.button.filter.label')}" />
        </div>
    </g:form>
</semui:filter>
<r:script>
    $(document).ready(function(){
        var org = false, license = false, subscription = false, pkg = false;

        setupDropdown();
        function setupDropdown() {
            $("#docTarget").dropdown({
               apiSettings: {
                   url: "<g:createLink controller="ajax" action="lookupCombined"/>?query={query}&org="+org+"&license="+license+"&subscription="+subscription+"&package="+pkg,
                   cache: false
               },
               clearable: true,
               minCharacters: 0
            });
        }

        $(".targetList").checkbox({
            onChecked: function() {
                $("#docTarget").dropdown('destroy');
                switch($(this).attr("id")){
                    case "org": org = true;
                    break;
                    case "license": license = true;
                    break;
                    case "subscription": subscription = true;
                    break;
                    case "pkg": pkg = true;
                    break;
                }
                setupDropdown();
            },
            onUnchecked: function() {
                $("#docTarget").dropdown('destroy');
                switch($(this).attr("id")){
                    case "org": org = false;
                    break;
                    case "license": license = false;
                    break;
                    case "subscription": subscription = false;
                    break;
                    case "pkg": pkg = false;
                    break;
                }
                setupDropdown();
            }
        });
    });
</r:script>