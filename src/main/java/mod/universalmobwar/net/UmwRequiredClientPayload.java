package mod.universalmobwar.net;

import mod.universalmobwar.UniversalMobWarMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Empty payload used only to verify that a connecting client has UniversalMobWar installed.
 *
 * The server checks {@code ServerPlayNetworking.canSend(...)} for this payload ID.
 * If the client doesn't have the mod, it won't advertise this payload and will be rejected.
 */
public record UmwRequiredClientPayload() implements CustomPayload {

    public static final CustomPayload.Id<UmwRequiredClientPayload> ID =
        new CustomPayload.Id<>(Identifier.of(UniversalMobWarMod.MODID, "required_client"));

    public static final PacketCodec<RegistryByteBuf, UmwRequiredClientPayload> CODEC =
        PacketCodec.ofStatic(UmwRequiredClientPayload::write, UmwRequiredClientPayload::read);

    public static UmwRequiredClientPayload read(RegistryByteBuf buf) {
        return new UmwRequiredClientPayload();
    }

    public static void write(RegistryByteBuf buf, UmwRequiredClientPayload payload) {
        // Intentionally empty.
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
