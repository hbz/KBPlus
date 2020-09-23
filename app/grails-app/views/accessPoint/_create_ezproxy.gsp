<semui:form>
    <g:form action="create_${accessMethod}" controller="accessPoint" id="${orgInstance.id}" method="post" class="ui form">

        <g:render template="name" model="${[nameOptions: [], name: '']}"/>
        <div class="field required">
            <label>URL</label>
            <g:textField name="url" value="${url}" />
        </div>
        <input type="submit" class="ui button js-click-control" value="${message(code: 'default.button.create.label')}"/>
    </g:form>
</semui:form>

