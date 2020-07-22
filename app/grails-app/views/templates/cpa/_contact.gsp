<g:if test="${contact}">
	<div class="ui js-copyTriggerParent item contact-details">
        <div style="display: flex">
            <semui:contactIcon type="${contact?.contentType.('value')}" />

            <div class="content la-space-right">
                <semui:xEditable class="js-copyTopic" owner="${contact}" field="content" overwriteEditable="${overwriteEditable}" />
            </div>
        </div>


        <div class="content">
            <g:if test="${['Mail', 'E-Mail'].contains(contact?.contentType?.value)}">
                <span class="la-popup-tooltip la-delay" data-position="top right" data-content="Mail senden an ..">
                    <a href="mailto:${contact?.content}" class="ui mini icon blue button">
                        <i class="share square icon"></i>
                    </a>
                </span>
            </g:if>

            <g:if test="${editable && tmplShowDeleteButton}">

                <g:if test="${contact.contentType?.getI10n('value') == 'Url'}">
                    <span class="la-popup-tooltip la-delay" data-position="top right" data-content="Diese URL aufrufen ..">
                        <a href="${contact?.content}" target="_blank" class="ui mini icon blue button">
                            <i class="share square icon"></i>
                        </a>
                    </span>
                </g:if>

                <g:set var="oid" value="${contact.class.name}:${contact.id}" />

				<g:link class="ui mini icon negative button js-open-confirm-modal"
                        data-confirm-tokenMsg="${message(code: "confirm.dialog.delete.contactItems.addressbook")}"
                        data-confirm-term-how="delete"
                        controller="ajax" action="delete" params="[cmd: 'deleteContact', oid: oid]">
					<i class="trash alternate icon"></i>
				</g:link>

            </g:if>
        </div><!-- .content -->

	</div>			
</g:if>
${contactInstance?.contentType}