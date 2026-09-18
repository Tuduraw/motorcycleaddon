package com.example.motorcycleaddon.entity;

import com.example.motorcycleaddon.MotorcycleAddon;
import com.example.motorcycleaddon.asset.MotorcycleSettings;
import com.example.motorcycleaddon.asset.MotorcycleSettingsRegistry;
import com.example.tudursvehiclemod.asset.VehicleDefinition;
import com.example.tudursvehiclemod.entity.CarEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/** A two-wheeler.
 *
 * Extends CarEntity, so throttle/steering response, step-up, wheel and steering-part animation,
 * water handling and the cosmetic terrain tilt all come across unchanged. What this class adds is
 * the two-wheeler-specific behaviour:
 *
 * <ul>
 *   <li>a cornering LEAN, layered on top of the terrain tilt CarEntity already computes;</li>
 *   <li>two CORNERING MODES, so lean can be turned off when it would fight the steering-part
 *       animation visually;</li>
 *   <li>optional WHEELIE acrobatics.</li>
 * </ul>
 *
 * All of it is driven by per-vehicle settings read from the vehicle's own JSON (see
 * MotorcycleSettings), so a scooter, a sidecar and a sports bike are all this one class with
 * different numbers - no subclass per tier.
 *
 * CORNERING MODES. Lean and a hard-swinging handlebar/front-wheel look wrong together: the bike
 * banks one way while the fork visibly cranks the other. Rather than compromise both, the rider
 * picks one:
 *
 * <ul>
 *   <li>UPRIGHT (mode 1) - no lean at all, so the steering parts read clearly. Turn rate is
 *       multiplied by upright_turn_penalty, so this genuinely handles worse.</li>
 *   <li>LEANING (mode 2) - full lean and full turn rate.</li>
 * </ul>
 *
 * This reuses the base mod's own MANUAL_MODE flag and its existing key/payload rather than adding a
 * parallel toggle: that state is already synced, already persisted per-entity, and already bound to
 * a key the player knows. Manual mode OFF is UPRIGHT (the safer default a newly-spawned bike gets);
 * ON is LEANING.
 *
 * Lean and wheelie are both applied to the rendered attitude only, never to movement - exactly how
 * the base mod's own ShipEntity treats its roll. */
public class MotorcycleEntity extends CarEntity {

	/** Minimum speed (blocks/tick) before a wheelie will start - a stationary bike shouldn't rear up. */
	private static final float WHEELIE_MIN_SPEED = 0.15f;

	/** The cornering lean, tracked separately from the inherited roll field so terrain tilt and lean
	 * stay independent rather than accumulating into each other. */
	private float leanDegrees;

	/** Current wheelie angle, eased toward its target. Non-zero only while acrobatics are enabled
	 * and the rider is holding the trigger. */
	private float wheelieDegrees;

	/** Set from the acrobatics key by the client, and synced to the server as a driving input. */
	private boolean acrobaticsHeld;

	public MotorcycleEntity(EntityType<?> type, World world) {
		super(type, world);
	}

	@Override
	protected Identifier defaultDefinitionId() {
		return Identifier.of(MotorcycleAddon.MOD_ID, "motorcycle");
	}

	/** Narrower and shorter than a car's own 1.6 x 1.0 box, so a bike fits through gaps a car can't.
	 * This is the block-collision hitbox only - the visible model comes from the OBJ. */
	@Override
	public net.minecraft.entity.EntityDimensions getDimensions(net.minecraft.entity.EntityPose pose) {
		return net.minecraft.entity.EntityDimensions.changing(0.8f, 1.2f);
	}

	/** This vehicle's own motorcycle settings, from its own JSON. Never null. */
	public MotorcycleSettings tudursvehiclemod$settings() {
		return MotorcycleSettingsRegistry.get(this.getVehicleDefinitionId());
	}

	/** True when the rider has selected the leaning (higher-performance) cornering mode.
	 *
	 * A vehicle configured with max_lean_degrees = 0 - a sidecar, say - never leans whichever mode
	 * is selected, so for those this only affects the turn-rate penalty. */
	public boolean tudursvehiclemod$isLeaningMode() {
		return this.isManualMode();
	}

	/** Upright cornering is deliberately worse than leaning cornering - that trade is the whole
	 * reason the modes exist. Applied here rather than inside the lean maths so it affects the
	 * ACTUAL yaw rate, which is what CarEntity's own steering reads. */
	@Override
	public float tudursvehiclemod$getEffectiveTurnSpeed() {
		float base = super.tudursvehiclemod$getEffectiveTurnSpeed();
		if (this.tudursvehiclemod$isLeaningMode()) {
			return base;
		}
		return base * Math.max(0.05f, this.tudursvehiclemod$settings().uprightTurnPenalty());
	}

	/** Called from the client when the acrobatics key's held state changes, and on the server when
	 * that change arrives. Ignored entirely by a vehicle that hasn't opted into acrobatics. */
	public void tudursvehiclemod$setAcrobaticsHeld(boolean held) {
		this.acrobaticsHeld = held;
	}

	public boolean tudursvehiclemod$isAcrobaticsHeld() {
		return this.acrobaticsHeld;
	}

	/** Current wheelie angle in degrees, for anything that wants to read it (HUD, sounds). */
	public float tudursvehiclemod$getWheelieDegrees() {
		return this.wheelieDegrees;
	}

	/** Terrain tilt first (CarEntity's own behaviour, untouched), then this bike's own lean and
	 * wheelie on top.
	 *
	 * this.roll and pitch carry this vehicle's OWN lean/wheelie contribution baked in from the
	 * previous tick, so that contribution is subtracted out FIRST, before super() runs - otherwise
	 * super()'s own terrain easing would treat an already-leaned value as its "current roll" and
	 * CarEntity's fixed 0.2 smoothing only pulls 20% of the way back toward level each tick, so
	 * re-adding the full lean on top every tick compounds without bound (steady state works out to
	 * maxLeanDegrees / 0.2 = 5x the configured value - a 60-degree setting runs away past 300). */
	@Override
	protected void updateCosmeticTilt(VehicleDefinition def, float maxTiltDegrees, float smoothing) {
		// The true previous roll as actually rendered (terrain + last tick's own lean), captured
		// before anything below touches this.roll.
		float previousTotalRoll = this.roll;

		// Undo LAST tick's own contribution so super() eases a clean, lean-free terrain roll/pitch.
		this.roll -= this.leanDegrees;
		this.setPitch(this.getPitch() + this.wheelieDegrees);

		super.updateCosmeticTilt(def, maxTiltDegrees, smoothing);
		// super() just captured this.prevRoll from the CLEAN value above, understating the true
		// previous rendered roll by last tick's own lean - restore it so interpolation between
		// ticks matches what was actually shown, rather than popping by that amount every tick.
		this.prevRoll = previousTotalRoll;

		MotorcycleSettings settings = this.tudursvehiclemod$settings();
		this.tudursvehiclemod$updateLean(settings);
		this.tudursvehiclemod$updateWheelie(settings);

		// Add back exactly ONE tick's worth of lean/wheelie on top of the now-clean terrain value.
		this.roll += this.leanDegrees;
		// Negative pitch is nose-up, matching the sign CarEntity's own climb handling uses.
		this.setPitch(this.getPitch() - this.wheelieDegrees);
	}

	/** Eases leanDegrees toward whatever the current mode, steering input and speed call for.
	 *
	 * Steering comes from the base mod's own SYNCED value rather than raw key state, so this behaves
	 * identically on both sides and for a bike being driven by something other than a player (a Drone
	 * Center ground route, for instance). */
	private void tudursvehiclemod$updateLean(MotorcycleSettings settings) {
		float target = 0f;

		// Upright mode and an un-ridden bike both settle back to level.
		if (this.tudursvehiclemod$isLeaningMode() && this.getControllingPassenger() != null) {
			float maxLean = settings.maxLeanDegrees();
			float fullLeanSpeed = Math.max(0.01f, settings.fullLeanSpeed());
			float steering = MathHelper.clamp(this.getSyncedSidewaysInput(), -1f, 1f);
			float forwardSpeed = this.tudursvehiclemod$getActualForwardSpeed();
			float speedFactor = MathHelper.clamp(Math.abs(forwardSpeed) / fullLeanSpeed, 0f, 1f);

			// Negated so the bike leans INTO the turn, matching the sign convention CarEntity's own
			// terrain roll already uses.
			target = -steering * maxLean * speedFactor;

			// Reversing swings the bike the other way for the same steering input, so the lean follows.
			if (forwardSpeed < 0f) {
				target = -target;
			}
		}

		float smoothing = MathHelper.clamp(settings.leanSmoothing(), 0.01f, 1f);
		this.leanDegrees += (target - this.leanDegrees) * smoothing;
	}

	/** Eases the wheelie angle in while the key is held and out when it isn't.
	 *
	 * Requires the vehicle to have opted in (enable_acrobatics), a rider, and enough forward speed -
	 * so a parked bike can't be left permanently reared up. */
	private void tudursvehiclemod$updateWheelie(MotorcycleSettings settings) {
		float target = 0f;

		if (settings.enableAcrobatics()
				&& this.acrobaticsHeld
				&& this.getControllingPassenger() != null
				&& this.tudursvehiclemod$getActualForwardSpeed() > WHEELIE_MIN_SPEED) {
			target = settings.wheelieAngleDegrees();
		}

		float rate = MathHelper.clamp(settings.wheelieRate(), 0.01f, 1f);
		this.wheelieDegrees += (target - this.wheelieDegrees) * rate;
	}
}
