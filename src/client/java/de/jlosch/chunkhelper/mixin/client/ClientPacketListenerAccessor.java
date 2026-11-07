package de.jlosch.chunkhelper.mixin.client;

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The server's view and simulation distance, as sent to the client on join and whenever they change. */
@Mixin(ClientPacketListener.class)
public interface ClientPacketListenerAccessor {

    @Accessor("serverChunkRadius")
    int chunkhelper$getServerChunkRadius();

    @Accessor("serverSimulationDistance")
    int chunkhelper$getServerSimulationDistance();
}
