package com.example.motorcycleaddon.item;

import com.example.motorcycleaddon.MotorcycleAddon;
import com.example.tudursvehiclemod.item.TieredVehicleSpawnerItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** This addon's own items.
 *
 * These are the base mod's own TieredVehicleSpawnerItem, not a bespoke item: that class is typed
 * against VehicleConverterTarget, so an addon's own target works in it directly. The payoff is the
 * base mod's own vehicle SELECTION screen - right-clicking a tier-3 spawner lists every registered
 * motorcycle of tier 3 or below, searchable and paged, with no screen or networking code here.
 *
 * The base mod still creates nothing on an addon's behalf: the items are registered here, under this
 * addon's own namespace, and the resulting array is what gets handed to the converter registry. */
public class MotorcycleItems {

	/** [tier - 1]. Handed to the base mod's converter through MotorcycleConverterTarget. */
	public static final Item[] MOTORCYCLE_SPAWNERS = new Item[5];

	/** The single converter target these items are scoped to - also what the selection screen filters
	 * by, so only motorcycles appear in it. */
	public static final MotorcycleConverterTarget TARGET = new MotorcycleConverterTarget();

	public static void register() {
		for (int tier = 1; tier <= 5; tier++) {
			String path = "motorcycle_spawner_t" + tier;
			RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MotorcycleAddon.MOD_ID, path));
			Item item = new TieredVehicleSpawnerItem(
					new Item.Settings().registryKey(key).maxCount(1), TARGET, tier);
			MOTORCYCLE_SPAWNERS[tier - 1] = Registry.register(Registries.ITEM, key, item);
		}

		RegistryKey<ItemGroup> groupKey =
				RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(MotorcycleAddon.MOD_ID, "motorcycles"));
		Registry.register(Registries.ITEM_GROUP, groupKey, FabricItemGroup.builder()
				.icon(() -> new ItemStack(MOTORCYCLE_SPAWNERS[0]))
				.displayName(Text.translatable("itemGroup.motorcycleaddon.motorcycles"))
				.build());

		ItemGroupEvents.modifyEntriesEvent(groupKey).register(entries -> {
			for (Item item : MOTORCYCLE_SPAWNERS) {
				entries.add(item);
			}
		});
	}
}
