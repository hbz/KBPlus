package com.k_int.kbplus

import com.k_int.properties.PropertyDefinition
import grails.converters.*
import grails.plugin.cache.Cacheable
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.annotation.Secured
import grails.converters.*
import org.elasticsearch.groovy.common.xcontent.*
import groovy.xml.MarkupBuilder
import com.k_int.kbplus.auth.*
import com.k_int.kbplus.Org

@Secured(['IS_AUTHENTICATED_FULLY'])
class ProfileController {

    def springSecurityService
    def passwordEncoder
    def errorReportService
    def refdataService
    def propertyService

    @Secured(['ROLE_USER'])
    def index() {
        def result = [:]
        result.user = User.get(springSecurityService.principal.id)
        result.editable = true
        result
    }

    @Secured(['ROLE_USER'])
    def errorReport() {
        def result = [:]
        result.user = User.get(springSecurityService.principal.id)

        if (params.sendErrorReport) {
            def data = [
                    author:     result.user,
                    title:      params.title?.trim(),
                    // TODO: workaround: erms-534 .. dbm required
                    described:  ( params.described?.trim() ) ?: '..',
                    expected:   ( params.expected?.trim() ) ?: '..',
                    info:       ( params.info?.trim() ) ?: '..',
                    status:     RefdataValue.getByValueAndCategory('New', 'Ticket.Status'),
                    category:   RefdataValue.getByValueAndCategory('Bug', 'Ticket.Category')
            ]
            result.sendingStatus = (errorReportService.writeReportIntoDB(data) ? 'ok' : 'fail')
        }

        result.title = params.title
        result.described = params.described
        result.expected = params.expected
        result.info = params.info

        result
    }

    @Secured(['ROLE_USER'])
    def errorOverview() {
        def result = [:]
        result.user = User.get(springSecurityService.principal.id)

        result.tickets = SystemTicket.where{}.list(sort: 'dateCreated', order: 'desc')

        result.editable = SpringSecurityUtils.ifAnyGranted("ROLE_YODA,ROLE_TICKET_EDITOR")
        result
    }

    @Secured(['ROLE_USER'])
    def help() {
        def result = [:]
        result.user = User.get(springSecurityService.principal.id)
        result
    }

    @Secured(['ROLE_USER'])
    def processJoinRequest() {
        log.debug("processJoinRequest(${params}) org with id ${params.org} role ${params.formalRole}")

        def user        = User.get(springSecurityService.principal.id)
        def org         = Org.get(params.org)
        def formal_role = Role.get(params.formalRole)

        try {
            if ( (org != null) && (formal_role != null) ) {
                def existingRel = UserOrg.find( { org==org && user==user && formalRole==formal_role } )

                if (existingRel && existingRel.status == UserOrg.STATUS_CANCELLED) {
                    existingRel.delete()
                    existingRel = null
                }

                if(existingRel) {
                    log.debug("existing rel");
                    flash.error= message(code:'profile.processJoinRequest.error', default:"You already have a relation with the requested organisation.")
                }
                else {
                    log.debug("Create new user_org entry....");
                    def p = new UserOrg(dateRequested:System.currentTimeMillis(),
                                      status:UserOrg.STATUS_PENDING,
                                      org:org,
                                      user:user,
                                      formalRole:formal_role)
                    p.save(flush:true, failOnError:true)
                }
            }
            else {
                log.error("Unable to locate org or role");
            }
        }
        catch ( Exception e ) {
            log.error("Problem requesting affiliation",e);
        }

        redirect(action: "index")
    }

    @Secured(['ROLE_USER'])
    def processCancelRequest() {
        log.debug("processCancelRequest(${params}) userOrg with id ${params.assoc}")
        def user        = User.get(springSecurityService.principal.id)
        def userOrg     = UserOrg.findByUserAndId(user, params.assoc)

        if (userOrg) {
            userOrg.status = UserOrg.STATUS_CANCELLED
        }

        redirect(action: "index")
    }

  @Secured(['ROLE_USER'])
  def updateProfile() {
    def user = User.get(springSecurityService.principal.id)

    flash.message=""

    if ( user.display != params.userDispName ) {
      user.display = params.userDispName
      flash.message += message(code:'profile.updateProfile.updated.name', default:"User display name updated<br/>")
    }

    if ( user.email != params.email ) {
      def mailPattern = /[_A-Za-z0-9-]+(\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)*(\.[A-Za-z]{2,})/
      if ( params.email ==~ mailPattern ) {
        user.email = params.email
        flash.message += message(code:'profile.updateProfile.updated.email', default:"User email address updated<br/>")
      }
      else {
        flash.error = message(code:'profile.updateProfile.updated.email.error', default:"Emails must be of the form user@domain.name<br/>")
      }
    }


    // deprecated
    if ( params.defaultPageSize != null ) {
      try {
        long l = Long.parseLong(params.defaultPageSize);
        if ( ( l >= 5 ) && ( l <= 100 ) ) {
          Long new_long = new Long(l);
          if ( new_long != user.defaultPageSize ) {
            flash.message += message(code:'profile.updateProfile.updated.pageSize', default:"User default page size updated<br/>")
          }
          user.defaultPageSize = new_long
     
        }
        else {
          flash.message+= message(code:'profile.updateProfile.updated.pageSize.error', default:"Default page size must be between 5 and 100<br/>");
        }
      }
      catch ( Exception e ) {
      }
    }

    if ( params.defaultDash != user.defaultDash?.id.toString() ) {
      flash.message+= message(code:'profile.updateProfile.updated.dash', default:"User default dashboard updated<br/>")
      if ( params.defaultDash == '' ) {
        user.defaultDash = null
      }
      else {
        user.defaultDash = Org.get(params.defaultDash);
      }
    }

    user.save();


    redirect(action: "index")
  }

    @Secured(['ROLE_USER'])
    def updatePassword() {
        def user = User.get(springSecurityService.principal.id)
        flash.message = ""

        if (passwordEncoder.isPasswordValid(user.password, params.passwordCurrent, null)) {
            if (params.passwordNew.trim().size() < 5) {
                flash.message += message(code:'profile.password.update.enterValidNewPassword', default:"Please enter new password (min. 5 chars)")
            } else {
                user.password = params.passwordNew

                if (user.save()) {
                    flash.message += message(code:'profile.password.update.success', default:"Password succesfully updated")
                }
            }

        } else {
            flash.message += message(code:'profile.password.update.enterValidCurrentPassword', default:"Please enter valid current password")
        }
        redirect(action: "index")
    }

    private def addTransforms() {

    def user = User.get(springSecurityService.principal.id)
    def transforms = Transforms.findById(params.transformId)
    
    if(user && transforms){
      def existing_transform = UserTransforms.findByUserAndTransforms(user,transforms);
      if ( existing_transform == null ) {
        new UserTransforms(
            user: user,
            transforms: transforms).save(failOnError: true)
        flash.message="Transformation added"
      }
      else {
        flash.error="You already have added this transform."
      }
    }else{  
      log.error("Unable to locate transforms");
      flash.error="Error we could not add this transformation"
    }

    redirect(action: "index")
  }


    private def removeTransforms() {
    def user = User.get(springSecurityService.principal.id)
    def transforms = Transforms.findById(params.transformId)
    
    //Check if has already transforms
    if(user && transforms){
      def existing_transform = UserTransforms.findByUserAndTransforms(user,transforms);
      if(existing_transform){
        transform.delete(failOnError: true, flush: true)
        flash.message="Transformation removed from your list."
      }else{
        flash.error="This transformation is not in your list."
      }
    }else{
      log.error("Unable to locate transforms");
      flash.error="Error we could not remove this transformation"
    }
    
    redirect(action: "index")
  }

    @Secured(['ROLE_USER'])
    def createReminder() {
        log.debug("Profile :: createReminder - ${params}")
        def result    = [:]
        def user      = User.load(springSecurityService.principal.id)
        def trigger   = (params.int('trigger'))? RefdataValue.load(params.trigger) : RefdataCategory.lookupOrCreate("ReminderTrigger","Subscription Manual Renewal Date")
        def remMethod = (params.int('method'))?  RefdataValue.load(params.method)  : RefdataCategory.lookupOrCreate("ReminderMethod","email")
        def unit      = (params.int('unit'))?    RefdataValue.load(params.unit)    : RefdataCategory.lookupOrCreate("ReminderUnit","Day")


        def reminder = new Reminder(trigger: trigger, unit: unit, reminderMethod: remMethod, amount: params.getInt('val')?:1, user: user, active: Boolean.TRUE)
        if (reminder.save())
        {
            log.debug("Profile :: Index - Successfully saved reminder, adding to user")
            user.addToReminders(reminder)
            log.debug("User has following reminders ${user.reminders}")
            result.status   = true
            result.reminder = reminder
        } else {
            result.status = false
            flash.error="Unable to create the reminder, invalid data received"
            log.debug("Unable to save Reminder for user ${user.username}... Params as follows ${params}")
        }
        if (request.isXhr())
            render result as JSON
        else
            redirect(action: "index", fragment: "reminders")
    }

    @Secured(['ROLE_USER'])
    def updateReminder() {
        def result    = [:]
        result.status = true
        result.op     = params.op
        def user      = User.get(springSecurityService.principal.id)
        def reminder  = Reminder.findByIdAndUser(params.id,user)
        if (reminder)
        {
            switch (result.op)
            {
                case 'delete':
                    user.reminders.clear()
                    user.reminders.remove(reminder)
                    reminder.delete(flush: true)
                    break
                case 'toggle':
                    reminder.active = !reminder.active
                    result.active   = reminder.active? 'disable':'enable'
                    break
                default:
                    result.status = false
                    log.error("Profile :: updateReminder - Unsupported operation for update reminder ${result.op}")
                    break
            }
        } else
            result.status = false

        render result as JSON
    }

    @Secured(['ROLE_USER'])
    def properties() {
        def propDefs = [:]
        PropertyDefinition.AVAILABLE_CUSTOM_DESCR.each { it ->
            def itResult = PropertyDefinition.findAllByDescrAndTenant(it, null, [sort: 'name']) // NO private properties!
            propDefs << ["${it}": itResult]
        }

        def (usedRdvList, rdvAttrMap) = refdataService.getUsageDetails()
        def (usedPdList,   pdAttrMap) = propertyService.getUsageDetails()

        render view: 'properties', model: [
                editable    : false,
                propertyDefinitions: propDefs,
                rdCategories: RefdataCategory.where{}.sort('desc'),
                usedRdvList : usedRdvList,
                usedPdList  : usedPdList
        ]
    }
}
