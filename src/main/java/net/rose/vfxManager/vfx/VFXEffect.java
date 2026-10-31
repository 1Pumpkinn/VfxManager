package net.rose.vfxManager.vfx;

import net.rose.vfxManager.vfx.animation.Timeline;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public abstract class VFXEffect {
    private final VFXManager manager;
    private final Location origin;
    private final List<Runnable> cleanupTasks = new ArrayList<>();
    private final List<Entity> trackedEntities = new ArrayList<>();
    private final Set<UUID> viewers = new LinkedHashSet<>();
    private final Timeline timeline = new Timeline();
    private Entity attachedEntity;
    private Vector offset = new Vector();
    private int age;
    private int duration = 60;
    private boolean running;
    private double renderDistance = 64.0D;
    private double boundingRadius = 4.0D;
    private VFXQuality qualityOverride;

    protected VFXEffect(final VFXManager manager, final Location origin) {
        this.manager = manager;
        this.origin = origin.clone();
    }

    public final VFXManager manager() {
        return this.manager;
    }

    public final Timeline timeline() {
        return this.timeline;
    }

    public final int age() {
        return this.age;
    }

    public final int duration() {
        return this.duration;
    }

    public final boolean isRunning() {
        return this.running;
    }

    public final VFXEffect setDuration(final int duration) {
        this.duration = Math.max(1, duration);
        return this;
    }

    public final VFXEffect setRenderDistance(final double renderDistance) {
        this.renderDistance = Math.max(1.0D, renderDistance);
        return this;
    }

    public final VFXEffect setOffset(final Vector offset) {
        this.offset = offset.clone();
        updateAttachmentOrigin();
        return this;
    }

    public final VFXEffect setQualityOverride(final VFXQuality qualityOverride) {
        this.qualityOverride = qualityOverride;
        return this;
    }

    public final VFXEffect setBoundingRadius(final double boundingRadius) {
        this.boundingRadius = Math.max(0.25D, boundingRadius);
        return this;
    }

    public final VFXEffect setOrigin(final Location location) {
        this.origin.setWorld(location.getWorld());
        this.origin.setX(location.getX());
        this.origin.setY(location.getY());
        this.origin.setZ(location.getZ());
        this.origin.setYaw(location.getYaw());
        this.origin.setPitch(location.getPitch());
        return this;
    }

    public final VFXEffect attach(final Entity entity) {
        this.attachedEntity = entity;
        updateAttachmentOrigin();
        return this;
    }

    public final VFXEffect detach() {
        this.attachedEntity = null;
        return this;
    }

    public final VFXEffect viewers(final Collection<? extends Player> players) {
        this.viewers.clear();
        for (final Player player : players) {
            this.viewers.add(player.getUniqueId());
        }
        return this;
    }

    public final VFXEffect clearViewers() {
        this.viewers.clear();
        return this;
    }

    public final VFXEffect start() {
        if (this.running) {
            return this;
        }

        this.age = 0;
        this.running = true;
        if (!this.manager.registerEffect(this)) {
            this.running = false;
            return this;
        }

        onStart();
        return this;
    }

    public final void stop() {
        if (!this.running) {
            cleanup();
            return;
        }

        this.running = false;
        try {
            onStop();
        } finally {
            cleanup();
            this.manager.unregisterEffect(this);
        }
    }

    public final void tick() {
        if (!this.running) {
            return;
        }

        if (!isEnvironmentValid()) {
            stop();
            return;
        }

        updateAttachmentOrigin();
        this.timeline.tick(this.age);
        onTick();
        render();
        this.age++;

        if (isFinished()) {
            stop();
        }
    }

    public final void render() {
        if (!this.running || this.origin.getWorld() == null) {
            return;
        }

        final VFXContext context = this.manager.createContext(this);
        onRender(context);
        if (context.debug()) {
            this.manager.debugRenderer().renderEffect(this, context);
        }
    }

    public boolean isFinished() {
        return this.age >= this.duration || !isEnvironmentValid();
    }

    public final double progress() {
        return Math.clamp((double) this.age / Math.max(1, this.duration), 0.0D, 1.0D);
    }

    public final Location origin() {
        return this.origin.clone();
    }

    public final Location originMutable() {
        return this.origin;
    }

    public final World world() {
        return this.origin.getWorld();
    }

    public final Entity attachedEntity() {
        return this.attachedEntity;
    }

    public final Vector offset() {
        return this.offset.clone();
    }

    public final double renderDistance() {
        return this.renderDistance;
    }

    public final double renderDistanceSquared() {
        return this.renderDistance * this.renderDistance;
    }

    public final double boundingRadius() {
        return this.boundingRadius;
    }

    public final VFXQuality quality() {
        return this.qualityOverride != null ? this.qualityOverride : this.manager.quality();
    }

    public final Set<UUID> viewerIds() {
        return Set.copyOf(this.viewers);
    }

    protected final void trackEntity(final Entity entity) {
        this.trackedEntities.add(entity);
        this.manager.trackManagedEntity(entity);
    }

    protected final void cleanupTask(final Runnable runnable) {
        this.cleanupTasks.add(runnable);
    }

    protected List<Vector> debugPoints() {
        return List.of();
    }

    protected void onStart() {
    }

    protected void onTick() {
    }

    protected void onRender(final VFXContext context) {
    }

    protected void onStop() {
    }

    private void cleanup() {
        for (final Runnable cleanupTask : this.cleanupTasks) {
            try {
                cleanupTask.run();
            } catch (final Exception ignored) {
            }
        }
        this.cleanupTasks.clear();

        for (final Entity trackedEntity : this.trackedEntities) {
            if (trackedEntity != null && trackedEntity.isValid()) {
                trackedEntity.remove();
            }
        }
        this.trackedEntities.clear();
    }

    private boolean isEnvironmentValid() {
        if (this.origin.getWorld() == null) {
            return false;
        }

        if (this.attachedEntity == null) {
            return true;
        }

        return this.attachedEntity.isValid()
                && !this.attachedEntity.isDead()
                && this.attachedEntity.getWorld().equals(this.origin.getWorld());
    }

    private void updateAttachmentOrigin() {
        if (this.attachedEntity == null || !this.attachedEntity.isValid()) {
            return;
        }

        final Location entityLocation = this.attachedEntity.getLocation();
        entityLocation.add(this.offset);
        setOrigin(entityLocation);
    }
}
