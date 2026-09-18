#!/usr/bin/env python3
"""Writes the five tier vehicle JSONs.

One script rather than five hand-edited files: the tiers share a lot of structure (steering pivots
on the centreline, the same "motorcycle" settings block, a single driver seat), and keeping that in
one place makes the per-tier DIFFERENCES the readable part.

Every file carries both halves of a vehicle's settings:
  - the base mod's own keys, parsed by VehicleDefinition;
  - this addon's own "motorcycle" object, parsed by MotorcycleSettings.
The base mod ignores keys it doesn't know, so the two coexist in one file per vehicle.
"""
import collections
import json
import pathlib

OUT = pathlib.Path(__file__).resolve().parents[1] / \
    "src/main/resources/data/motorcycleaddon/vehicles"

OD = collections.OrderedDict


def steering(part, y, z, max_angle=35.0):
    """A part that swings about the vertical axis through the steering head."""
    return OD([
        ("part", part),
        ("pivot_x", 0.0), ("pivot_y", y), ("pivot_z", z),
        ("axis_x", 0.0), ("axis_y", 1.0), ("axis_z", 0.0),
        ("max_angle", max_angle),
    ])


def wheel(part, y, z, x=0.0, steer=None):
    """A rolling wheel. Passing steer makes it ALSO swing with the steering, pivoting about the
    steering head rather than its own axle."""
    w = OD([("part", part), ("pivot_x", x), ("pivot_y", y), ("pivot_z", z)])
    if steer is not None:
        w["steer_angle"] = steer
        w["steer_axis_x"] = 0.0
        w["steer_axis_y"] = 1.0
        w["steer_axis_z"] = 0.0
        w["steer_pivot_x"] = x
        w["steer_pivot_y"] = y
        w["steer_pivot_z"] = z
    return w


def seat(name, y, z, x=0.0, driver=False):
    s = OD([("name", name), ("offset_x", x), ("offset_y", y), ("offset_z", z)])
    if driver:
        s["driver"] = True
    return s


def bike(name, display, tier, model, *, width, height, max_speed, acceleration,
         turn_speed, step_height, reverse_throttle, max_health, max_fuel,
         seats, wheels, steering_parts, settings, extra=None, fuel_consumption=None):
    d = OD([
        ("entity_type", "motorcycleaddon:motorcycle"),
        ("model", f"motorcycleaddon:models/obj/{model}.obj"),
        ("texture", "motorcycleaddon:textures/vehicle/motorcycle.png"),
        ("display_name", display),
        ("scale", 1.0),
        ("width", width),
        ("height", height),
        ("max_speed", max_speed),
        ("acceleration", acceleration),
        ("turn_speed", turn_speed),
        ("step_height", step_height),
        ("gravity", -0.04),
        ("reverse_throttle", reverse_throttle),
        ("weight_type", "car"),
        ("engine_sound_volume", 3.0),
        ("max_health", max_health),
        ("armor_damage_factor", 1.0),
        ("damage_factor", 1.0),
        ("max_fuel", max_fuel),
        *([("fuel_consumption", fuel_consumption)] if fuel_consumption is not None else []),
        ("inventory_size", 9),
        ("spawn_item", OD([("display_name", display), ("tier", tier)])),
        ("seats", seats),
        ("wheel_parts", wheels),
    ])
    if steering_parts:
        d["steering_wheel_parts"] = steering_parts
    if extra:
        d.update(extra)
    d["motorcycle"] = settings
    return name, d


def settings(max_lean, *, smoothing=0.15, full_lean_speed=0.35,
             upright_penalty=0.6, acrobatics=False, wheelie_angle=35.0, wheelie_rate=0.08):
    return OD([
        ("max_lean_degrees", max_lean),
        ("lean_smoothing", smoothing),
        ("full_lean_speed", full_lean_speed),
        ("upright_turn_penalty", upright_penalty),
        ("enable_acrobatics", acrobatics),
        ("wheelie_angle_degrees", wheelie_angle),
        ("wheelie_rate", wheelie_rate),
    ])


vehicles = []

# --- Tier 1: bicycle ------------------------------------------------------
# Slow, no fuel worth speaking of, but nimble. The chain is a crawler_track riding a closed loop,
# and the pedal crank is a track_roller - both borrowed from the base mod's own tank running gear,
# which is the point of the exercise.
vehicles.append(bike(
    "bicycle", "Bicycle", 1, "bicycle",
    width=0.6, height=1.1,
    max_speed=0.6, acceleration=0.04, turn_speed=4.5,
    step_height=0.5, reverse_throttle=0.3,
    max_health=10.0, max_fuel=0.0, fuel_consumption=-1.0,
    seats=[seat("rider", 0.95, -0.45, driver=True)],
    wheels=[wheel("$wheel0", 0.35, 0.78, steer=32.0), wheel("$wheel1", 0.35, -0.78)],
    steering_parts=[steering("handlebar", 1.05, 0.62, 32.0), steering("fork", 0.72, 0.70, 32.0)],
    settings=settings(45.0, smoothing=0.12, full_lean_speed=0.18, upright_penalty=0.7),
    extra=OD([
        # The chain. "path" is a closed Y/Z loop the track links ride around; x places it off the
        # centreline on the drive side.
        ("crawler_tracks", [OD([
            ("part", "$chain"),
            ("x", 0.10),
            ("link_spacing", 0.12),
            ("path", [
                OD([("y", 0.46), ("z", 0.12)]),
                OD([("y", 0.46), ("z", -0.32)]),
                OD([("y", 0.24), ("z", -0.32)]),
                OD([("y", 0.24), ("z", 0.12)]),
            ]),
        ])]),
        # The pedal crank, turning with distance travelled exactly as a road wheel does.
        ("track_roller_parts", [OD([
            ("part", "$crank"),
            ("pivot_x", 0.12), ("pivot_y", 0.35), ("pivot_z", -0.10),
            ("rotations_per_block", 0.45),
        ])]),
    ]),
))

# --- Tier 2: scooter ------------------------------------------------------
# Upright riding position, small wheels, and deliberately LITTLE lean - a scooter doesn't drop into
# a corner the way a sports bike does.
vehicles.append(bike(
    "scooter", "Scooter", 2, "scooter",
    width=0.7, height=1.2,
    max_speed=1.2, acceleration=0.06, turn_speed=4.0,
    step_height=0.5, reverse_throttle=0.3,
    max_health=16.0, max_fuel=400.0,
    seats=[seat("rider", 0.78, -0.45, driver=True), seat("pillion", 0.78, -0.75)],
    wheels=[wheel("$wheel0", 0.26, 0.62, steer=30.0), wheel("$wheel1", 0.26, -0.62)],
    steering_parts=[steering("handlebar", 1.16, 0.46, 30.0), steering("fork", 0.62, 0.58, 30.0)],
    settings=settings(18.0, smoothing=0.18, full_lean_speed=0.25, upright_penalty=0.8),
))

# --- Tier 3: sidecar ------------------------------------------------------
# Three wheels, so it never banks: max_lean_degrees is 0 and the cornering mode only affects the
# turn-rate penalty. Carries a gunner with a machine gun on the sidecar.
vehicles.append(bike(
    "sidecar", "Sidecar", 3, "sidecar",
    width=1.6, height=1.3,
    max_speed=1.1, acceleration=0.05, turn_speed=2.6,
    step_height=0.6, reverse_throttle=0.35,
    max_health=30.0, max_fuel=700.0,
    seats=[seat("rider", 0.75, -0.10, driver=True), seat("gunner", 0.66, -0.10, x=0.72)],
    wheels=[
        wheel("$wheel0", 0.35, 0.85, steer=28.0),
        wheel("$wheel1", 0.35, -0.85),
        wheel("$wheel2", 0.35, -0.30, x=0.72),
    ],
    steering_parts=[steering("handlebar", 1.20, 0.70, 28.0), steering("fork", 0.70, 0.80, 28.0)],
    # A three-wheeler stays flat. Upright is its only real mode, so the penalty is mild.
    settings=settings(0.0, upright_penalty=0.9),
    extra=OD([
        # The gun itself, operated from seat 1 (the gunner). weapon_name refers to this addon's own
        # bundled weapons/sidecar_mg.txt by file name.
        ("weapons", [OD([
            ("seat_index", 1),
            ("weapon_name", "sidecar_mg"),
            ("projectile_item", "minecraft:iron_nugget"),
            ("offsets", [OD([
                ("x", 0.72), ("y", 1.06), ("z", 0.80),
                ("mount_yaw", 0.0), ("mount_pitch", 0.0),
            ])]),
            ("turret_rotation_speed", 6.0),
        ])]),
        # The visible parts that swing with the gunner's aim.
        ("weapon_parts", [
            OD([
                ("part", "$mg_mount"),
                ("seat_index", 1),
                ("yaw_follow", True), ("pitch_follow", False),
                ("pivot_x", 0.72), ("pivot_y", 0.92), ("pivot_z", 0.10),
            ]),
            OD([
                ("part", "$mg_barrel"),
                ("seat_index", 1),
                ("yaw_follow", True), ("pitch_follow", True),
                ("pivot_x", 0.72), ("pivot_y", 1.06), ("pivot_z", 0.10),
                ("recoil_distance", 0.08),
            ]),
        ]),
    ]),
))

# --- Tier 4: sport bike ---------------------------------------------------
# The authored model. Leans hard (60 degrees, as configured) and can wheelie.
vehicles.append(bike(
    "sport_bike", "Sport Bike", 4, "sport_bike",
    width=0.8, height=1.2,
    max_speed=2.0, acceleration=0.09, turn_speed=5.5,
    step_height=0.6, reverse_throttle=0.35,
    max_health=20.0, max_fuel=600.0,
    seats=[seat("rider", 0.35, -0.10, driver=True)],
    wheels=[wheel("$wheel0", 0.35, 0.85, steer=35.0), wheel("$wheel1", 0.35, -0.85)],
    steering_parts=[steering("handlebar", 1.55, 0.70), steering("fork", 0.95, 0.78)],
    settings=settings(60.0, acrobatics=True, wheelie_angle=40.0),
))

# --- Tier 5: hover bike ---------------------------------------------------
# Ducted fans instead of contact wheels - the "wheels" are the fan discs, which the base mod's own
# wheel rotation spins regardless of whether they touch anything. Fastest, leans hardest.
vehicles.append(bike(
    "hover_bike", "Hover Bike", 5, "hover_bike",
    width=0.9, height=1.3,
    max_speed=2.6, acceleration=0.11, turn_speed=6.5,
    step_height=1.0, reverse_throttle=0.4,
    max_health=24.0, max_fuel=900.0,
    seats=[seat("rider", 0.95, -0.45, driver=True)],
    wheels=[wheel("$wheel0", 0.50, 0.90, steer=25.0), wheel("$wheel1", 0.50, -0.90)],
    steering_parts=[steering("handlebar", 1.16, 0.72, 25.0), steering("fork", 0.92, 0.82, 25.0)],
    settings=settings(70.0, smoothing=0.12, full_lean_speed=0.45,
                      upright_penalty=0.5, acrobatics=True,
                      wheelie_angle=45.0, wheelie_rate=0.06),
))

OUT.mkdir(parents=True, exist_ok=True)
for name, data in vehicles:
    path = OUT / f"{name}.json"
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"wrote {path.name}  (tier {data['spawn_item']['tier']}, "
          f"lean {data['motorcycle']['max_lean_degrees']} deg)")
