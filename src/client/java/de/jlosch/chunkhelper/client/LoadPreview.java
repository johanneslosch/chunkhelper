package de.jlosch.chunkhelper.client;

import de.jlosch.chunkhelper.mixin.client.ClientPacketListenerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/**
 * The one fixed point set with {@code /ch <x> <z> pearl|player} and the chunk areas it keeps loaded.
 * <p>
 * Vanilla rules (26.x): a ticket gives its chunk a level, which rises by 1 per chunk (chessboard distance).
 * Level &le; 31 = entity ticking, 32 = block ticking, 33 = loaded but nothing ticks.
 * <ul>
 *   <li><b>Ender pearl:</b> one ticket with radius 2 (level 31) on the pearl's chunk -> 1x1 / 3x3 / 5x5.</li>
 *   <li><b>Player:</b> a loading ticket (level 31) on every chunk within the server view distance, plus a
 *   simulation ticket on the player's chunk at level 31 - simulation distance. Entities only tick where both
 *   reach level 31.</li>
 * </ul>
 */
public final class LoadPreview {

    private LoadPreview() {
    }

    public enum Type {
        PEARL("Ender pearl", 0xE040FB),
        PLAYER("Player", 0xFFFFFF);

        public final String displayName;
        public final int color;

        Type(String displayName, int color) {
            this.displayName = displayName;
            this.color = color;
        }
    }

    public record Marker(Type type, int blockX, int blockZ, ResourceKey<Level> dimension) {
        public int chunkX() {
            return this.blockX >> 4;
        }

        public int chunkZ() {
            return this.blockZ >> 4;
        }
    }

    /** Chessboard radius in chunks around the marker chunk for each zone (0 = only the marker chunk). */
    public record Zones(int entityTicking, int blockTicking, int loaded) {
    }

    private static volatile Marker marker;

    public static Marker marker() {
        return marker;
    }

    public static void set(Marker value) {
        marker = value;
    }

    public static void clear() {
        marker = null;
    }

    public static Zones zones(Type type) {
        return switch (type) {
            case PEARL -> new Zones(0, 1, 2);
            case PLAYER -> {
                int view = viewDistance();
                int simulation = Math.min(simulationDistance(), 31);
                yield new Zones(Math.min(view, simulation), Math.min(view, simulation) + 1, view + 2);
            }
        };
    }

    /** Server view distance (what the player loading tickets use), falling back to the client setting. */
    public static int viewDistance() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        int value = connection == null ? 0 : ((ClientPacketListenerAccessor) connection).chunkhelper$getServerChunkRadius();
        if (value <= 0) {
            value = minecraft.options.renderDistance().get();
        }
        return Mth.clamp(value, 2, 32);
    }

    /** Server simulation distance, falling back to the client setting. */
    public static int simulationDistance() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        int value = connection == null ? 0 : ((ClientPacketListenerAccessor) connection).chunkhelper$getServerSimulationDistance();
        if (value <= 0) {
            value = minecraft.options.simulationDistance().get();
        }
        return Math.max(0, value);
    }
}
