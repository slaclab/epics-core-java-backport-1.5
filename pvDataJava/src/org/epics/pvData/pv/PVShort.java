/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvData.pv;

/**
 * Get/put short data.
 * @author mrk
 *
 */
public interface PVShort extends PVScalar{
    /**
     * Get the <i>short</i> value stored in the field.
     * @return short value of field.
     */
    short get();
    /**
     * Put the <i>short</i> value into the field.
     * @param value New value.
     * @throws IllegalStateException if the field is not mutable.
     */
    void put(short value);
}
