package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.SendPathOptimizer;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import org.jetbrains.annotations.NotNull;

public class PreViaCheckManagerListener extends PacketListenerAbstract {

    public PreViaCheckManagerListener() {
        super(PacketListenerPriority.LOW);
    }

    @Override
    public boolean isPreVia() {
        return true;
    }

    @Override
    public void onPacketReceive(final @NotNull PacketReceiveEvent event) {
        // Allow checks to listen to configuration packets
        if (event.getConnectionState() == ConnectionState.CONFIGURATION) {
            final GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;
            player.checkManager.onPreViaPacketReceive(event);
        }

        if (event.getConnectionState() != ConnectionState.PLAY) return;
        final GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
        if (player == null) return;

        player.checkManager.onPreViaPacketReceive(event);
    }

    @Override
    public void onPacketSend(final @NotNull PacketSendEvent event) {
        final GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
        if (player == null) return;

        // Reached from the pre-Via encoder while it is installed, or replayed by
        // CheckManagerListener (PreViaSendDispatcher) once a native client has dropped it.
        // Exactly one of the two happens per packet (the post-Via pass removes the encoder
        // before deciding), so no native gate here.

        if (event.getConnectionState() == ConnectionState.CONFIGURATION) {
            if (player.shouldRunSendChecks()) {
                player.checkManager.onPreViaPacketSend(event);
            }
            return;
        }

        if (event.getConnectionState() != ConnectionState.PLAY) return;

        SendPathOptimizer.handleWindowAndBundle(player, event);

        if (!player.shouldRunSendChecks()) {
            return;
        }
        player.checkManager.onPreViaPacketSend(event);
    }
}
