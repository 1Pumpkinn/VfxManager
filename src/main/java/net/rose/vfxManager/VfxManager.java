package net.rose.vfxManager;

import net.rose.vfxManager.vfx.VFXManager;
import net.rose.vfxManager.vfx.VFXQuality;
import org.bukkit.plugin.java.JavaPlugin;

public final class VfxManager extends JavaPlugin {
    private VFXManager vfxManager;

    @Override
    public void onEnable() {
        this.vfxManager = new VFXManager(this)
                .quality(VFXQuality.HIGH)
                .maxActiveEffects(512)
                .debugMode(false);
        this.vfxManager.start();
    }

    @Override
    public void onDisable() {
        if (this.vfxManager != null) {
            this.vfxManager.shutdown();
            this.vfxManager = null;
        }
    }

    public VFXManager vfx() {
        return this.vfxManager;
    }
}
