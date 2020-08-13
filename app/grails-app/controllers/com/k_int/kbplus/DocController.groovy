package com.k_int.kbplus

import com.k_int.kbplus.auth.User
import de.laser.controller.AbstractDebugController
import de.laser.helper.DebugAnnotation
import grails.plugin.springsecurity.annotation.Secured
import org.springframework.dao.DataIntegrityViolationException

@Secured(['IS_AUTHENTICATED_FULLY'])
class DocController extends AbstractDebugController {

	def springSecurityService
	def contextService

    static allowedMethods = [create: ['GET', 'POST'], edit: ['GET', 'POST'], delete: 'POST']

	@Secured(['ROLE_ADMIN'])
    def index() {
        redirect action: 'list', params: params
    }

	@Secured(['ROLE_ADMIN'])
    def list() {
      	Map<String, Object> result = [:]
      	result.user = User.get(springSecurityService.principal.id)

		params.max = params.max ?: result.user?.getDefaultPageSize()

      	result.docInstanceList = Doc.list(params)
      	result.docInstanceTotal = Doc.count()
      	result
    }

	@DebugAnnotation(test='hasAffiliation("INST_EDITOR")')
	@Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_EDITOR") })
    def create() {
		params.org = contextService.org
		switch (request.method) {
			case 'GET':
				[docInstance: new Doc(params)]
				break
			case 'POST':
				Doc docInstance = new Doc(params)
				if (!docInstance.save(flush: true)) {
					render view: 'create', model: [docInstance: docInstance]
					return
				}

				flash.message = message(code: 'default.created.message', args: [message(code: 'doc.label'), docInstance.id])
				redirect action: 'show', id: docInstance.id
			break
		}
    }

    @DebugAnnotation(test = 'hasAffiliation("INST_USER")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_USER") })
    def show() {
		Doc docInstance = Doc.get(params.id)
        if (!docInstance) {
			flash.message = message(code: 'default.not.found.message', args: [message(code: 'doc.label'), params.id])
            redirect action: 'list'
            return
        }

        [docInstance: docInstance]
    }

	@DebugAnnotation(test='hasAffiliation("INST_EDITOR")')
	@Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_EDITOR") })
    def edit() {
		switch (request.method) {
		case 'GET':
			Doc docInstance = Doc.get(params.id)
	        if (!docInstance) {
	            flash.message = message(code: 'default.not.found.message', args: [message(code: 'doc.label'), params.id])
	            redirect action: 'list'
	            return
	        }
			docInstance.owner = contextService.org
	        [docInstance: docInstance]
			break
		case 'POST':
				Doc docInstance = Doc.get(params.id)
	        if (!docInstance) {
	            flash.message = message(code: 'default.not.found.message', args: [message(code: 'doc.label'), params.id])
	            redirect action: 'list'
	            return
	        }

	        if (params.version) {
	            def version = params.version.toLong()
	            if (docInstance.version > version) {
	                docInstance.errors.rejectValue('version', 'default.optimistic.locking.failure',
	                          [message(code: 'doc.label')] as Object[],
	                          "Another user has updated this Doc while you were editing")
	                render view: 'edit', model: [docInstance: docInstance]
	                return
	            }
	        }

	        docInstance.properties = params
			if(!docInstance.owner)
				docInstance.owner = contextService.org

	        if (!docInstance.save(flush: true)) {
	            render view: 'edit', model: [docInstance: docInstance]
	            return
	        }

			flash.message = message(code: 'default.updated.message', args: [message(code: 'doc.label'), docInstance.id])
	        redirect action: 'show', id: docInstance.id
			break
		}
    }
	@DebugAnnotation(test='hasAffiliation("INST_EDITOR")')
	@Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_EDITOR") })
	def editNote() {
		switch (request.method) {
		/*case 'GET':
	        def docInstance = Doc.get(params.id)
	        if (!docInstance) {
	            flash.message = message(code: 'default.not.found.message', args: [message(code: 'doc.label'), params.id])
	            redirect action: 'list'
	            return
	        }

	        [docInstance: docInstance]
			break*/
			case 'POST':
				Doc docInstance = Doc.get(params.id)
				if (!docInstance) {
					flash.message = message(code: 'default.not.found.message', args: [message(code: 'default.note.label'), params.id])
					//redirect action: 'list'
					redirect(url: request.getHeader('referer'))
					return
				}

				if (params.version) {
					def version = params.version.toLong()
					if (docInstance.version > version) {
						docInstance.errors.rejectValue('version', 'default.optimistic.locking.failure',
								[message(code: 'default.note.label')] as Object[],
								"Another user has updated this Doc while you were editing")
						//render view: 'edit', model: [docInstance: docInstance]
						redirect(url: request.getHeader('referer'))
						return
					}
				}

				docInstance.properties = params
				if(!docInstance.owner)
					docInstance.owner = contextService.org

				if (!docInstance.save(flush: true)) {
					//render view: 'edit', model: [docInstance: docInstance]
					redirect(url: request.getHeader('referer'))
					return
				}

				flash.message = message(code: 'default.updated.message', args: [message(code: 'default.note.label'), docInstance.title])
				//redirect action: 'show', id: docInstance.id
				redirect(url: request.getHeader('referer'))
				break
		}
	}

	@DebugAnnotation(test='hasAffiliation("INST_EDITOR")')
	@Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_EDITOR") })
    def delete() {
		Doc docInstance = Doc.get(params.id)
        if (!docInstance) {
			flash.message = message(code: 'default.not.found.message', args: [message(code: 'doc.label'), params.id])
            redirect action: 'list'
            return
        }

        try {
            docInstance.delete(flush: true)
			flash.message = message(code: 'default.deleted.message', args: [message(code: 'doc.label'), params.id])
            redirect action: 'list'
        }
        catch (DataIntegrityViolationException e) {
			flash.message = message(code: 'default.not.deleted.message', args: [message(code: 'doc.label'), params.id])
            redirect action: 'show', id: params.id
        }
    }
}
