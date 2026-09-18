package com.example.motorcycleaddon.client;

import com.example.motorcycleaddon.MotorcycleAddon;
import com.example.motorcycleaddon.entity.MotorcycleEntity;
import com.example.motorcycleaddon.network.AcrobaticsPayload;
import com.example.tudursvehiclemod.client.render.VehicleEntityRenderer;
import com.example.tudursvehiclemod.entity.AbstractVehicleEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/** Client-side setup.
 *
 * Rendering needs no code of its own: the base mod's own VehicleEntityRenderer draws any
 * AbstractVehicleEntity subclass, so OBJ loading, texturing, translucency, wheel/steering part
 * animation and the roll this entity computes all come for free.
 *
 * The one client concern is the acrobatics key. Cornering MODE deliberately has no key here - it
 * reuses the base mod's own manual-mode key (hold M by default), so riders don't learn a second
 * binding for the same idea. */
public class MotorcycleAddonClient implements ClientModInitializer {

	private static KeyBinding acrobaticsKey;

	/** Last value sent to the server, so the payload goes out on CHANGE only rather than every tick. */
	private static boolean lastSentHeld;

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(MotorcycleAddon.MOTORCYCLE, VehicleEntityRenderer::new);

		KeyBinding.Category category =
				KeyBinding.Category.create(Identifier.of(MotorcycleAddon.MOD_ID, "motorcycle"));

		acrobaticsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.motorcycleaddon.acrobatics",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_X,
				category
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) {
				return;
			}

			AbstractVehicleEntity vehicle =
					AbstractVehicleEntity.tudursvehiclemod$getEffectiveVehicle(client.player);
			boolean onMotorcycle = vehicle instanceof MotorcycleEntity;
			boolean held = onMotorcycle && acrobaticsKey.isPressed();

			if (held != lastSentHeld) {
				lastSentHeld = held;
				// Applied locally too, so the wheelie starts on this client without waiting for the
				// round trip - the server's own copy is what everyone else sees.
				if (vehicle instanceof MotorcycleEntity motorcycle) {
					motorcycle.tudursvehiclemod$setAcrobaticsHeld(held);
				}
				ClientPlayNetworking.send(new AcrobaticsPayload(held));
			}
		});
	}
}
