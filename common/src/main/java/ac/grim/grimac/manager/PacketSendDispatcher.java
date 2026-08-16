package ac.grim.grimac.manager;

import ac.grim.grimac.checks.type.PacketCheck;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-player outbound check dispatch. Built once when {@link CheckManager}
 * constructs: checks that do not override {@code onPacketSend} are omitted,
 * and the rest are indexed by {@link PacketCheck#handledSendTypes()}.
 */
public final class PacketSendDispatcher {
    private static final PacketCheck[] EMPTY = new PacketCheck[0];

    private final Map<PacketTypeCommon, PacketCheck[]> byType;
    private final PacketCheck[] catchAll;

    @SafeVarargs
    public PacketSendDispatcher(List<? extends PacketCheck>... groups) {
        Map<PacketTypeCommon, List<PacketCheck>> tmp = new IdentityHashMap<>();
        List<PacketCheck> always = new ArrayList<>();

        for (List<? extends PacketCheck> group : groups) {
            for (PacketCheck check : group) {
                if (!overridesOnPacketSend(check.getClass())) {
                    continue;
                }
                PacketTypeCommon[] types = check.handledSendTypes();
                if (types == null || types.length == 0) {
                    always.add(check);
                    continue;
                }
                for (PacketTypeCommon type : types) {
                    if (type == null) continue;
                    tmp.computeIfAbsent(type, ignored -> new ArrayList<>()).add(check);
                }
            }
        }

        Map<PacketTypeCommon, PacketCheck[]> built = new IdentityHashMap<>(tmp.size());
        tmp.forEach((type, checks) -> built.put(type, checks.toArray(EMPTY)));
        this.byType = built;
        this.catchAll = always.toArray(EMPTY);
    }

    public void dispatch(final PacketSendEvent event) {
        PacketCheck[] typed = byType.get(event.getPacketType());
        if (typed != null) {
            for (PacketCheck check : typed) {
                check.onPacketSend(event);
            }
        }
        for (PacketCheck check : catchAll) {
            check.onPacketSend(event);
        }
    }

    int typedTypeCount() {
        return byType.size();
    }

    int catchAllCount() {
        return catchAll.length;
    }

    static boolean overridesOnPacketSend(Class<?> clazz) {
        try {
            Method method = clazz.getMethod("onPacketSend", PacketSendEvent.class);
            return method.getDeclaringClass() != PacketCheck.class;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
