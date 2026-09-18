package com.example.motorcycleaddon.asset;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/** Per-vehicle MotorcycleSettings, keyed by the same vehicle Identifier the base mod's own
 * VehicleRegistry uses.
 *
 * Scans data/&lt;namespace&gt;/vehicles/*.json - the exact same files the base mod parses - and picks
 * out only the "motorcycle" object, leaving every other key to the base mod. Reloading on the same
 * resource-reload event means /reload updates both halves together.
 *
 * NOTE: this reads the resource-pack/datapack tree only. Vehicles supplied through the base mod's
 * own external tudursvehiclemod-addons/ folder still load and drive fine - they simply fall back to
 * MotorcycleSettings.DEFAULT, since this listener never sees those files. */
public final class MotorcycleSettingsRegistry implements SimpleSynchronousResourceReloadListener {

	private static final Logger LOGGER = LoggerFactory.getLogger("MotorcycleAddon/Settings");
	private static final String DIRECTORY = "vehicles";
	private static final String SUFFIX = ".json";
	/** The one key inside a vehicle JSON that belongs to this addon. */
	private static final String SETTINGS_KEY = "motorcycle";

	private static Map<Identifier, MotorcycleSettings> loaded = Map.of();

	/** Settings for one vehicle, or DEFAULT if it declared none. Never null. */
	public static MotorcycleSettings get(Identifier vehicleId) {
		if (vehicleId == null) {
			return MotorcycleSettings.DEFAULT;
		}
		return loaded.getOrDefault(vehicleId, MotorcycleSettings.DEFAULT);
	}

	@Override
	public Identifier getFabricId() {
		return Identifier.of("motorcycleaddon", "motorcycle_settings");
	}

	@Override
	public void reload(ResourceManager manager) {
		Map<Identifier, MotorcycleSettings> result = new HashMap<>();

		for (Map.Entry<Identifier, Resource> entry :
				manager.findResources(DIRECTORY, id -> id.getPath().endsWith(SUFFIX)).entrySet()) {

			Identifier fileId = entry.getKey();
			String path = fileId.getPath();
			// Same id derivation the base mod's own listener uses, so the keys line up exactly.
			Identifier vehicleId = Identifier.of(
					fileId.getNamespace(),
					path.substring(DIRECTORY.length() + 1, path.length() - SUFFIX.length())
			);

			try (Reader reader = entry.getValue().getReader()) {
				JsonElement json = JsonParser.parseReader(reader);
				if (!json.isJsonObject()) {
					continue;
				}
				JsonObject obj = json.getAsJsonObject();
				if (!obj.has(SETTINGS_KEY)) {
					continue;
				}
				MotorcycleSettings.CODEC.parse(JsonOps.INSTANCE, obj.get(SETTINGS_KEY))
						.resultOrPartial(error ->
								LOGGER.error("Failed to parse motorcycle settings for '{}': {}", vehicleId, error))
						.ifPresent(settings -> result.put(vehicleId, settings));
			} catch (Exception e) {
				LOGGER.error("Failed to read motorcycle settings for {}", vehicleId, e);
			}
		}

		loaded = Map.copyOf(result);
		LOGGER.info("Loaded motorcycle settings for {} vehicle(s)", loaded.size());
	}
}
