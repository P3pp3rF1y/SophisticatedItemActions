package net.p3pp3rf1y.sophisticateditemactions.init;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.p3pp3rf1y.sophisticateditemactions.SophisticatedItemActions;
import net.p3pp3rf1y.sophisticateditemactions.network.*;

public class ModPayloads {
	private ModPayloads() {
	}

	public static void registerPayloads(final RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar(SophisticatedItemActions.MOD_ID).versioned(SophisticatedItemActions.getNetworkProtocolVersion());
		registrar.playToServer(DepositItemsPayload.TYPE, DepositItemsPayload.STREAM_CODEC, DepositItemsPayload::handlePayload);
		registrar.playToServer(RestockItemsPayload.TYPE, RestockItemsPayload.STREAM_CODEC, RestockItemsPayload::handlePayload);
		registrar.playToServer(RestockAlternativeItemsPayload.TYPE, RestockAlternativeItemsPayload.STREAM_CODEC, RestockAlternativeItemsPayload::handlePayload);
		registrar.playToServer(RestockRecipeItemsPayload.TYPE, RestockRecipeItemsPayload.STREAM_CODEC, RestockRecipeItemsPayload::handlePayload);
		registrar.playToServer(RestockRegisteredRecipePayload.TYPE, RestockRegisteredRecipePayload.STREAM_CODEC, RestockRegisteredRecipePayload::handlePayload);
		registrar.playToClient(SyncItemTransfersPayload.TYPE, SyncItemTransfersPayload.STREAM_CODEC, SyncItemTransfersPayload::handlePayload);
		registrar.playToServer(RequestItemHighlightsPayload.TYPE, RequestItemHighlightsPayload.STREAM_CODEC, RequestItemHighlightsPayload::handlePayload);
		registrar.playToClient(SyncRenderedBlockHighlightsPayload.TYPE, SyncRenderedBlockHighlightsPayload.STREAM_CODEC,
				SyncRenderedBlockHighlightsPayload::handlePayload);
		registrar.playToClient(SyncRenderedEntityBlockHighlightsPayload.TYPE, SyncRenderedEntityBlockHighlightsPayload.STREAM_CODEC,
				SyncRenderedEntityBlockHighlightsPayload::handlePayload);
		registrar.playToClient(SyncEntityHighlightsPayload.TYPE, SyncEntityHighlightsPayload.STREAM_CODEC, SyncEntityHighlightsPayload::handlePayload);
		registrar.playToClient(SyncHighlightDirectionsPayload.TYPE, SyncHighlightDirectionsPayload.STREAM_CODEC, SyncHighlightDirectionsPayload::handlePayload);
	}
}
