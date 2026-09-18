package com.example.motorcycleaddon.network;

import com.example.motorcycleaddon.MotorcycleAddon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Client -> server: whether the rider is currently holding the acrobatics key.
 *
 * A held state rather than a toggle, so the wheelie lasts exactly as long as the key is down. Sent
 * only when the value actually changes (see MotorcycleAddonClient), not every tick. */
public record AcrobaticsPayload(boolean held) implements CustomPayload {

	public static final CustomPayload.Id<AcrobaticsPayload> ID =
			new CustomPayload.Id<>(Identifier.of(MotorcycleAddon.MOD_ID, "acrobatics"));

	public static final PacketCodec<RegistryByteBuf, AcrobaticsPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.BOOLEAN, AcrobaticsPayload::held,
			AcrobaticsPayload::new
	);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
