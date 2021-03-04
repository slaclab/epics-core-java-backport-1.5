package org.epics.pvaccess.client.pvms;

import org.epics.pvaccess.PVAConstants;
import org.epics.pvdata.factory.PVDataFactory;
import org.epics.pvdata.misc.SerializeHelper;
import org.epics.pvdata.pv.DeserializableControl;
import org.epics.pvdata.pv.Field;
import org.epics.pvdata.pv.PVDataCreate;
import org.epics.pvdata.pv.PVField;
import org.epics.util.compat.legacy.net.MulticastSocket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;

class PVMSSubscriber extends PVMSCodec implements DeserializableControl {
    private static PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();

    private final MulticastSocket socket;

    private final ByteBuffer buffer = ByteBuffer.allocate(PVAConstants.MAX_UDP_PACKET);

    private IncomingMulticastIntrospectionRegistry activeRegistry = null;

    // TODO clear!!! removed if not used for certain period of time
    private final Map<PVMSSubscriber.PublisherInfo, IncomingMulticastIntrospectionRegistry> publishers =
            new HashMap<PVMSSubscriber.PublisherInfo, IncomingMulticastIntrospectionRegistry>();

    private boolean destroyed = false;

    private final Set<InetAddress> joinedGroups = new HashSet<InetAddress>();

    private static class PublisherInfo {
        public long id;
        public InetSocketAddress socketAddress;

        public void set(long id, InetSocketAddress socketAddress) {
            this.id = id;
            this.socketAddress = socketAddress;
        }

        @Override
        public int hashCode() {
            return (int) id;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            PVMSSubscriber.PublisherInfo other = (PVMSSubscriber.PublisherInfo) obj;
            if (id != other.id)
                return false;
            if (socketAddress == null) {
                if (other.socketAddress != null)
                    return false;
            } else if (!socketAddress.equals(other.socketAddress))
                return false;
            return true;
        }


    }

    /**
     * Incoming (codes generated by other party) introspection registry.
     *
     * @param address where to listen.
     * @param port    port.
     * @throws IOException rethrown IO exception.
     */
    public PVMSSubscriber(InetAddress address, int port) throws IOException {
        socket = new MulticastSocket(port);
        socket.joinGroup(address);
    }

    public void joinGroup(InetAddress address) throws IOException {
        synchronized (joinedGroups) {
            if (!joinedGroups.contains(address)) {
                socket.joinGroup(address);
                joinedGroups.add(address);
            }
        }
    }

    public void leaveGroup(InetAddress address) throws IOException {
        synchronized (joinedGroups) {
            if (joinedGroups.contains(address)) {
                socket.leaveGroup(address);
                joinedGroups.remove(address);
            }
        }
    }

	/*
	protected synchronized void sendSubscribeControlMessage(String topicID, int expirationTimeSec) throws IOException
	{
		buffer.clear();

		pmsSubscribeControlMessage(buffer, expirationTimeSec, topicID);

		packet.setLength(buffer.position());

		socket.send(packet);
	}

	public void subscribe(String topicId) throws IOException
	{
		// TODO * as all, some filtering?
		// TODO periodic send

		sendSubscribeControlMessage(topicId, 3);
	}
	*/

    public void unsubscribe(String topicId) throws IOException {
        // TODO
    }

    public void ensureData(int size) {
        // TODO Auto-generated method stub

    }

    public void alignData(int alignment) {
        // TODO Auto-generated method stub

    }

    public Field cachedDeserialize(ByteBuffer buffer) {
        return activeRegistry.deserialize(buffer, this);
    }

    public static class PVMSMessage {
        public String topicId;
        public String[] tags;
        public PVField data;

        public PVMSMessage(String topicId, String[] tags, PVField data) {
            this.topicId = topicId;
            this.tags = tags;
            this.data = data;
        }

    }

    private PVMSSubscriber.PublisherInfo cachedPI = new PublisherInfo();

    public void receive(PVMSMessage message, String[] filterTags) throws IOException {
        while (true) {
            InetSocketAddress socketAddress = socket.receive(buffer);

            int t = buffer.getInt();

            // messageSeqNumber (data) of additionalInfo (control)
            int t2 = buffer.getInt();

            // id (timestamp in ms), also destinationSocketID (for multiplexing)
            long id = buffer.getLong();


            int seqNum = t & 0x7FFFFFFF;

            // control packet
            if (t != seqNum) {
                handleControlPacket(buffer, socketAddress, t, t2, id);
                continue;
            }

            //final int messageSeqNumber = t & 0x1FFFFFFF;

            // 10 - first packet of a message (0x80000000)
            // 01 - last packet of a message (0x40000000)
            // 11 - solo message packet (0xC0000000)
            // 00 - packet in the middle of a message (0xC0000000)
            final int positionFlags = t2 & 0xC0000000;
            if (positionFlags != PacketPosition.SOLO.getMaskValue()) {
                // we support only solo-s for now
                continue; // TODO
            }


            // 0 (in order delivery not required) / 1 (in order delivery required) of messages
            //final int inOrderDeliveryRequiredFlag = t2 & 0x20000000;

            synchronized (publishers) {
                cachedPI.set(id, socketAddress);
                activeRegistry = publishers.get(cachedPI);
                if (activeRegistry == null) {
                    activeRegistry = new IncomingMulticastIntrospectionRegistry();
                    publishers.put(cachedPI, activeRegistry);
                    cachedPI = new PublisherInfo();
                }
            }

            // string topicID
            String topicId = SerializeHelper.deserializeString(buffer, this);

            // filter on topicID
            if (message.topicId != null) {
                if (!message.topicId.equals(topicId))
                    continue;
            } else
                message.topicId = topicId;

            // string[] tags
            int tagsCount = SerializeHelper.readSize(buffer, this);
            // TODO do not allocate tags over and over again
            String[] tags = tagsCount > 0 ? new String[tagsCount] : null;
            for (int i = 0; i < tagsCount; i++)
                tags[i] = SerializeHelper.deserializeString(buffer, this);

            // filtering, avoids unnecessary deserialization
            // message is accepted if message.tags (filter) is a subset of tags (received message tags)
            if (filterTags != null && filterTags.length > 0) {
                // can not be a subset, if element count is lower
                if (tags.length < filterTags.length)
                    continue;

                boolean notSubSet = false;
                HashSet<String> hashSet = new HashSet<String>();
                Collections.addAll(hashSet, tags);

                for (String tag : filterTags)
                    if (!hashSet.contains(tag)) {
                        notSubSet = true;
                        break;
                    }

                if (notSubSet)
                    continue;
            }
            message.tags = tags;

            final Field field = this.cachedDeserialize(buffer);
            if (field == null) {
                message.data = null;
                return;
            } else {
                // create new PVField or reuse
                if (message.data == null || !message.data.getField().equals(field))
                    message.data = pvDataCreate.createPVField(field);

                message.data.deserialize(buffer, this);
                return;
            }
        }


    }

    @Override
    protected void handleKeepAlive(InetSocketAddress socketAddress,
                                   long id, int expirationTimeSec) {
        // TODO
    }

    @Override
    protected void handleShutdown(InetSocketAddress socketAddress, long id) {
        synchronized (publishers) {
            cachedPI.set(id, socketAddress);
            activeRegistry = publishers.remove(cachedPI);
        }
    }

    public synchronized void destroy() {
        if (destroyed)
            return;
        destroyed = true;

        try {
            synchronized (joinedGroups) {
                for (InetAddress g : joinedGroups)
                    socket.leaveGroup(g);
            }
        } catch (IOException e) {
            // noop
        }

        socket.close();

    }
}
