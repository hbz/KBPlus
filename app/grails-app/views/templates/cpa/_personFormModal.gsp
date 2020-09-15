<%@ page import="com.k_int.kbplus.PersonRole; de.laser.Contact; de.laser.Person; de.laser.FormService; de.laser.helper.RDStore; com.k_int.kbplus.RefdataValue;com.k_int.kbplus.RefdataCategory;de.laser.helper.RDConstants" %>
<laser:serviceInjection />
<semui:modal id="personEditModal" text="${modalText ?: message(code: 'person.create_new.label')}" msgClose="${message(code: 'default.button.cancel')}" msgSave="${message(code: 'default.button.save.label')}">
    <g:form  id="create_person" class="ui form" url="${url}" method="POST">
        <div class="field">
            <div class="two fields">

                <div class="field wide twelve ${hasErrors(bean: personInstance, field: 'last_name', 'error')} required">
                    <label for="last_name">
                        <g:message code="person.last_name.label" />
                    </label>
                    <g:textField name="last_name" required="" value="${personInstance?.last_name}"/>
                </div>

                <div id="person_title"
                     class="field wide four ${hasErrors(bean: personInstance, field: 'title', 'error')}">
                    <label for="title">
                        <g:message code="person.title.label" />
                    </label>
                    <g:textField name="title" required="required" value="${personInstance?.title}"/>
                </div>

            </div>
        </div>

        <div class="field">
            <div class="three fields">

                <div id="person_first_name"
                     class="field wide eight ${hasErrors(bean: personInstance, field: 'first_name', 'error')}">
                    <label for="first_name">
                        <g:message code="person.first_name.label" />
                    </label>
                    <g:textField name="first_name"  value="${personInstance?.first_name}"/>
                </div>

                <div id="person_middle_name"
                     class="field wide four ${hasErrors(bean: personInstance, field: 'middle_name', 'error')} ">
                    <label for="middle_name">
                        <g:message code="person.middle_name.label" />
                    </label>
                    <g:textField name="middle_name" value="${personInstance?.middle_name}"/>
                </div>

                <div id="person_gender"
                     class="field wide four ${hasErrors(bean: personInstance, field: 'gender', 'error')} ">
                    <label for="gender">
                        <g:message code="person.gender.label" />
                    </label>
                    <laser:select class="ui dropdown" id="gender" name="gender"
                                  from="${Person.getAllRefdataValues(RDConstants.GENDER)}"
                                  optionKey="id"
                                  optionValue="value"
                                  value="${personInstance?.gender?.id}"
                                  noSelection="${['': message(code: 'default.select.choose.label')]}"/>
                </div>
            </div>
        </div>


        <g:if test="${contextService.getOrg()}">
            <input type="hidden" name="tenant.id" value="${contextService.getOrg().id}"/>
            <input id="isPublic" name="isPublic" type="hidden" value="${isPublic}"/>
        </g:if>
        <g:else>
            <div class="field">
                <div class="two fields">

                    <div class="field wide twelve fieldcontain ${hasErrors(bean: personInstance, field: 'tenant', 'error')} required">
                        <label for="tenant">
                            <g:message code="person.tenant.label"
                                       default="Tenant (Permissions to edit this person and depending addresses and contacts)"/>
                        </label>
                        <g:select id="tenant" name="tenant.id" from="${contextService.getMemberships()}" optionKey="id"
                                  value="${contextService.getOrg().id}"/>
                    </div>

                    <div class="field wide four fieldcontain ${hasErrors(bean: personInstance, field: 'isPublic', 'error')} required">
                        <label for="isPublic">
                            <g:message code="person.isPublic.label" />
                        </label>

                        ${isPublic ? RDStore.YN_YES.getI10n('value') : RDStore.YN_NO.getI10n('value')}
                        <input id="isPublic" name="isPublic" required="required" type="hidden" value="${isPublic}"/>
                    </div>
                </div>
            </div>
            <hr/>
        </g:else>

        <g:if test="${!tmplHideContacts}">
            <div class="field">

                <div class="three fields">

                    <div class="field three wide fieldcontain">
                        <label for="contact1_contentType" for="contact2_contentType" for="contact3_contentType">
                            &nbsp;<%--<g:message code="contact.contentType.label" default="ContentType" />--%>
                        </label>

                        <input type="text" readonly value="${RDStore.CCT_EMAIL.getI10n('value')}" />
                        <input type="text" readonly value="${RDStore.CCT_PHONE.getI10n('value')}" />
                        <input type="text" readonly value="${RDStore.CCT_FAX.getI10n('value')}" />

                        <input type="hidden" id="contact1_contentType" name="contact1_contentType"
                               value="${RDStore.CCT_EMAIL.id}" />

                        <input type="hidden" id="contact2_contentType" name="contact2_contentType"
                               value="${RDStore.CCT_PHONE.id}" />

                        <input type="hidden" id="contact3_contentType" name="contact3_contentType"
                               value="${RDStore.CCT_FAX.id}" />
                    </div>

                    <div class="field three fieldcontain">
                        <label for="contact1_type" for="contact2_type" for="contact3_type">
                            ${RefdataCategory.getByDesc(RDConstants.CONTACT_TYPE).getI10n('desc')}
                        </label>
                        <g:set var="allContactTypes" value="${Contact.getAllRefdataValues(RDConstants.CONTACT_TYPE)}" />
                        <laser:select class="ui dropdown" name="contact1_type"
                                      from="${allContactTypes}"
                                      optionKey="id"
                                      optionValue="value" />

                        <laser:select class="ui dropdown" name="contact2_type"
                                      from="${allContactTypes}"
                                      optionKey="id"
                                      optionValue="value" />

                        <laser:select class="ui dropdown" name="contact3_type"
                                      from="${allContactTypes}"
                                      optionKey="id"
                                      optionValue="value" />
                    </div>


                    <div class="field ten wide fieldcontain">
                        <label for="contact1_content" for="contact2_content" for="contact3_content">
                            <g:message code="contact.content.label" />
                        </label>

                        <g:textField name="contact1_content" value=""/>
                        <g:textField name="contact2_content" value=""/>
                        <g:textField name="contact3_content" value=""/>
                    </div>

                </div>

            </div>
        </g:if>


        <div id="person-role-manager">

            <g:if test="${!tmplHideFunctions}">
                <div class="person-role-function-manager">


                    <div class="field">
                        <div class="two fields">
                            <div class="field">
                                <label for="functionType">
                                    <g:message code="person.function.label" />
                                </label>
                                <laser:select class="ui dropdown values"
                                              name="functionType"
                                              from="${PersonRole.getAllRefdataValues(RDConstants.PERSON_FUNCTION)}"
                                              optionKey="id"
                                              value="${presetFunctionType?.id}"
                                              optionValue="value"/>
                            </div>

                            <g:if test="${actionName != 'myPublicContacts' }">
                                <div class="field">
                                    <g:if test="${institution}">
                                        <label for="functionOrg">
                                            <g:message code="contact.belongesTo.label"/>
                                        </label>
                                        <g:select class="ui search dropdown"
                                                  name="functionOrg"
                                                  from="${orgList}"
                                                  value="${org?.id}"
                                                  optionKey="id"
                                                  optionValue=""/>
                                    </g:if>
                                    <g:else>
                                        <label for="functionOrg">
                                            <g:message code="contact.belongesTo.label"/>
                                        </label>
                                        <i class="icon university la-list-icon"></i>${org?.name}
                                        <input id="functionOrg" name="functionOrg" type="hidden" value="${org?.id}"/>
                                    </g:else>
                                </div>
                            </g:if>

                        </div>
                    </div><!-- .field -->


                    <div class="field js-positionTypeWrapper">
                        <div class="two fields">
                            <div class="field">
                                <label for="positionOrg">
                                    <g:message code="person.position.label" />
                                </label>
                                <laser:select class="ui dropdown values"
                                              name="positionType"
                                              from="${PersonRole.getAllRefdataValues(RDConstants.PERSON_POSITION)}"
                                              optionKey="id"
                                              value="${presetPositionType?.id}"
                                              optionValue="value"
                                              noSelection="${['': '']}"/>
                            </div>
                            <g:if test="${actionName != 'myPublicContacts'}">
                                <div class="field">

                                    <g:if test="${institution}">
                                        <label for="positionOrg">
                                            <g:message code="contact.belongesTo.label"/>
                                        </label>
                                        <g:select class="ui search dropdown"
                                                  name="positionOrg"
                                                  from="${orgList}"
                                                  value="${org?.id}"
                                                  optionKey="id"
                                                  optionValue=""/>
                                    </g:if>
                                    <g:else>
                                        <label for="positionOrg">
                                            <g:message code="contact.belongesTo.label"/>
                                        </label>
                                        <i class="icon university la-list-icon"></i>${org?.name}
                                        <input id="positionOrg" name="positionOrg" type="hidden" value="${org?.id}"/>
                                    </g:else>
                                </div>
                            </g:if>

                        </div>
                    </div><!-- .field -->
                </div>
            </g:if>
            <g:else>
            <%-- DEFAULT --%>
                <input type="hidden" name="functionOrg" value="${org?.id}"/>
                <input type="hidden" name="functionType" value="${presetFunctionType?.id}"/><%-- not used ? --%>
            </g:else>
        </div>
        <g:if test="${actionName == 'myPublicContacts'}">
            <input type="hidden" name="functionOrg" value="${org?.id}"/>
        </g:if>

    </g:form>

    <r:script>

        $('#create_person')
                .form({
            on: 'blur',
            inline: true,
            fields: {
                last_name: {
                    identifier  : 'last_name',
                    rules: [
                        {
                            type   : 'empty',
                            prompt : '{name} <g:message code="validation.needsToBeFilledOut"
                                                        default=" muss ausgefüllt werden"/>'
                        }
                    ]
                }
             }
        });
        var fc = "${RDStore.CONTACT_TYPE_FUNCTIONAL.getI10n('value')}";

        $(document).ready( function(){
            changeForm( ($("#${modalId} #contactType option:selected").text() == fc), "${modalId}")
        });

        $("#${modalId} #contactType").on('change', function() {
            changeForm( ($("#${modalId} #contactType option:selected").text() == fc), "${modalId}")
        });

        function changeForm(hide, mId) {
            var group1 = $("#"+mId+" #person_middle_name, #"+mId+" #person_first_name, #"+mId+" #person_title, #"+mId+" #person_gender, #"+mId+" .js-positionTypeWrapper")
            var group2 = $("#"+mId+" #person_gender .dropdown,#"+mId+" #person_gender select")
            var group3 = $("#"+mId+" #positionType,#"+mId+" #positionOrg")
            var group4 = $("#"+mId+" #person_middle_name input, #"+mId+" #person_first_name input,#"+mId+" #person_title input")

            if (hide) {
                group1.hide()
                group2.addClass('disabled')
                group3.attr('disabled', 'disabled')
                group4.attr('disabled', 'disabled')

                $("label[for='last_name']").text("Bezeichnung")
                ! $("#"+mId+"#last_name").val() ? $("#"+mId+"#last_name").val("Allgemeiner Funktionskontakt") : false

            }
            else {
                group1.show()
                group2.removeClass('disabled')
                group3.removeAttr('disabled')
                group4.removeAttr('disabled')

                $("label[for='last_name']").text("Nachname")
                $("#"+mId+"#last_name").val() == "Allgemeiner Funktionskontakt" ? $("#"+mId+"#last_name").val("") : false
            }
        }

        changeForm(false) // init
    </r:script>

</semui:modal>
