package com.k_int.kbplus

import com.k_int.kbplus.auth.User
import de.laser.controller.AbstractDebugController
import de.laser.helper.RDConstants
import grails.plugin.springsecurity.annotation.Secured

@Secured(['IS_AUTHENTICATED_FULLY'])
class PendingChangeController extends AbstractDebugController {

    def genericOIDService
    def pendingChangeService
    def executorWrapperService
    def contextService
    def springSecurityService

    @Secured(['ROLE_USER'])
    def accept() {
        log.debug("Accept");
        pendingChangeService.performAccept(PendingChange.get(params.long('id')), User.get(springSecurityService.principal.id))
        redirect(url: request.getHeader('referer'))
    }

    @Secured(['ROLE_USER'])
    def reject() {
        log.debug("Reject")
        pendingChangeService.performReject(params.id, User.get(springSecurityService.principal.id))
        redirect(url: request.getHeader('referer'))
    }

    @Secured(['ROLE_USER'])
    def acceptAll() {
        log.debug("acceptAll - ${params}")
        def owner = genericOIDService.resolveOID(params.OID)

        def changes_to_accept = []
        def pending_change_pending_status = RefdataValue.getByValueAndCategory("Pending", RDConstants.PENDING_CHANGE_STATUS)
        List<PendingChange> pendingChanges = owner?.pendingChanges.findAll {
            (it.status == pending_change_pending_status) || it.status == null
        }
        User user = User.get(springSecurityService.principal.id)
        executorWrapperService.processClosure({
            pendingChanges.each { pc ->
                pendingChangeService.performAccept(pc, user)
            }
        }, owner)

        redirect(url: request.getHeader('referer'))
    }

    @Secured(['ROLE_USER'])
    def rejectAll() {
        log.debug("rejectAll ${params}")
        def owner = genericOIDService.resolveOID(params.OID)

        def changes_to_reject = []
        def pending_change_pending_status = RefdataValue.getByValueAndCategory("Pending", RDConstants.PENDING_CHANGE_STATUS)
        def pendingChanges = owner?.pendingChanges.findAll {
            (it.status == pending_change_pending_status) || it.status == null
        }
        pendingChanges = pendingChanges.collect { it.id }

        //def user = [user: request.user]
        User user = User.get(springSecurityService.principal.id)
        executorWrapperService.processClosure({
            pendingChanges.each { pc ->
                pendingChangeService.performReject(pc, user)
            }
        }, owner)

        redirect(url: request.getHeader('referer'))
    }
}
