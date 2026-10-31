package net.rose.vfxManager.vfx;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class VFXDisplay<T extends Display> {
    private final T display;
    private final Vector3f translation = new Vector3f();
    private final Vector3f scale = new Vector3f(1.0F, 1.0F, 1.0F);
    private Quaternionf leftRotation = new Quaternionf();
    private Quaternionf rightRotation = new Quaternionf();
    private Display.Brightness brightness;
    private Display.Billboard billboard = Display.Billboard.FIXED;
    private int interpolationDuration;
    private int interpolationDelay;
    private int teleportDuration;

    private VFXDisplay(final T display) {
        this.display = display;
    }

    public static VFXDisplay<ItemDisplay> item(
            final VFXManager manager,
            final Location location,
            final ItemStack itemStack
    ) {
        final ItemDisplay display = location.getWorld().spawn(location, ItemDisplay.class, entity -> {
            entity.setItemStack(itemStack);
            entity.setPersistent(false);
        });
        manager.trackManagedEntity(display);
        return new VFXDisplay<>(display);
    }

    public static VFXDisplay<ItemDisplay> item(final Location location, final ItemStack itemStack) {
        final ItemDisplay display = location.getWorld().spawn(location, ItemDisplay.class, entity -> {
            entity.setItemStack(itemStack);
            entity.setPersistent(false);
        });
        return new VFXDisplay<>(display);
    }

    public static VFXDisplay<BlockDisplay> block(
            final VFXManager manager,
            final Location location,
            final BlockData blockData
    ) {
        final BlockDisplay display = location.getWorld().spawn(location, BlockDisplay.class, entity -> {
            entity.setBlock(blockData);
            entity.setPersistent(false);
        });
        manager.trackManagedEntity(display);
        return new VFXDisplay<>(display);
    }

    public static VFXDisplay<TextDisplay> text(
            final VFXManager manager,
            final Location location,
            final String text
    ) {
        final TextDisplay display = location.getWorld().spawn(location, TextDisplay.class, entity -> {
            entity.setText(text);
            entity.setPersistent(false);
        });
        manager.trackManagedEntity(display);
        return new VFXDisplay<>(display);
    }

    public static ItemStack modelItem(final Material material, final Integer customModelData) {
        final ItemStack itemStack = new ItemStack(material);
        if (customModelData != null) {
            itemStack.editMeta(meta -> meta.setCustomModelData(customModelData));
        }
        return itemStack;
    }

    public T entity() {
        return this.display;
    }

    public VFXDisplay<T> translation(final float x, final float y, final float z) {
        this.translation.set(x, y, z);
        return this;
    }

    public VFXDisplay<T> scale(final float x, final float y, final float z) {
        this.scale.set(x, y, z);
        return this;
    }

    public VFXDisplay<T> rotation(final Quaternionf quaternionf) {
        this.leftRotation = new Quaternionf(quaternionf);
        return this;
    }

    public VFXDisplay<T> rotation(final AxisAngle4f axisAngle) {
        this.leftRotation = new Quaternionf(axisAngle);
        return this;
    }

    public VFXDisplay<T> rightRotation(final Quaternionf quaternionf) {
        this.rightRotation = new Quaternionf(quaternionf);
        return this;
    }

    public VFXDisplay<T> brightness(final int blockLight, final int skyLight) {
        this.brightness = new Display.Brightness(blockLight, skyLight);
        return this;
    }

    public VFXDisplay<T> interpolation(final int delay, final int duration) {
        this.interpolationDelay = Math.max(0, delay);
        this.interpolationDuration = Math.max(0, duration);
        return this;
    }

    public VFXDisplay<T> billboard(final Display.Billboard billboard) {
        this.billboard = billboard;
        return this;
    }

    public VFXDisplay<T> teleportDuration(final int teleportDuration) {
        this.teleportDuration = Math.max(0, teleportDuration);
        return this;
    }

    public VFXDisplay<T> itemStack(final ItemStack itemStack) {
        if (this.display instanceof ItemDisplay itemDisplay) {
            itemDisplay.setItemStack(itemStack);
        }
        return this;
    }

    public VFXDisplay<T> blockData(final BlockData blockData) {
        if (this.display instanceof BlockDisplay blockDisplay) {
            blockDisplay.setBlock(blockData);
        }
        return this;
    }

    public VFXDisplay<T> text(final String text) {
        if (this.display instanceof TextDisplay textDisplay) {
            textDisplay.setText(text);
        }
        return this;
    }

    public VFXDisplay<T> apply() {
        if (!this.display.isValid()) {
            return this;
        }

        this.display.setTransformation(new Transformation(
                new Vector3f(this.translation),
                new Quaternionf(this.leftRotation),
                new Vector3f(this.scale),
                new Quaternionf(this.rightRotation)
        ));
        this.display.setBillboard(this.billboard);
        this.display.setInterpolationDelay(this.interpolationDelay);
        this.display.setInterpolationDuration(this.interpolationDuration);
        this.display.setTeleportDuration(this.teleportDuration);
        if (this.brightness != null) {
            this.display.setBrightness(this.brightness);
        }
        return this;
    }

    public VFXDisplay<T> teleport(final Location location) {
        if (this.display.isValid()) {
            this.display.teleport(location);
        }
        return this;
    }

    public void remove() {
        if (this.display.isValid()) {
            this.display.remove();
        }
    }
}
