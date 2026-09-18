package com.example.motorcycleaddon.item;

import com.example.motorcycleaddon.MotorcycleAddon;
import com.example.tudursvehiclemod.item.VehicleConverterTarget;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

/** Opts the motorcycle into the base mod's own tiered base item -> spawner item converter, and into
 * packing a placed motorcycle back up into a spawner item.
 *
 * Registering this is entirely optional. An addon that wants a costlier or otherwise independent
 * way of obtaining its vehicles simply never registers a target, and the converter then has no
 * knowledge of it at all. */
public class MotorcycleConverterTarget implements VehicleConverterTarget {

	/** Must match the entity type registered in MotorcycleAddon, and the "entity_type" every
	 * motorcycle vehicle JSON names - this is what ties a placed vehicle back to this target. */
	@Override
	public Identifier entityTypeId() {
		return Identifier.of(MotorcycleAddon.MOD_ID, "motorcycle");
	}

	@Override
	public String translationKey() {
		return "item.motorcycleaddon.category.motorcycle";
	}

	/** This addon's own namespace, so it can never collide with the base mod's own target ids. */
	@Override
	public Identifier id() {
		return Identifier.of(MotorcycleAddon.MOD_ID, "motorcycle");
	}

	@Override
	public Item[] tieredSpawnerItems() {
		return MotorcycleItems.MOTORCYCLE_SPAWNERS;
	}
}
