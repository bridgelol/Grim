package ac.grim.grimac.utils.anticheat;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Replays Grim's pre-Via send listeners for native-protocol connections.
 * <p>
 * {@link SendPathOptimizer} drops the pre-Via PacketEvents encoder once a client is on the
 * server protocol. That encoder is the only thing that ever calls listeners answering
 * {@code isPreVia() == true}, so dropping it also silences every global pre-Via listener:
 * self metadata (riptide pose, server-side item-use resync), bed state, block actions.
 * Those track server-authoritative state Grim cannot infer from the client, so the post-Via
 * pass replays them through this dispatcher once the encoder is gone.
 * <p>
 * Listeners run in PacketEvents priority order (LOWEST first, MONITOR last); registration
 * order breaks ties, matching {@code EventManager}.
 */
public final class PreViaSendDispatcher {
    public static final PreViaSendDispatcher INSTANCE = new PreViaSendDispatcher();

    private static final PacketListenerAbstract[] EMPTY = new PacketListenerAbstract[0];

    private final List<PacketListenerAbstract> registered = new ArrayList<>();
    private final BiConsumer<String, Throwable> errorSink;
    private volatile PacketListenerAbstract[] listeners = EMPTY;

    PreViaSendDispatcher() {
        this(LogUtil::error);
    }

    /** Test seam: {@link LogUtil} needs a live Grim platform. */
    PreViaSendDispatcher(@NotNull BiConsumer<String, Throwable> errorSink) {
        this.errorSink = errorSink;
    }

    /**
     * Records {@code listener} for replay when it is a pre-Via listener. Non-pre-Via listeners
     * are ignored so callers can route every registration through here. Only
     * {@link PacketListenerAbstract} exposes {@code onPacketSend} publicly; every Grim listener is one.
     */
    public synchronized void register(@NotNull PacketListenerCommon listener) {
        if (!listener.isPreVia() || !(listener instanceof PacketListenerAbstract abstractListener)) {
            return;
        }
        registered.add(abstractListener);
        // Stable sort: equal priorities keep registration order, as PacketEvents does.
        registered.sort(Comparator.comparingInt(l -> l.getPriority().ordinal()));
        listeners = registered.toArray(EMPTY);
    }

    /**
     * Calls every registered pre-Via listener with {@code event}. A listener that throws is
     * logged and skipped so a single failure cannot drop the packet for the others.
     */
    public void dispatch(@NotNull PacketSendEvent event) {
        for (PacketListenerAbstract listener : listeners) {
            try {
                listener.onPacketSend(event);
            } catch (Exception e) {
                errorSink.accept("Pre-Via send replay failed in " + listener.getClass().getSimpleName(), e);
            }
        }
    }

    int size() {
        return listeners.length;
    }

    PacketListenerAbstract[] snapshot() {
        return listeners.clone();
    }
}
