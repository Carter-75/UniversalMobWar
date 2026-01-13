package mod.universalmobwar.net;

import mod.universalmobwar.UniversalMobWarMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record UmwClientEnchantmentsPayload(List<Identifier> enchantmentIds) implements CustomPayload {

    public static final CustomPayload.Id<UmwClientEnchantmentsPayload> ID =
        new CustomPayload.Id<>(Identifier.of(UniversalMobWarMod.MODID, "client_enchantments"));

    public static final PacketCodec<RegistryByteBuf, UmwClientEnchantmentsPayload> CODEC =
        PacketCodec.ofStatic(UmwClientEnchantmentsPayload::write, UmwClientEnchantmentsPayload::read);

    public static UmwClientEnchantmentsPayload read(RegistryByteBuf buf) {
        int count = buf.readVarInt();
        List<Identifier> ids = new ArrayList<>(Math.max(0, count));
        for (int i = 0; i < count; i++) {
            ids.add(buf.readIdentifier());
        }
        return new UmwClientEnchantmentsPayload(ids);
    }

    public static void write(RegistryByteBuf buf, UmwClientEnchantmentsPayload payload) {
        List<Identifier> ids = payload.enchantmentIds();
        buf.writeVarInt(ids.size());
        for (Identifier id : ids) {
            buf.writeIdentifier(id);
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
