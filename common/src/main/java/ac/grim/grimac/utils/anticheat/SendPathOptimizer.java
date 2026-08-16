package ac.grim.grimac.utils.anticheat;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Send-path fast-path: skip Grim's pre-Via encoder when the client is already
 * on the server protocol, and gate CheckManager send dispatch from config.
 */
public final class SendPathOptimizer {
    private SendPathOptimizer() {
    }

    public static boolean needsViaTranslation(@NotNull GrimPlayer player) {
        if (!ac.grim.grimac.utils.viaversion.ViaVersionUtil.isAvailable) {
            return clientProtocolDiffersFromServer(player.getClientVersion());
        }

        UserConnection via = player.viaUserConnection();
        if (via != null) {
            ProtocolVersion client = via.getProtocolInfo().protocolVersion();
            ProtocolVersion server = via.getProtocolInfo().serverProtocolVersion();
            if (client == null || server == null) {
                return clientProtocolDiffersFromServer(player.getClientVersion());
            }
            if (client.equals(server)) {
                return false;
            }
            return Via.getManager().getProtocolManager().getProtocolPath(client, server) != null;
        }

        return clientProtocolDiffersFromServer(player.getClientVersion());
    }

    private static boolean clientProtocolDiffersFromServer(@Nullable ClientVersion client) {
        if (client == null) return false;
        ServerVersion server = PacketEvents.getAPI().getServerManager().getVersion();
        return client.getProtocolVersion() != server.getProtocolVersion();
    }

    /**
     * Drop PacketEvents' pre-Via outbound encoder. Safe to call repeatedly;
     * always runs on the connection's event loop.
     */
    public static void disablePreViaSendEncoder(@NotNull User user) {
        Object raw = user.getChannel();
        if (!(raw instanceof Channel channel) || !channel.isOpen()) {
            return;
        }
        Runnable remove = () -> {
            String name = "pre-" + PacketEvents.ENCODER_NAME;
            ChannelPipeline pipeline = channel.pipeline();
            if (pipeline.get(name) != null) {
                pipeline.remove(name);
            }
        };
        if (channel.eventLoop().inEventLoop()) {
            remove.run();
        } else {
            channel.eventLoop().execute(remove);
        }
    }

    public static void handleWindowAndBundle(@NotNull GrimPlayer player, @NotNull PacketSendEvent event) {
        PacketTypeCommon packetType = event.getPacketType();
        if (packetType == PacketType.Play.Server.OPEN_WINDOW || packetType == PacketType.Play.Server.OPEN_HORSE_WINDOW) {
            player.sendTransaction();
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.serverOpenedInventoryThisTick = true);
        }
        if (packetType == PacketType.Play.Server.BUNDLE) {
            player.packetStateData.sendingBundlePacket = !player.packetStateData.sendingBundlePacket;
        }
    }
}
