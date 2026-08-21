package net.p3pp3rf1y.sophisticateditemactions.network;

import net.minecraftforge.network.NetworkDirection;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;
import net.p3pp3rf1y.sophisticateditemactions.SophisticatedItemActions;

public class ItemActionsPacketHandler extends PacketHandler {
	public static final ItemActionsPacketHandler INSTANCE = new ItemActionsPacketHandler();

	protected ItemActionsPacketHandler() {
		super(SophisticatedItemActions.MOD_ID, SophisticatedItemActions.getNetworkProtocolVersion());
	}

	@Override
	public void registerMessages() {
		registerMessage(DepositItemsMessage.class, DepositItemsMessage::encode, DepositItemsMessage::decode, DepositItemsMessage::onMessage,
				NetworkDirection.PLAY_TO_SERVER);
		registerMessage(RequestItemHighlightsMessage.class, RequestItemHighlightsMessage::encode, RequestItemHighlightsMessage::decode,
				RequestItemHighlightsMessage::onMessage, NetworkDirection.PLAY_TO_SERVER);
		registerMessage(RestockItemsMessage.class, RestockItemsMessage::encode, RestockItemsMessage::decode, RestockItemsMessage::onMessage,
				NetworkDirection.PLAY_TO_SERVER);
		registerMessage(RestockAlternativeItemsMessage.class, RestockAlternativeItemsMessage::encode, RestockAlternativeItemsMessage::decode,
				RestockAlternativeItemsMessage::onMessage, NetworkDirection.PLAY_TO_SERVER);
		registerMessage(RestockRecipeItemsMessage.class, RestockRecipeItemsMessage::encode, RestockRecipeItemsMessage::decode,
				RestockRecipeItemsMessage::onMessage, NetworkDirection.PLAY_TO_SERVER);
		registerMessage(RestockRegisteredRecipeMessage.class, RestockRegisteredRecipeMessage::encode, RestockRegisteredRecipeMessage::decode,
				RestockRegisteredRecipeMessage::onMessage, NetworkDirection.PLAY_TO_SERVER);
		registerMessage(SyncEntityHighlightsMessage.class, SyncEntityHighlightsMessage::encode, SyncEntityHighlightsMessage::decode,
				SyncEntityHighlightsMessage::onMessage, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(SyncHighlightDirectionsMessage.class, SyncHighlightDirectionsMessage::encode, SyncHighlightDirectionsMessage::decode,
				SyncHighlightDirectionsMessage::onMessage, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(SyncItemTransfersMessage.class, SyncItemTransfersMessage::encode, SyncItemTransfersMessage::decode, SyncItemTransfersMessage::onMessage,
				NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(SyncRenderedEntityBlockHighlightsMessage.class, SyncRenderedEntityBlockHighlightsMessage::encode,
				SyncRenderedEntityBlockHighlightsMessage::decode, SyncRenderedEntityBlockHighlightsMessage::onMessage, NetworkDirection.PLAY_TO_CLIENT);
	}
}
