package de.jlosch.chunkhelper.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;

public class ChunkhelperClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> ChunkHelperCommand.register(dispatcher));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> LoadPreview.clear());

        // Runs during level extraction while the per-frame gizmo collector is active.
        LevelExtractionEvents.AFTER_BLOCK_OUTLINE_EXTRACTION.register((context, hitResult) -> ChunkOutlineRenderer.emit(context));
    }
}
