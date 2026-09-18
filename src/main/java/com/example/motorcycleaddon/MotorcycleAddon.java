package com.example.motorcycleaddon;

import com.example.motorcycleaddon.asset.MotorcycleSettingsRegistry;
import com.example.motorcycleaddon.entity.MotorcycleEntity;
import com.example.motorcycleaddon.item.MotorcycleItems;
import com.example.motorcycleaddon.network.AcrobaticsPayload;
import com.example.tudursvehiclemod.entity.AbstractVehicleEntity;
import com.example.tudursvehiclemod.item.VehicleConverterTargets;
import com.example.tudursvehiclemod.registry.ModEntityTypes;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

/** Adds two-wheelers on top of Tudur's Vehicle Mod.
 *
 * Everything here goes through the base mod's own public addon API - no mixins into it, and nothing
 * reaching past what it deliberately exposes. */
public class MotorcycleAddon implements ModInitializer {

	public static final String MOD_ID = "motorcycleaddon";

	public static EntityType<MotorcycleEntity> MOTORCYCLE;

	@Override
	public void onInitialize() {
		// One entity type serves every tier - a scooter, a sidecar and a sports bike differ only in
		// their own JSON, not in code. Width/height here are the entity TYPE's own registered size;
		// MotorcycleEntity overrides getDimensions() with the same values.
		MOTORCYCLE = ModEntityTypes.registerAddonVehicleType(
				Identifier.of(MOD_ID, "motorcycle"),
				MotorcycleEntity::new,
				0.8f, 1.2f);

		MotorcycleItems.register();

		// Opt into the base mod's own tiered base item -> spawner item conversion. Purely optional:
		// without this the motorcycles still work, they just wouldn't be obtainable through the
		// converter block, leaving an addon free to define its own (costlier) route instead.
		VehicleConverterTargets.register(MotorcycleItems.TARGET);

		// Reads this addon's own "motorcycle" object out of the same vehicle JSONs the base mod
		// parses - see MotorcycleSettingsRegistry.
		ResourceManagerHelper.get(ResourceType.SERVER_DATA)
				.registerReloadListener(new MotorcycleSettingsRegistry());

		PayloadTypeRegistry.playC2S().register(AcrobaticsPayload.ID, AcrobaticsPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(AcrobaticsPayload.ID, (payload, context) ->
				context.server().execute(() -> {
					// The base mod's own helper: resolves the vehicle the player is actually
					// controlling, including when seated in a child/turret seat.
					AbstractVehicleEntity vehicle =
							AbstractVehicleEntity.tudursvehiclemod$getEffectiveVehicle(context.player());
					if (vehicle instanceof MotorcycleEntity motorcycle) {
						motorcycle.tudursvehiclemod$setAcrobaticsHeld(payload.held());
					}
				}));
	}
}
