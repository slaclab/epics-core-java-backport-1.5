/**
 * Copyright information and license terms for this software can be
 * found in the file LICENSE.TXT included with the distribution.
 */
package org.epics.vtype;

import org.epics.util.array.ListInteger;
import org.epics.util.array.ListUInteger;

/**
 * Immutable {@code VUIntArray} implementation.
 *
 * @author carcassi
 */
final class IVUIntArray extends VUIntArray {
    
    private final ListUInteger data;
    private final ListInteger sizes;
    private final Alarm alarm;
    private final Time time;
    private final Display display;

    IVUIntArray(ListUInteger data, ListInteger sizes, Alarm alarm, Time time, Display display) {
        VType.argumentNotNull("data", data);
        VType.argumentNotNull("sizes", sizes);
        VType.argumentNotNull("alarm", alarm);
        VType.argumentNotNull("time", time);
        VType.argumentNotNull("display", display);
        this.data = data;
        this.alarm = alarm;
        this.time = time;
        this.display = display;
        this.sizes = sizes;
    }

    @Override
    public ListInteger getSizes() {
        return sizes;
    }

    @Override
    public ListUInteger getData() {
        return data;
    }

    @Override
    public Alarm getAlarm() {
        return alarm;
    }

    @Override
    public Time getTime() {
        return time;
    }

    @Override
    public Display getDisplay() {
        return display;
    }

}
