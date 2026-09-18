#!/usr/bin/env python3
"""Builds the OBJ models for the tier 1/2/3/5 sample vehicles.

The tier 4 sport bike is NOT generated here - it ships as an authored model. These four are
deliberately blocky stand-ins built to the same proportions as that one (same wheel radius, same
ground plane at y=0, same steering head around z=+0.8), so the pivots in each vehicle JSON line up
the same way across tiers and can be compared directly.

FACES REFERENCE POSITIONS ONLY ("f 1 2 3 4"). The base mod's own OBJ parser resolves a vt index by
indexing straight into its uv list, so naming one without emitting matching "vt" lines fails at
load; with position-only tokens it falls back to u=v=0 for every corner.
"""
import math
import pathlib

OUT_DIR = pathlib.Path(__file__).resolve().parents[1] / \
    "src/main/resources/assets/motorcycleaddon/models/obj"

WHEEL_R = 0.35


class Obj:
    def __init__(self, title):
        self.lines = [f"# {title} - motorcycleaddon sample", ""]
        self.n = 0

    def box(self, name, cx, cy, cz, sx, sy, sz):
        hx, hy, hz = sx / 2, sy / 2, sz / 2
        corners = [
            (cx - hx, cy - hy, cz - hz), (cx + hx, cy - hy, cz - hz),
            (cx + hx, cy + hy, cz - hz), (cx - hx, cy + hy, cz - hz),
            (cx - hx, cy - hy, cz + hz), (cx + hx, cy - hy, cz + hz),
            (cx + hx, cy + hy, cz + hz), (cx - hx, cy + hy, cz + hz),
        ]
        self.lines.append(f"g {name}")
        for x, y, z in corners:
            self.lines.append(f"v {x:.6f} {y:.6f} {z:.6f}")
        b = self.n
        for f in [(1, 2, 3, 4), (5, 8, 7, 6), (1, 5, 6, 2),
                  (2, 6, 7, 3), (3, 7, 8, 4), (4, 8, 5, 1)]:
            self.lines.append("f " + " ".join(str(b + i) for i in f))
        self.n += 8
        self.lines.append("")

    def disc(self, name, cx, cz, radius=WHEEL_R, width=0.14, segments=12, cy=None):
        """A wheel: a cylinder in the YZ plane spinning about X. cy defaults to radius, putting the
        wheel's own bottom exactly on the ground plane."""
        if cy is None:
            cy = radius
        self.lines.append(f"g {name}")
        ring = [(radius * math.cos(2 * math.pi * i / segments),
                 radius * math.sin(2 * math.pi * i / segments)) for i in range(segments)]
        for side in (cx - width / 2, cx + width / 2):
            for y, z in ring:
                self.lines.append(f"v {side:.6f} {cy + y:.6f} {cz + z:.6f}")
        b = self.n
        for i in range(segments):
            j = (i + 1) % segments
            a0, a1 = b + 1 + i, b + 1 + j
            c0, c1 = b + 1 + segments + i, b + 1 + segments + j
            self.lines.append(f"f {a0} {a1} {c1} {c0}")
        self.n += segments * 2
        self.lines.append("")

    def write(self, filename):
        OUT_DIR.mkdir(parents=True, exist_ok=True)
        path = OUT_DIR / filename
        path.write_text("\n".join(self.lines) + "\n", encoding="utf-8")
        print(f"wrote {path.name} ({self.n} vertices)")


# --- Tier 1: bicycle -------------------------------------------------------
# Exercises crawler_tracks (the chain) and track_roller_parts (the pedal crank), both borrowed from
# the base mod's own tank running gear. $chain rides the closed loop declared in the JSON; $crank
# turns with distance travelled like a road wheel does.
b = Obj("Bicycle (tier 1)")
b.box("frame", 0.0, 0.62, 0.0, 0.08, 0.30, 1.30)
b.box("seat", 0.0, 0.95, -0.45, 0.14, 0.08, 0.34)
b.box("handlebar", 0.0, 1.05, 0.62, 0.60, 0.05, 0.05)
b.box("fork", 0.0, 0.72, 0.70, 0.06, 0.70, 0.06)
b.disc("$chain", 0.10, -0.10, radius=0.22, width=0.03, segments=16)
b.disc("$crank", 0.12, -0.10, radius=0.16, width=0.05, segments=8)
b.disc("$wheel0", 0.0, 0.78)
b.disc("$wheel1", 0.0, -0.78)
b.write("bicycle.obj")

# --- Tier 2: scooter -------------------------------------------------------
# Step-through frame, upright riding position, small wheels. Leans only slightly.
s = Obj("Scooter (tier 2)")
s.box("body", 0.0, 0.42, -0.10, 0.34, 0.26, 1.10)
s.box("apron", 0.0, 0.80, 0.42, 0.34, 0.60, 0.12)
s.box("seat", 0.0, 0.70, -0.45, 0.30, 0.14, 0.50)
s.box("handlebar", 0.0, 1.16, 0.46, 0.56, 0.06, 0.06)
s.box("fork", 0.0, 0.62, 0.58, 0.08, 0.60, 0.08)
s.disc("$wheel0", 0.0, 0.62, radius=0.26)
s.disc("$wheel1", 0.0, -0.62, radius=0.26)
s.write("scooter.obj")

# --- Tier 3: sidecar -------------------------------------------------------
# Three wheels, so it never banks. The machine gun rides on the sidecar: $mg_mount is the weapon
# part the base mod's own turret handling aims.
c = Obj("Sidecar (tier 3)")
c.box("body", 0.0, 0.55, 0.0, 0.32, 0.36, 1.50)
c.box("tank", 0.0, 0.86, -0.10, 0.30, 0.24, 0.60)
c.box("handlebar", 0.0, 1.20, 0.70, 0.66, 0.07, 0.07)
c.box("fork", 0.0, 0.70, 0.80, 0.10, 0.62, 0.10)
c.box("car_body", 0.72, 0.52, -0.10, 0.62, 0.42, 1.10)
c.box("car_strut", 0.40, 0.52, -0.10, 0.36, 0.08, 0.08)
c.box("$mg_mount", 0.72, 0.92, 0.10, 0.10, 0.36, 0.10)
c.box("$mg_barrel", 0.72, 1.06, 0.45, 0.07, 0.07, 0.70)
c.disc("$wheel0", 0.0, 0.85)
c.disc("$wheel1", 0.0, -0.85)
c.disc("$wheel2", 0.72, -0.30)
c.write("sidecar.obj")

# --- Tier 5: hover bike ----------------------------------------------------
# No contact wheels at all - the "wheels" are ducted fans mounted flat, which the base mod's own
# wheel rotation still spins. Leans hardest of the five and does acrobatics.
h = Obj("Hover Bike (tier 5)")
h.box("body", 0.0, 0.70, 0.0, 0.36, 0.30, 1.80)
h.box("cowl", 0.0, 0.92, 0.55, 0.32, 0.26, 0.70)
h.box("seat", 0.0, 0.88, -0.45, 0.30, 0.14, 0.55)
h.box("handlebar", 0.0, 1.16, 0.72, 0.62, 0.07, 0.07)
h.box("fork", 0.0, 0.92, 0.82, 0.10, 0.40, 0.10)
h.box("duct_front", 0.0, 0.50, 0.90, 0.60, 0.16, 0.60)
h.box("duct_rear", 0.0, 0.50, -0.90, 0.60, 0.16, 0.60)
h.disc("$wheel0", 0.0, 0.90, radius=0.26, width=0.05, cy=0.50)
h.disc("$wheel1", 0.0, -0.90, radius=0.26, width=0.05, cy=0.50)
h.write("hover_bike.obj")
