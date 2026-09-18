package com.example.motorcycleaddon.asset;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** The motorcycle-specific part of a vehicle's own settings.
 *
 * These live under a "motorcycle" object INSIDE the ordinary vehicle JSON:
 *
 * <pre>
 * {
 *   "entity_type": "motorcycleaddon:motorcycle",
 *   ...
 *   "motorcycle": {
 *     "max_lean_degrees": 60.0,
 *     "enable_acrobatics": true
 *   }
 * }
 * </pre>
 *
 * The base mod's own VehicleDefinition codec ignores keys it doesn't recognise, so adding this
 * object doesn't disturb its parsing at all - and the addon reads the same file for its own half.
 * That keeps one vehicle described by one file rather than splitting its settings across two.
 *
 * Every field is optional; a vehicle with no "motorcycle" object at all gets DEFAULT. */
public record MotorcycleSettings(
		float maxLeanDegrees,
		float leanSmoothing,
		float fullLeanSpeed,
		float uprightTurnPenalty,
		boolean enableAcrobatics,
		float wheelieAngleDegrees,
		float wheelieRate
) {

	/** Used for any motorcycle whose own JSON omits the "motorcycle" object. Matches the values the
	 * addon shipped with before these became configurable. */
	public static final MotorcycleSettings DEFAULT =
			new MotorcycleSettings(35.0f, 0.15f, 0.35f, 0.6f, false, 35.0f, 0.08f);

	public static final Codec<MotorcycleSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			/* How far the bike leans from cornering alone. 0 disables lean entirely - which is what
			 * a sidecar wants, since a three-wheeler doesn't bank. */
			Codec.FLOAT.optionalFieldOf("max_lean_degrees", 35.0f).forGetter(MotorcycleSettings::maxLeanDegrees),
			/* Per-tick blend factor for how quickly the lean follows the steering input. */
			Codec.FLOAT.optionalFieldOf("lean_smoothing", 0.15f).forGetter(MotorcycleSettings::leanSmoothing),
			/* Speed (blocks/tick) at which the bike leans by the full amount asked for. */
			Codec.FLOAT.optionalFieldOf("full_lean_speed", 0.35f).forGetter(MotorcycleSettings::fullLeanSpeed),
			/* Turn rate multiplier while in UPRIGHT mode - see MotorcycleEntity's own mode doc. Below
			 * 1 means upright cornering is genuinely worse, which is the trade the mode exists for. */
			Codec.FLOAT.optionalFieldOf("upright_turn_penalty", 0.6f).forGetter(MotorcycleSettings::uprightTurnPenalty),
			/* Whether this vehicle can do acrobatics at all. Off unless a vehicle opts in. */
			Codec.BOOL.optionalFieldOf("enable_acrobatics", false).forGetter(MotorcycleSettings::enableAcrobatics),
			/* Nose-up angle held during a wheelie, in degrees. */
			Codec.FLOAT.optionalFieldOf("wheelie_angle_degrees", 35.0f).forGetter(MotorcycleSettings::wheelieAngleDegrees),
			/* Per-tick blend factor for entering and leaving a wheelie. */
			Codec.FLOAT.optionalFieldOf("wheelie_rate", 0.08f).forGetter(MotorcycleSettings::wheelieRate)
	).apply(instance, MotorcycleSettings::new));
}
