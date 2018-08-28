package de.laser.api.v1.entities

import com.k_int.kbplus.Identifier
import com.k_int.kbplus.Org
import com.k_int.kbplus.Subscription
import com.k_int.kbplus.auth.User
import de.laser.api.v1.ApiReader
import de.laser.domain.Constants
import grails.converters.JSON
import groovy.util.logging.Log4j

@Log4j
class ApiSubscription {

    /**
     * @return Subscription | BAD_REQUEST | PRECONDITION_FAILED
     */
    static findSubscriptionBy(String query, String value) {
        def result

        switch(query) {
            case 'id':
                result = Subscription.findAllWhere(id: Long.parseLong(value))
                break
            case 'globalUID':
                result = Subscription.findAllWhere(globalUID: value)
                break
            case 'identifier':
                result = Subscription.findAllWhere(identifier: value)
                break
            case 'impId':
                result = Subscription.findAllWhere(impId: value)
                break
            case 'ns:identifier':
                result = Identifier.lookupObjectsByIdentifierString(new Subscription(), value)
                break
            default:
                return Constants.HTTP_BAD_REQUEST
                break
        }
        if (result) {
            result = result.size() == 1 ? result.get(0) : Constants.HTTP_PRECONDITION_FAILED
        }
        result
    }

    /**
     * @return grails.converters.JSON | FORBIDDEN
     */
    static getSubscription(Subscription sub, User user, Org context){
        def result = []
        def hasAccess = ApiReader.isDataManager(user)

        if (! hasAccess) {
            sub.orgRelations.each { orgRole ->
                if (orgRole.getOrg().id == context?.id) {
                    hasAccess = true
                }
            }
        }

        if (hasAccess) {
            result = ApiReader.exportSubscription(sub, context) // TODO check orgRole.roleType
        }

        return (hasAccess ? new JSON(result) : Constants.HTTP_FORBIDDEN)
    }
}
