package ac.grim.grimac.utils.anticheat;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PreViaSendDispatcherTest {

    @Test
    void ignoresPostViaListeners() {
        PreViaSendDispatcher dispatcher = new PreViaSendDispatcher();
        dispatcher.register(new Recording("post", PacketListenerPriority.NORMAL, false, new ArrayList<>()));
        assertEquals(0, dispatcher.size());
    }

    @Test
    void ordersByPriorityThenRegistration() {
        PreViaSendDispatcher dispatcher = new PreViaSendDispatcher();
        List<String> calls = new ArrayList<>();
        Recording high = new Recording("high", PacketListenerPriority.HIGH, true, calls);
        Recording lowFirst = new Recording("low-1", PacketListenerPriority.LOW, true, calls);
        Recording lowSecond = new Recording("low-2", PacketListenerPriority.LOW, true, calls);
        Recording monitor = new Recording("monitor", PacketListenerPriority.MONITOR, true, calls);
        dispatcher.register(high);
        dispatcher.register(lowFirst);
        dispatcher.register(monitor);
        dispatcher.register(lowSecond);

        assertArrayEquals(new Object[]{lowFirst, lowSecond, high, monitor}, dispatcher.snapshot());
    }

    @Test
    void replaysEveryListenerEvenWhenOneThrows() {
        List<String> errors = new ArrayList<>();
        PreViaSendDispatcher dispatcher = new PreViaSendDispatcher((message, cause) -> errors.add(message));
        List<String> calls = new ArrayList<>();
        dispatcher.register(new Recording("first", PacketListenerPriority.LOW, true, calls));
        dispatcher.register(new Throwing(PacketListenerPriority.NORMAL));
        dispatcher.register(new Recording("last", PacketListenerPriority.HIGH, true, calls));

        // The event is only handed through; the stubs never dereference it.
        dispatcher.dispatch(null);

        assertEquals(List.of("first", "last"), calls);
        assertEquals(List.of("Pre-Via send replay failed in Throwing"), errors);
    }

    private static final class Recording extends PacketListenerAbstract {
        private final String name;
        private final boolean preVia;
        private final List<String> calls;

        Recording(String name, PacketListenerPriority priority, boolean preVia, List<String> calls) {
            super(priority);
            this.name = name;
            this.preVia = preVia;
            this.calls = calls;
        }

        @Override
        public boolean isPreVia() {
            return preVia;
        }

        @Override
        public void onPacketSend(PacketSendEvent event) {
            calls.add(name);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final class Throwing extends PacketListenerAbstract {
        Throwing(PacketListenerPriority priority) {
            super(priority);
        }

        @Override
        public boolean isPreVia() {
            return true;
        }

        @Override
        public void onPacketSend(PacketSendEvent event) {
            throw new IllegalStateException("boom");
        }
    }
}
