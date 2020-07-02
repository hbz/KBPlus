<%@ page import="com.k_int.kbplus.RefdataCategory;com.k_int.kbplus.RefdataValue;de.laser.helper.RDConstants;com.k_int.kbplus.Org;de.laser.I10nTranslation; java.text.SimpleDateFormat;" %>
<laser:serviceInjection />
<%
    SimpleDateFormat sdf = de.laser.helper.DateUtil.getSDF_NoTime()
    Date startOfYear = new SimpleDateFormat('yyyy-MM-dd').parse(Calendar.getInstance().get(Calendar.YEAR)+'-01-01')
%>
<semui:modal id="${formId ?: 'create_number'}" text="${message(code: 'readerNumber.create.label')}" isEditModal="${formId ?: null}">

    <g:form class="ui form create_number" url="[controller: 'readerNumber', action: formId ? 'edit' : 'create', id: numbersInstance ? numbersInstance.id : null]" method="POST">
    <g:hiddenField name="orgid" value="${params.id}"/>

        <div class="field">
            <div class="two fields">
                <div class="field three wide">
                    <semui:datepicker label="readerNumber.dueDate.label" id="dueDate" name="dueDate"
                                      placeholder="default.date.label" value="${numbersInstance?.dueDate ?: sdf.format(startOfYear)}" required=""
                                      bean="${numbersInstance}"/>
                </div>

                <div class="field thirteen wide">
                    <label for="referenceGroup">
                        <g:message code="readerNumber.referenceGroup.label" />
                    </label>
                    <%
                        List refdatasWithI10n = RefdataCategory.getAllRefdataValuesWithI10nExplanation(RDConstants.NUMBER_TYPE, [sort:'order',order:'asc'])
                    %>
                    <semui:dropdownWithI18nExplanations name="referenceGroup" class="referenceGroup search"
                                                        from="${refdatasWithI10n}"
                                                        optionKey="id" optionValue="value" optionExpl="expl" noSelection="${message(code:'default.select.choose.label')}"
                                                        value="${numbersInstance?.referenceGroup}"
                    />

                </div>

            </div>
        </div>

        <div class="field">
            <div class="two fields">
                <div class="field eight wide">
                    <div class="field fieldcontain">
                        <label for="semester"><g:message code="readerNumber.semester.label"/></label>
                        <laser:select class="ui selection dropdown la-full-width" label="readerNumber.semester.label" id="semester" name="semester"
                                      from="${RefdataCategory.getAllRefdataValues(RDConstants.SEMESTER)}"
                                      optionKey="id" optionValue="value"
                                      value="${numbersInstance?.semester?.id}"/>
                    </div>
                </div>

                <div class="field eight wide required">
                    <label for="value">
                        <g:message code="readerNumber.number.label"/>
                    </label>
                    <input type="number" id="value" name="value" value="${numbersInstance?.value}"/>

                </div>

            </div>
        </div>

    </g:form>

    <r:script>
        $(document).ready(function() {
            $(".referenceGroup").dropdown({
                allowAdditions: true,
                clearable: true
            });

            $('.create_number').form({
                on: 'blur',
                inline: true,
                fields: {
                    value: {
                        identifier  : 'value',
                        rules: [
                            {
                                type   : 'empty',
                                prompt : '{name} <g:message code="validation.needsToBeFilledOut" />'
                            }
                        ]
                    },
                    dueDate: {
                        identifier : 'dueDate',
                        rules: [
                            {
                                type : 'regExp',
                                value: /\d{2}\.\d{2}\.\d{4}/,
                                prompt: '<g:message code="validation.validDate"/>'
                            }
                        ]
                    }
                 }
            });
        });

    </r:script>

</semui:modal>