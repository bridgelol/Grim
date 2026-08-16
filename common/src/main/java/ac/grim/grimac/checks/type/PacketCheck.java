package ac.grim.grimac.checks.type;

import ac.grim.grimac.api.AbstractCheck;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;

public interface PacketCheck extends AbstractCheck {
    PacketTypeCommon[] EMPTY_SEND_TYPES = new PacketTypeCommon[0];

    default void onPacketReceive(final PacketReceiveEvent event) {
    }

    default void onPacketSend(final PacketSendEvent event) {
    }

    /**
     * Outbound packet types this check inspects. Empty means it is skipped on
     * the send path. Checks that override {@link #onPacketSend} should declare
     * the types they actually handle so {@code CheckManager} does not walk
     * every send listener on every packet.
     */
    default PacketTypeCommon[] handledSendTypes() {
        return EMPTY_SEND_TYPES;
    }
}
