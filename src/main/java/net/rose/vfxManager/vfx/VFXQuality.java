package net.rose.vfxManager.vfx;

public enum VFXQuality {
    LOW(0.45D, 0.55D, false, false),
    MEDIUM(0.7D, 0.8D, true, false),
    HIGH(1.0D, 1.0D, true, true),
    ULTRA(1.35D, 1.3D, true, true);

    private final double pointMultiplier;
    private final double particleMultiplier;
    private final boolean secondaryEffects;
    private final boolean tertiaryEffects;

    VFXQuality(
            final double pointMultiplier,
            final double particleMultiplier,
            final boolean secondaryEffects,
            final boolean tertiaryEffects
    ) {
        this.pointMultiplier = pointMultiplier;
        this.particleMultiplier = particleMultiplier;
        this.secondaryEffects = secondaryEffects;
        this.tertiaryEffects = tertiaryEffects;
    }

    public double pointMultiplier() {
        return this.pointMultiplier;
    }

    public double particleMultiplier() {
        return this.particleMultiplier;
    }

    public boolean secondaryEffects() {
        return this.secondaryEffects;
    }

    public boolean tertiaryEffects() {
        return this.tertiaryEffects;
    }

    public int scalePoints(final int basePoints) {
        return Math.max(4, (int) Math.round(basePoints * this.pointMultiplier));
    }

    public int scaleParticles(final int baseParticles) {
        return Math.max(1, (int) Math.round(baseParticles * this.particleMultiplier));
    }
}
