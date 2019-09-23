package de.laser.domain

import com.k_int.kbplus.IssueEntitlement
import com.k_int.kbplus.RefdataValue
import de.laser.helper.RefdataAnnotation
import de.laser.domain.AbstractBaseDomain

class PriceItem extends AbstractBaseDomain {

    @RefdataAnnotation(cat = 'Currency')
    RefdataValue listCurrency

    @RefdataAnnotation(cat = 'Currency')
    RefdataValue localCurrency

    BigDecimal listPrice
    BigDecimal localPrice

    Date priceDate

    static belongsTo = [issueEntitlement: IssueEntitlement]

    static mapping = {
        id                  column: 'pi_id'
        globalUID           column: 'pi_guid'
        listPrice           column: 'pi_list_price'
        listCurrency        column: 'pi_list_currency_rv_fk'
        localPrice          column: 'pi_local_price'
        localCurrency       column: 'pi_local_currency_rv_fk'
        priceDate           column: 'pi_price_date'
        issueEntitlement    column: 'pi_ie_fk'
    }

    static constraints = {
        globalUID           (nullable: false, blank: false, unique: true, maxSize: 255)
        listPrice           (nullable: true, blank: false)
        listCurrency        (nullable: true, blank: false)
        localPrice          (nullable: true, blank: false)
        localCurrency       (nullable: true, blank: false)
        priceDate           (nullable: true, blank: false)
        issueEntitlement    (nullable: false, blank: false)
    }

}
