package de.jlosch.chunkhelper.client;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import de.jlosch.chunkhelper.client.LoadPreview.Marker;
import de.jlosch.chunkhelper.client.LoadPreview.Type;
import de.jlosch.chunkhelper.client.LoadPreview.Zones;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

/**
 * <pre>
 * /ch &lt;x&gt; &lt;z&gt; pearl    chunks an ender pearl at that position keeps loaded
 * /ch &lt;x&gt; &lt;z&gt; player   chunks a player at that position keeps loaded
 * /ch clear              remove the outline
 * </pre>
 * {@code /chunkhelper} works the same. Coordinates are block coordinates, {@code ~} is relative to you.
 */
public final class ChunkHelperCommand {

    private ChunkHelperCommand() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(build("ch"));
        dispatcher.register(build("chunkhelper"));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> build(String name) {
        return literal(name)
                .executes(context -> usage(context.getSource()))
                .then(literal("clear").executes(context -> clear(context.getSource())))
                .then(argument("pos", ColumnPosArgument.columnPos())
                        .then(literal("pearl").executes(context -> show(context, Type.PEARL)))
                        .then(literal("player").executes(context -> show(context, Type.PLAYER))));
    }

    private static int show(CommandContext<FabricClientCommandSource> context, Type type) {
        FabricClientCommandSource source = context.getSource();
        Coordinates coordinates = context.getArgument("pos", Coordinates.class);
        if (!(coordinates instanceof WorldCoordinates world)) {
            source.sendError(prefix().append("Use absolute or ~ coordinates."));
            return 0;
        }

        int x = Mth.floor(world.x().get(source.getPlayer().getX()));
        int z = Mth.floor(world.z().get(source.getPlayer().getZ()));
        Marker marker = new Marker(type, x, z, source.getLevel().dimension());
        LoadPreview.set(marker);

        Zones zones = LoadPreview.zones(type);
        MutableComponent message = prefix()
                .append(Component.literal(type.displayName).withColor(type.color))
                .append(Component.literal(" at " + x + " " + z + " (chunk " + marker.chunkX() + " " + marker.chunkZ() + ")")
                        .withStyle(ChatFormatting.GRAY));
        if (type == Type.PLAYER) {
            message.append(Component.literal(", view distance " + LoadPreview.viewDistance()
                    + ", simulation distance " + LoadPreview.simulationDistance()).withStyle(ChatFormatting.GRAY));
        }
        source.sendFeedback(message);
        source.sendFeedback(Component.literal("  ")
                .append(zone("entity ticking", zones.entityTicking(), ChunkOutlineRenderer.ENTITY_TICKING_COLOR))
                .append(zone("block ticking", zones.blockTicking(), ChunkOutlineRenderer.BLOCK_TICKING_COLOR))
                .append(zone("loaded", zones.loaded(), ChunkOutlineRenderer.LOADED_COLOR)));
        return Command.SINGLE_SUCCESS;
    }

    private static int clear(FabricClientCommandSource source) {
        LoadPreview.clear();
        source.sendFeedback(prefix().append(Component.literal("Outline removed.").withStyle(ChatFormatting.GRAY)));
        return Command.SINGLE_SUCCESS;
    }

    private static int usage(FabricClientCommandSource source) {
        source.sendFeedback(prefix().append(Component.literal("/ch <x> <z> pearl|player").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" shows which chunks that loads, ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("/ch clear").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" removes it.").withStyle(ChatFormatting.GRAY)));
        return Command.SINGLE_SUCCESS;
    }

    private static MutableComponent zone(String name, int radius, int color) {
        int size = 2 * radius + 1;
        return Component.literal("■ ").withColor(color)
                .append(Component.literal(name + " " + size + "x" + size + "   ").withStyle(ChatFormatting.WHITE));
    }

    private static MutableComponent prefix() {
        return Component.literal("[ChunkHelper] ").withStyle(ChatFormatting.DARK_AQUA);
    }
}
