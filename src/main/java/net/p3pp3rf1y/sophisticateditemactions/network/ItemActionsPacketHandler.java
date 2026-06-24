package net.p3pp3rf1y.sophisticateditemactions.network;

import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;
import net.p3pp3rf1y.sophisticateditemactions.SophisticatedItemActions;

public class ItemActionsPacketHandler extends PacketHandler {
	public static final ItemActionsPacketHandler INSTANCE = new ItemActionsPacketHandler();

	protected ItemActionsPacketHandler() {
		super(SophisticatedItemActions.MOD_ID, SophisticatedItemActions.getNetworkProtocolVersion());
	}

	@Override
	public void registerMessages() {
		registerMessage(DepositItemsMessage.class, DepositItemsMessage::encode, DepositItemsMessage::decode, DepositItemsMessage::onMessage);
		registerMessage(RequestItemHighlightsMessage.class, RequestItemHighlightsMessage::encode, RequestItemHighlightsMessage::decode,
				RequestItemHighlightsMessage::onMessage);
		registerMessage(RestockItemsMessage.class, RestockItemsMessage::encode, RestockItemsMessage::decode, RestockItemsMessage::onMessage);
		registerMessage(SyncEntityHighlightsMessage.class, SyncEntityHighlightsMessage::encode, SyncEntityHighlightsMessage::decode,
				SyncEntityHighlightsMessage::onMessage);
		registerMessage(SyncHighlightDirectionsMessage.class, SyncHighlightDirectionsMessage::encode, SyncHighlightDirectionsMessage::decode,
				SyncHighlightDirectionsMessage::onMessage);
		registerMessage(SyncItemTransfersMessage.class, SyncItemTransfersMessage::encode, SyncItemTransfersMessage::decode,
				SyncItemTransfersMessage::onMessage);
		registerMessage(SyncRenderedEntityBlockHighlightsMessage.class, SyncRenderedEntityBlockHighlightsMessage::encode,
				SyncRenderedEntityBlockHighlightsMessage::decode, SyncRenderedEntityBlockHighlightsMessage::onMessage);
	}
}
