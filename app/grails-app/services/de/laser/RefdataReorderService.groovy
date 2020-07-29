package de.laser

import com.k_int.kbplus.RefdataCategory
import com.k_int.kbplus.RefdataValue
import de.laser.helper.RDConstants
import grails.transaction.Transactional

@Transactional
class RefdataReorderService {

    /*
        This bootstrapped method should capsulate every reordering queries so that no manual database migration scripts needs to be executed
     */
    void reorderRefdata() {
        //semesters: take the order of insertion and make then the ID ascending
        List semesters = RefdataValue.findAllByOwner(RefdataCategory.getByDesc(RDConstants.SEMESTER),[sort:'id', order:'asc'])
        //RefdataValue.executeUpdate('update RefdataValue rdv set rdv.order = 0 where rdv.value = :value',[value:'semester.not.applicable'])
        int order = 10
        semesters.each { s ->
            s.order = order
            s.save()
            order += 10
        }
        //number types: defined by external
        RefdataValue.executeUpdate('update RefdataValue rdv set rdv.order = 0 where rdv.value = :value',[value:'Students'])
        RefdataValue.executeUpdate('update RefdataValue rdv set rdv.order = 10 where rdv.value = :value',[value:'Scientific staff'])
        RefdataValue.executeUpdate('update RefdataValue rdv set rdv.order = 20 where rdv.value = :value',[value:'User'])
        RefdataValue.executeUpdate('update RefdataValue rdv set rdv.order = 30 where rdv.value = :value',[value:'Population'])
        //currencies: defined by external
        List currencies = RefdataValue.findAllByOwnerAndValueNotEqual(RefdataCategory.getByDesc('Currency'),'EUR',[sort:'value',order:'asc'])
        RefdataValue.executeUpdate('update RefdataValue rdv set rdv.order = 0 where rdv.value = :value',[value:'EUR'])
        order = 10
        currencies.each { c ->
            c.order = order
            c.save()
            order += 10
        }
    }
}
