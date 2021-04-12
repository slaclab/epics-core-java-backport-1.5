package org.epics.pvaccess.server.test.helpers;

import org.epics.pvdata.pv.*;

public class PVRequestUtils {

    public static boolean getProcess(PVStructure pvRequest) {
        PVField pvField = pvRequest.getSubField("record._options.process");
        if (pvField == null || pvField.getField().getType() != Type.scalar) return false;
        Scalar scalar = (Scalar) pvField.getField();
        if (scalar.getScalarType() == ScalarType.pvString) {
            PVString pvString = (PVString) pvField;
            return pvString.get().equalsIgnoreCase("true");
        } else if (scalar.getScalarType() == ScalarType.pvBoolean) {
            PVBoolean pvBoolean = (PVBoolean) pvField;
            return pvBoolean.get();
        }
        return false;
    }

}
