package ac.grim.grimac.manager;

import ac.grim.grimac.checks.type.PacketCheck;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketSendDispatcherTest {

    @Test
    void skipsChecksThatDoNotOverrideOnPacketSend() {
        PacketSendDispatcher dispatcher = new PacketSendDispatcher(List.of(new ReceiveOnlyCheck()));
        assertEquals(0, dispatcher.typedTypeCount());
        assertEquals(0, dispatcher.catchAllCount());
    }

    @Test
    void indexesDeclaredSendTypes() {
        PacketSendDispatcher dispatcher = new PacketSendDispatcher(List.of(
                new TypedSendCheck(PacketType.Play.Server.KEEP_ALIVE),
                new TypedSendCheck(PacketType.Play.Server.EXPLOSION)
        ));
        assertEquals(2, dispatcher.typedTypeCount());
        assertEquals(0, dispatcher.catchAllCount());
    }

    @Test
    void undeclaredSendOverrideIsCatchAll() {
        PacketSendDispatcher dispatcher = new PacketSendDispatcher(List.of(new UndeclaredSendCheck()));
        assertEquals(0, dispatcher.typedTypeCount());
        assertEquals(1, dispatcher.catchAllCount());
    }

    @Test
    void overrideDetectionSeesInterfaceDefaultAsNoSend() {
        assertFalse(PacketSendDispatcher.overridesOnPacketSend(ReceiveOnlyCheck.class));
        assertTrue(PacketSendDispatcher.overridesOnPacketSend(TypedSendCheck.class));
        assertTrue(PacketSendDispatcher.overridesOnPacketSend(UndeclaredSendCheck.class));
    }

    private static final class ReceiveOnlyCheck extends StubCheck {
    }

    private static final class TypedSendCheck extends StubCheck {
        private final PacketTypeCommon type;

        private TypedSendCheck(PacketTypeCommon type) {
            this.type = type;
        }

        @Override
        public PacketTypeCommon[] handledSendTypes() {
            return new PacketTypeCommon[]{type};
        }

        @Override
        public void onPacketSend(PacketSendEvent event) {
        }
    }

    private static final class UndeclaredSendCheck extends StubCheck {
        @Override
        public void onPacketSend(PacketSendEvent event) {
        }
    }

    private abstract static class StubCheck implements PacketCheck {
        @Override
        public String getCheckName() {
            return getClass().getSimpleName();
        }

        @Override
        public String getConfigName() {
            return getCheckName();
        }

        @Override
        public double getViolations() {
            return 0;
        }

        @Override
        public long getLastViolationTime() {
            return 0;
        }

        @Override
        public double getDecay() {
            return 0;
        }

        @Override
        public double getSetbackVL() {
            return 0;
        }

        @Override
        public boolean isExperimental() {
            return false;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public void setEnabled(boolean enabled) {
        }

        @Override
        public void reload() {
        }
    }
}
