package com.k_int.kbplus.api.v0.in

import com.k_int.kbplus.*
import com.k_int.kbplus.api.v0.MainService
import groovy.util.logging.Log4j
import org.codehaus.groovy.grails.web.json.JSONObject
import org.springframework.transaction.TransactionStatus

@Log4j
class InService {

    InHelperService inHelperService

    /**
     *
     * @param JSONObject data
     * @param com.k_int.kbplus.Org context
     * @return
     */
    def importLicense(JSONObject data, Org context) {

        def license
        License.withTransaction { TransactionStatus status ->

            try {
                license = new License(
                        contact:            data.contact,
                        licenseUrl:         data.licenseUrl,
                        licensorRef:        data.licensorRef,
                        licenseeRef:        data.licenseeRef,
                        licenseType:        data.licenseType,
                        licenseStatus:      data.licenseStatus,
                        noticePeriod:       data.noticePeriod,
                        reference:          data.reference,
                        sortableReference:  data.sortableReference,
                )

                license.startDate   = inHelperService.getValidDateFormat(data.startDate)
                license.endDate     = inHelperService.getValidDateFormat(data.endDate)
                // todo: license.lastmod     = data.lastmod // long ????

                // RefdataValues
                license.isPublic         = inHelperService.getRefdataValue(data.isPublic, "YN")
                license.licenseCategory  = inHelperService.getRefdataValue(data.licenseCategory, "LicenseCategory")
                license.status           = inHelperService.getRefdataValue(data.status, "License Status")
                license.type             = inHelperService.getRefdataValue(data.type, "License Type")

                // References
                def properties           = inHelperService.getProperties(data.properties, license, context)
                license.customProperties = properties['custom']

                // not supported: license.documents
                // not supported: license.onixplLicense

                //def organisations = inHelperService.getOrgLinks(data.organisations, license)
                //license.orgLinks = organisations

                //TODO
                //def subscriptions = inHelperService.getSubscriptions(data.subscriptions)
                //license.orgLinks = organisations


                license.save(flush: true)
            }
            catch (Exception e) {
                log.error("Error while importing LICENSE via API; rollback forced")
                log.error(e)
                status.setRollbackOnly()
                return ['result': MainService.INTERNAL_SERVER_ERROR, 'debug': e]
            }
        }

        return ['result': MainService.CREATED, 'debug': license]
    }

    /**
     *
     * @param JSONObject data
     * @param com.k_int.kbplus.Org context
     * @return
     */
    def importOrganisation(JSONObject data, Org context) {

        def org
        Org.withTransaction { TransactionStatus status ->

            try {
                org = new Org(
                        name: data.name,
                        comment: data.comment,
                        scope: data.scope
                )

                // RefdataValues
                org.sector  = inHelperService.getRefdataValue(data.sector, "OrgSector")
                org.status  = inHelperService.getRefdataValue(data.status, "OrgStatus") // TODO unknown catagory !!!
                org.orgType = inHelperService.getRefdataValue(data.type, "OrgType")

                // References
                org.addresses = inHelperService.getAddresses(data.addresses, org, null)
                org.contacts  = inHelperService.getContacts(data.contacts, org, null)
                org.ids       = inHelperService.getIdentifiers(data.identifiers, org) // implicit creation of identifier and namespace

                def properties        = inHelperService.getProperties(data.properties, org, context)
                org.customProperties  = properties['custom']
                org.privateProperties = properties['private']

                def personsAndRoles = inHelperService.getPersonsAndRoles(data.persons, org, context)
                org.prsLinks        = personsAndRoles['personRoles']

                personsAndRoles['persons'].each { p ->
                    (Person) p.save() // save persons before saving prsLinks
                }

                org.save(flush: true)
            }
            catch (Exception e) {
                log.error("Error while importing ORG via API; rollback forced")
                log.error(e)
                status.setRollbackOnly()
                return ['result': MainService.INTERNAL_SERVER_ERROR, 'debug': e]
            }
        }

        return ['result': MainService.CREATED, 'debug': org]
    }

    /**
     *
     * @param JSONObject data
     * @param com.k_int.kbplus.Org context
     * @return
     */
    def importSubscription(JSONObject data, Org context) {

    }
}
