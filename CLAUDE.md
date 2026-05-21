# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

In-progress single-player 3D RTS in Java, aimed at a Command & Conquer Generals feel. Empty entry point (`com.boot.Main`) hands off to `com.boot.core.Engine`, which owns the entire frame: window → input → camera → physics step → render → HUD → swap. No networking, no multi-scene support; everything currently runs in one game loop.

## Build & run

```
mvn -q -DskipTests compile   # incremental compile
mvn -q exec:java             # launch the game window (main class = com.boot.Main)
```

The app is a GUI window — `exec:java` blocks until the user closes it. There are no unit tests; verify changes by running the app and observing behavior (terrain renders, camera pans/zooms/rotates, hover coord shows in the debug overlay, HUD doesn't drop input gating).

Java target = **24** (release flag in `pom.xml`). LWJGL natives + PhysX natives are Windows-only in `pom.xml` (`natives-windows` classifiers); change those classifiers if moving to another OS.

## Architecture map

Packages under `com.boot.*`. Each class is small and single-purpose; the wiring is in `Engine.init()` and `Engine.loop()`.

- **`core/`** — `Engine` (game loop), `Window` (GLFW + GL 4.6 core ctx), `Input` (GLFW callbacks + per-frame ImGui input gating).
- **`world/`** — `Heightmap` (NxN fBm via `org.joml.SimplexNoise`, bilinear sampler), `RtsCamera` (target-relative state: `target/yaw/pitch/distance`, AoE-style angled, WASD + edge-pan + scroll-zoom + MMB-rotate; glues `target.y` to the heightmap).
- **`render/`** — `Shader` (classpath GLSL loader, uniform cache), `TerrainMesh` (interleaved `vec3 pos + vec3 color` VBO; **no normal attribute** — fragment shader reconstructs it via `cross(dFdx, dFdy)` for flat shading), `Renderer` (per-frame draw; hosts `pickTerrain` which unprojects the cursor to a world ray and delegates to PhysX).
- **`physics/`** — `PhysicsWorld` (PxFoundation/Physics/Scene + raycast wrapper), `TerrainCollider` (PxHeightField built from the same `Heightmap`, attached as a static actor). One physics step per frame.
- **`ui/`** — `Hud` (ImGui lifecycle + per-frame UI), `HudState` (cash/power, active tab, selection placeholder), `Minimap` (GL_RGBA8 texture sampled from heightmap colors), `UiLayout` (shared sizing constants used by both the HUD and the camera's edge-pan logic).

GLSL files live under `src/main/resources/shaders/` and target **`#version 460 core`**. Same version is passed to the ImGui GL3 backend.

## Conventions worth knowing before editing

- **No new Maven deps for current scope.** Terrain noise uses JOML's bundled `SimplexNoise`; PhysX, ImGui, LWJGL are already wired. Add deps only when truly new functionality demands it.
- **Coordinate system:** right-handed, Y-up. At `yaw=0` the camera looks in **+Z** direction. The forward vector used for WASD is `(sin(yaw), 0, cos(yaw))`; the right vector is `(-cos(yaw), 0, sin(yaw))`. Both were verified empirically — flipping a sign breaks the "W goes north" feel.
- **HUD chrome owns screen edges.** `RtsCamera.update()` reads `UiLayout.SIDEBAR_WIDTH` and `TOP_BAR_HEIGHT` to define its edge-pan bounds, and gates the whole input pipeline through `ImGuiIO.getWantCaptureMouse/Keyboard`. If you add a new HUD panel, update `UiLayout` so edge-pan still feels right.
- **Heightfield/mesh alignment:** PhysX sample at `(r, c)` maps to world `(r * quadSize, h * heightScale, c * quadSize)`. The mesh uses the same row-major layout — if you change one, change the other together or hover-pick will drift.
- **PhysX query filter trap (physx-jni 2.7.2):** `new PxQueryFilterData()` produces `flags = 0` and every raycast silently misses. Use the constructor that takes `PxQueryFlags` (with `eSTATIC | eDYNAMIC` raised). Hits also land in `nbTouches`, not `getBlock()`, unless a prefilter callback returns `eBLOCK` — `PhysicsWorld.raycast` already handles the touch-fallback. See `feedback-physx-query-filter` memory entry.
- **ImGui 1.92 atlas:** every frame must call `implGl3.newFrame()` **and** `implGlfw.newFrame()` before `ImGui.newFrame()`. Missing the first call asserts on frame 1.
- **No tests; trust the loop.** Verifying a change means running the app. Type-check first via `mvn compile`, then visually confirm.
