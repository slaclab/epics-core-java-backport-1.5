package org.epics.pvaccess.impl.remote;

import org.epics.pvaccess.PVFactory;
import org.epics.pvdata.pv.*;

import java.nio.ByteBuffer;

public class SerializationHelper {

    /**
     * PVField factory.
     */
    private static final PVDataCreate pvDataCreate = PVFactory.getPVDataCreate();

    /**
     * Deserialize PVRequest.
     *
     * @param payloadBuffer data buffer.
     * @param control       deserialization control.
     * @return deserialized PVRequest, can be <code>null</code>.
     */
    public static PVStructure deserializePVRequest(ByteBuffer payloadBuffer, DeserializableControl control) {
        // for now ordinary structure, later can be changed
        return deserializeStructureFull(payloadBuffer, control);
    }

    /**
     * Deserialize Structure and create PVStructure instance, if necessary.
     *
     * @param payloadBuffer     data buffer.
     * @param control           deserialization control.
     * @param existingStructure if deserialized Field matches <code>existingStructure</code> Field, then
     *                          <code>existingStructure</code> instance is returned. <code>null</code> value is allowed.
     * @return PVStructure instance, can be <code>null</code>.
     */
    public static PVStructure deserializeStructureAndCreatePVStructure(ByteBuffer payloadBuffer, DeserializableControl control, PVStructure existingStructure) {
        final Field field = control.cachedDeserialize(payloadBuffer);
        if (field == null)
            return null;
        // reuse existing structure case
        if (existingStructure != null && field.equals(existingStructure.getField()))
            return existingStructure;
        else
            return pvDataCreate.createPVStructure((Structure) field);
    }

    /**
     * Deserialize optional PVStructure.
     *
     * @param payloadBuffer data buffer.
     * @param control       deserialization control.
     * @return deserialized PVStructure, can be <code>null</code>.
     */
    public static PVStructure deserializeStructureFull(ByteBuffer payloadBuffer, DeserializableControl control) {
        return (PVStructure) deserializeFull(payloadBuffer, control);
    }

    /**
     * Deserialize optional PVField.
     *
     * @param payloadBuffer data buffer.
     * @param control       deserialization control.
     * @return deserialized PVField, can be <code>null</code>.
     */
    public static PVField deserializeFull(ByteBuffer payloadBuffer, DeserializableControl control) {
        PVField pvField = null;
        final Field field = control.cachedDeserialize(payloadBuffer);
        if (field != null) {
            pvField = pvDataCreate.createPVField(field);
            pvField.deserialize(payloadBuffer, control);
        }
        return pvField;
    }

    public static void serializeNullField(ByteBuffer buffer, SerializableControl control) {
        control.ensureBuffer(1);
        buffer.put(IntrospectionRegistry.NULL_TYPE_CODE);
    }

    /**
     * Serialize PVRequest.
     *
     * @param buffer    data buffer.
     * @param control   serialization control.
     * @param pvRequest pvRequest to serialize.
     */
    public static void serializePVRequest(ByteBuffer buffer, SerializableControl control, PVStructure pvRequest) {
        // for now ordinary structure, later can be changed
        serializeStructureFull(buffer, control, pvRequest);
    }

    /**
     * Serialize optional PVStructure.
     *
     * @param buffer      data buffer.
     * @param control     serialization control.
     * @param pvStructure PVStructure to fully serialize.
     */
    public static void serializeStructureFull(ByteBuffer buffer, SerializableControl control, PVStructure pvStructure) {
        serializeFull(buffer, control, pvStructure);
    }

    /**
     * Serialize optional PVField.
     *
     * @param buffer  data buffer.
     * @param control serialization control.
     * @param pvField PVField to fully serialize.
     */
    public static void serializeFull(ByteBuffer buffer, SerializableControl control, PVField pvField) {

        if (pvField == null)
            serializeNullField(buffer, control);
        else {
            control.cachedSerialize(pvField.getField(), buffer);
            pvField.serialize(buffer, control);
        }
    }
}
