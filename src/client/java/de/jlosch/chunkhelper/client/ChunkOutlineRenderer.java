package de.jlosch.chunkhelper.client;

import de.jlosch.chunkhelper.client.LoadPreview.Marker;
import de.jlosch.chunkhelper.client.LoadPreview.Zones;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the zones of the current {@link LoadPreview} marker as square walls on the chunk borders,
 * in the style of the F3+G chunk borders: translucent full-height fill, thick corner posts,
 * a thin post at every chunk border and horizontal lines every 16 blocks around the camera height.
 */
public final class ChunkOutlineRenderer {

    public static final int ENTITY_TICKING_COLOR = 0x3DDC84;
    public static final int BLOCK_TICKING_COLOR = 0xFFC233;
    public static final int LOADED_COLOR = 0x4DA3FF;

    private ChunkOutlineRenderer() {
    }

    public static void emit(LevelExtractionContext context) {
        Marker marker = LoadPreview.marker();
        ClientLevel level = context.level();
        if (marker == null || level == null || !level.dimension().equals(marker.dimension())) {
            return;
        }

        Vec3 camera = context.camera().position();
        double minY = level.getMinY();
        double maxY = level.getMaxY() + 1;
        double sectionY = Math.floor(camera.y / 16.0) * 16.0;
        double bandLow = Math.max(minY, sectionY - 48.0);
        double bandHigh = Math.min(maxY, sectionY + 64.0);

        Zones zones = LoadPreview.zones(marker.type());
        drawSquare(marker, zones.loaded(), LOADED_COLOR, minY, maxY, bandLow, bandHigh, sectionY);
        drawSquare(marker, zones.blockTicking(), BLOCK_TICKING_COLOR, minY, maxY, bandLow, bandHigh, sectionY);
        drawSquare(marker, zones.entityTicking(), ENTITY_TICKING_COLOR, minY, maxY, bandLow, bandHigh, sectionY);

        // beam + label on the exact point
        double x = marker.blockX() + 0.5;
        double z = marker.blockZ() + 0.5;
        int color = 0xFF000000 | marker.type().color;
        Gizmos.line(new Vec3(x, minY, z), new Vec3(x, maxY, z), color, 2.0F);
        double dx = x - camera.x;
        double dz = z - camera.z;
        float scale = (float) Mth.clamp(Math.sqrt(dx * dx + dz * dz) * 0.02, 0.32, 3.0);
        Gizmos.billboardText(marker.type().displayName + " " + marker.blockX() + " " + marker.blockZ(),
                        new Vec3(x, camera.y + 1.5, z), TextGizmo.Style.forColorAndCentered(color).withScale(scale))
                .setAlwaysOnTop();
    }

    private static void drawSquare(Marker marker, int radius, int rgb,
                                   double minY, double maxY, double bandLow, double bandHigh, double sectionY) {
        if (radius < 0) {
            return;
        }
        double x0 = (marker.chunkX() - radius) * 16.0;
        double z0 = (marker.chunkZ() - radius) * 16.0;
        double x1 = (marker.chunkX() + radius + 1) * 16.0;
        double z1 = (marker.chunkZ() + radius + 1) * 16.0;

        wall(x0, z0, x1, z0, rgb, minY, maxY, bandLow, bandHigh, sectionY);
        wall(x1, z0, x1, z1, rgb, minY, maxY, bandLow, bandHigh, sectionY);
        wall(x1, z1, x0, z1, rgb, minY, maxY, bandLow, bandHigh, sectionY);
        wall(x0, z1, x0, z0, rgb, minY, maxY, bandLow, bandHigh, sectionY);
    }

    private static void wall(double xa, double za, double xb, double zb, int rgb,
                             double minY, double maxY, double bandLow, double bandHigh, double sectionY) {
        int strong = argb(0xF0, rgb);
        int weak = argb(0x90, rgb);

        Gizmos.rect(new Vec3(xa, minY, za), new Vec3(xb, minY, zb), new Vec3(xb, maxY, zb), new Vec3(xa, maxY, za),
                GizmoStyle.fill(argb(0x22, rgb)));

        // corner post (the next wall draws the other corner)
        Gizmos.line(new Vec3(xa, minY, za), new Vec3(xa, maxY, za), strong, 3.0F);

        // a thin post on every chunk border along the wall, so the chunks can be counted
        double length = Math.max(Math.abs(xb - xa), Math.abs(zb - za));
        double stepX = Math.signum(xb - xa) * 16.0;
        double stepZ = Math.signum(zb - za) * 16.0;
        for (double d = 16.0; d < length; d += 16.0) {
            double x = xa + stepX * (d / 16.0);
            double z = za + stepZ * (d / 16.0);
            Gizmos.line(new Vec3(x, bandLow, z), new Vec3(x, bandHigh, z), weak, 1.0F);
        }

        // horizontal lines every 16 blocks around the camera
        for (double y = bandLow; y <= bandHigh; y += 16.0) {
            boolean atCamera = y == sectionY;
            Gizmos.line(new Vec3(xa, y, za), new Vec3(xb, y, zb), atCamera ? strong : weak, atCamera ? 2.5F : 1.5F);
        }
    }

    private static int argb(int alpha, int rgb) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }
}
