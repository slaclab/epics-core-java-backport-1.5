package org.epics.pvaccess.client.example;

import org.epics.pvaccess.PVFactory;
import org.epics.pvaccess.impl.remote.IntrospectionRegistry;
import org.epics.pvaccess.util.HexDump;
import org.epics.pvdata.factory.StandardFieldFactory;
import org.epics.pvdata.pv.*;

import java.nio.ByteBuffer;

public class SerializationExamples {

    static class SerDeSerControl implements SerializableControl, DeserializableControl {
        final IntrospectionRegistry incomingIR;
        final IntrospectionRegistry outgoingIR;

        public SerDeSerControl() {
            this.incomingIR = new IntrospectionRegistry();
            this.outgoingIR = new IntrospectionRegistry();
        }

        public void ensureData(int size) {
        }

        public void alignData(int alignment) {
        }

        public void flushSerializeBuffer() {
        }

        public void ensureBuffer(int size) {
        }

        public void alignBuffer(int alignment) {
        }

        public Field cachedDeserialize(ByteBuffer buffer) {
            return incomingIR.deserialize(buffer, this);
        }

        public void cachedSerialize(Field field, ByteBuffer buffer) {
            outgoingIR.serialize(field, buffer, this);
        }

    }

    static final SerDeSerControl control = new SerDeSerControl();

    static void structureExample() {
        FieldCreate fieldCreate = PVFactory.getFieldCreate();
        PVDataCreate pvDataCreate = PVFactory.getPVDataCreate();

        ByteBuffer bb = ByteBuffer.allocate(10240);

        // TODO access via PVFactory?
        StandardField standardField = StandardFieldFactory.getStandardField();

        Structure structure = fieldCreate.createFieldBuilder().
                setId("exampleStructure").
                addArray("value", ScalarType.pvByte).
                addBoundedArray("boundedSizeArray", ScalarType.pvByte, 16).
                addFixedArray("fixedSizeArray", ScalarType.pvByte, 4).
                add("timeStamp", standardField.timeStamp()).
                add("alarm", standardField.alarm()).
                addNestedUnion("valueUnion").
                add("stringValue", ScalarType.pvString).
                add("intValue", ScalarType.pvInt).
                add("doubleValue", ScalarType.pvDouble).
                endNested().
                add("variantUnion", fieldCreate.createVariantUnion()).
                createStructure();

        PVStructure pvStructure = pvDataCreate.createPVStructure(structure);

        PVByteArray ba = (PVByteArray) pvStructure.getSubField("value");
        byte[] toPut = new byte[]{(byte) 1, (byte) 2, (byte) 3};
        ba.put(0, toPut.length, toPut, 0);

        ba = (PVByteArray) pvStructure.getSubField("boundedSizeArray");
        toPut = new byte[]{(byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8};
        ba.put(0, toPut.length, toPut, 0);

        ba = (PVByteArray) pvStructure.getSubField("fixedSizeArray");
        toPut = new byte[]{(byte) 9, (byte) 10, (byte) 11, (byte) 12};
        ba.put(0, toPut.length, toPut, 0);

        PVStructure timeStampStructure = pvStructure.getStructureField("timeStamp");
        timeStampStructure.getLongField("secondsPastEpoch").put(0x1122334455667788L);
        timeStampStructure.getIntField("nanoseconds").put(0xAABBCCDD);
        timeStampStructure.getIntField("userTag").put(0xEEEEEEEE);

        PVStructure alarmStructure = pvStructure.getStructureField("alarm");
        alarmStructure.getIntField("severity").put(0x11111111);
        alarmStructure.getIntField("status").put(0x22222222);
        alarmStructure.getStringField("message").put("Allo, Allo!");

        ((PVInt) pvStructure.getUnionField("valueUnion").select("intValue")).put(0x33333333);

        PVString pvString = (PVString) pvDataCreate.createPVScalar(ScalarType.pvString);
        pvString.put("String inside variant union.");
        pvStructure.getUnionField("variantUnion").set(pvString);

        control.outgoingIR.serialize(pvStructure.getStructure(), bb, control);

        System.out.println(pvStructure.getStructure());
        System.out.println();

        HexDump.hexDump("Serialized structure IF", bb.array(), bb.position());
        System.out.println();


        bb.clear();

        pvStructure.serialize(bb, control);

        System.out.println(pvStructure);
        System.out.println();

        HexDump.hexDump("Serialized structure", bb.array(), bb.position());
        System.out.println();
    }

    public static void main(String[] args) {
        structureExample();
    }

}
