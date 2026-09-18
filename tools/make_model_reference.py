#!/usr/bin/env python3
"""Builds a simple motorcycle OBJ for the addon sample.

Deliberately blocky - the point is to exercise the base mod's own OBJ loading, part naming and
wheel animation, not to be a detailed model. Groups are emitted with "g", matching what the base
mod's own converter produces and what its loader expects for named parts.

Faces reference POSITIONS ONLY ("f 1 2 3 4", no "/vt"). The base mod's own parser resolves a vt
index by indexing straight into its uv list, so naming one without emitting matching "vt" lines
would fail at load; with position-only tokens it falls back to u=v=0 for every corner, which is
what this untextured sample wants anyway.
"""
import math
import pathlib

OUT = pathlib.Path(__file__).resolve().parents[1] / \
    "src/main/resources/assets/motorcycleaddon/models/obj/motorcycle.obj"

lines = ["# Simple motorcycle for the Tudur's Vehicle Mod addon sample", ""]
vertex_count = 0


def box(name, cx, cy, cz, sx, sy, sz):
    """One axis-aligned box as its own named group."""
    global vertex_count
    hx, hy, hz = sx / 2, sy / 2, sz / 2
    corners = [
        (cx - hx, cy - hy, cz - hz), (cx + hx, cy - hy, cz - hz),
        (cx + hx, cy + hy, cz - hz), (cx - hx, cy + hy, cz - hz),
        (cx - hx, cy - hy, cz + hz), (cx + hx, cy - hy, cz + hz),
        (cx + hx, cy + hy, cz + hz), (cx - hx, cy + hy, cz + hz),
    ]
    lines.append(f"g {name}")
    for x, y, z in corners:
        lines.append(f"v {x:.6f} {y:.6f} {z:.6f}")
    b = vertex_count
    faces = [
        (1, 2, 3, 4), (5, 8, 7, 6), (1, 5, 6, 2),
        (2, 6, 7, 3), (3, 7, 8, 4), (4, 8, 5, 1),
    ]
    for f in faces:
        lines.append("f " + " ".join(str(b + i) for i in f))
    vertex_count += 8
    lines.append("")


def wheel(name, cz, radius=0.35, width=0.14, segments=12):
    """A cylinder standing in the XY plane, spinning about X - matching the pivot the vehicle
    JSON's own wheel_parts declares for this part."""
    global vertex_count
    lines.append(f"g {name}")
    ring = []
    for i in range(segments):
        a = 2 * math.pi * i / segments
        ring.append((radius * math.cos(a), radius * math.sin(a)))
    for side in (-width / 2, width / 2):
        for y, z in ring:
            lines.append(f"v {side:.6f} {radius + y:.6f} {cz + z:.6f}")
    b = vertex_count
    for i in range(segments):
        j = (i + 1) % segments
        a0, a1 = b + 1 + i, b + 1 + j
        b0, b1 = b + 1 + segments + i, b + 1 + segments + j
        lines.append(f"f {a0} {a1} {b1} {b0}")
    vertex_count += segments * 2
    lines.append("")


# Body, low and narrow - a two-wheeler silhouette.
box("body", 0.0, 0.52, 0.0, 0.30, 0.34, 1.50)
# Fuel tank / seat hump.
box("tank", 0.0, 0.78, -0.10, 0.28, 0.22, 0.60)
# Handlebars.
box("handlebar", 0.0, 0.95, 0.70, 0.70, 0.07, 0.07)
# Front fork.
box("fork", 0.0, 0.62, 0.82, 0.10, 0.50, 0.10)

# The two wheels, named exactly as the vehicle JSON's own wheel_parts reference them.
wheel("$wheel0", 0.85)
wheel("$wheel1", -0.85)

OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
print(f"wrote {OUT} ({vertex_count} vertices)")
