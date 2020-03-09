package com.k_int.kbplus.abstract_domain


import com.k_int.kbplus.RefdataValue
import de.laser.helper.DateUtil

import javax.persistence.Transient

abstract class AbstractProperty implements Serializable {

    String           stringValue
    Integer          intValue
    BigDecimal       decValue
    RefdataValue     refValue
    URL              urlValue
    String           note = ""
    Date             dateValue

    static mapping = {
        stringValue  type: 'text'
        note         type: 'text'
    }

    static constraints = {
        stringValue (nullable: true)
        intValue    (nullable: true)
        decValue    (nullable: true)
        refValue    (nullable: true)
        urlValue    (nullable: true)
        note        (nullable: true)
        dateValue   (nullable: true)
    }

    @Transient
    def getValueType(){
        if(stringValue)
            return "stringValue"
        if(intValue)
            return "intValue"
        if(decValue)
            return "decValue"
        if(refValue)
            return "refValue"
        if(dateValue)
            return "dateValue"
        if(urlValue)
            return "urlValue"
    }

    String getValue() {
        return toString()
    }

    @Override
    String toString(){
        if(stringValue)
            return stringValue
        if(intValue != null)
            return intValue.toString()
        if(decValue != null)
            return decValue.toString()
        if(refValue)
            return refValue.toString()
        if(dateValue)
            return dateValue.getDateString()
        if(urlValue)
            return urlValue.toString()
    }

    def copyInto(AbstractProperty newProp){
        if (type != newProp.type) {
            throw new IllegalArgumentException("AbstractProperty.copyInto nicht möglich, weil die Typen nicht übereinstimmen.")
        } else {
            newProp.stringValue = stringValue
            newProp.intValue = intValue
            newProp.decValue = decValue
            newProp.refValue = refValue
            newProp.dateValue = dateValue
            newProp.urlValue = urlValue
            newProp.note = note
        }
        newProp
    }

    def static parseValue(value, type){
        def result
        
        switch (type){
            case Integer.toString():
                result = Integer.parseInt(value)
                break
            case String.toString():
                result = value
                break
            case BigDecimal.toString():
                result = new BigDecimal(value)
                break
            case org.codehaus.groovy.runtime.NullObject.toString():
                result = null
                break
            case Date.toString():
                result = DateUtil.toDate_NoTime(value)
                break
            case URL.toString():
                result = new URL(value)
                break
            default:
                result = "AbstractProperty.parseValue failed"
        }
        return result
    }

    def setValue(value, type, rdc) {

        if (type == Integer.toString()) {
            intValue = parseValue(value, type)
        }
        else if (type == BigDecimal.toString()) {
            decValue = parseValue(value, type)
        }
        else if (type == String.toString()) {
            stringValue = parseValue(value, type)
        }
        else if (type == Date.toString()) {
            dateValue = parseValue(value, type)
        }
        else if (type == RefdataValue.toString()) {
            refValue = RefdataValue.getByValueAndCategory(value.toString(), rdc)
        }
        else if (type == URL.toString()) {
            urlValue = parseValue(value, type)
        }
    }
}
