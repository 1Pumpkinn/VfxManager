# VfxManager

Reusable, high-performance VFX framework for Paper 26.2 plugins. Effects are written once against a small API and
share one scheduler, one set of math/shape utilities and one cleanup path.

## Architecture

```
vfx/
├── VFXManager        one central tick loop (1 BukkitTask), effect registry, limits, quality, debug, cleanup
├── VFXEffect         abstract base: age/duration, start/stop/tick/render, origin, entity attach, auto cleanup
├── VFXContext        per-tick render context (world, origin, quality, pre-filtered viewers)
├── VFXMath           shape generators + rotation/transform helpers (radians)
├── VFXRenderer       particle batches (DUST, DustTransition, count/offset/speed/randomised)
├── VFXDisplay        ItemDisplay / BlockDisplay / TextDisplay wrapper (translation, scale, rotation, interpolation)
├── VFXBeam           single ItemDisplay stretched between two points, updated in place
├── VFXQuality        LOW / MEDIUM / HIGH / ULTRA
├── VFXDebugRenderer  origin, axes, control points, bounding sphere
├── animation/        Easing, Timeline
├── shape/            VFXShape + Circle/Ellipse/Sphere/Hemisphere/Line/Arc/Spiral/Helix/Cone/Cylinder/Bezier/RandomCloud
└── effects/          MagicOrbEffect, MagicCircleEffect, BeamEffect, DisplayRingEffect
```

Key rules:

- Effects never create their own `BukkitRunnable`. `VFXManager` ticks every active effect from one loop.
- Shapes return `List<Vector>` and never spawn anything, so the same points work with particles or Displays.
- Effects register on `start()` and unregister themselves when finished.
- Displays are created once and updated in place. Displays registered with `trackDisplay(...)` are removed when the
  effect stops; the manager also removes leftovers on world unload and plugin disable.
- All Bukkit API access happens on the main thread.

## Quick start

```java
VFXManager vfx = new VFXManager(plugin).quality(VFXQuality.HIGH).maxActiveEffects(512);
vfx.start();            // onEnable
vfx.shutdown();         // onDisable
```

```java
vfx.magicCircle(location)
        .radius(5).rings(3).color(255, 40, 180).rotationSpeed(0.08).duration(60)
        .play();

vfx.beam(start, end).width(0.4).duration(20).play();

vfx.displayRing(location).radius(3).count(8).model(new ItemStack(Material.AMETHYST_SHARD)).duration(80).play();

MagicOrbEffect orb = new MagicOrbEffect(vfx, player.getLocation().add(0, 5, 0));
orb.setDuration(100);
orb.start();
```

Follow an entity:

```java
effect.attach(player);
effect.setOffset(new Vector(0, 3, 0));
```

Check for a refused start (effect limit reached): `effect.wasRejected()`.

## Writing a new effect

```java
public final class RingBurstEffect extends VFXEffect {
    private final List<Vector> base = VFXMath.circle(2, 40);

    public RingBurstEffect(VFXManager manager, Location origin) {
        super(manager, origin);
        setDuration(30);
    }

    @Override
    protected void onRender(VFXContext context) {
        double radius = Easing.EASE_OUT.apply(progress(), 0, 5);
        List<Vector> points = VFXMath.scale(base, radius);
        VFXRenderer.particles(points).color(Color.AQUA, 1.2f).render(context);
    }
}
```

No changes to `VFXManager` are needed; add a convenience method to it if you want a fluent entry point.

Hooks: `onStart()`, `onTick()` (logic), `onRender(context)` (drawing), `onStop()`. `timeline()` schedules events at
ticks (`at`, `every`, `repeatSection`). `progress()` is 0.0 to 1.0 from age and duration.

## Performance notes

- One scheduler task regardless of effect count; `maxActiveEffects` caps load (rejected starts are logged, throttled).
- `VFXQuality` scales point density, particle counts and optional secondary/tertiary effects.
- Viewers are resolved once per tick per effect, filtered by world and render distance; optional per-player viewers
  via `effect.viewers(players)`.
- Hot loops (`MagicCircleEffect`, `MagicOrbEffect` rings and core) rotate preallocated buffers in place.
  `VFXMath.rotateX/Y/Z` return new vectors; use the `...InPlace` variants in render loops.
- Particles are built from point lists; generate point lists once and reuse them where the shape is static.

## Debugging

`/vfx debug on` (or `vfxManager.debugMode(true)`) draws, per effect: origin, XYZ axes (red/green/blue), control
points (yellow, via `debugPoints()`), a three-ring bounding sphere, and beam endpoints.

## Test command

`/vfx <orb|circle|beam|displays|quality|debug|status>` (permission `vfx.admin`, ops by default).

## Build

Gradle with `paperweight-userdev` (`paperDevBundle("26.2.build.+")`), Java toolchain as set in `build.gradle.kts`.
`./gradlew build`, or `./gradlew runServer` to try it on a dev server.
