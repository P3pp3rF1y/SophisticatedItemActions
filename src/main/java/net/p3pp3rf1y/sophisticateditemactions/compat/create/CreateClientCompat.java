package net.p3pp3rf1y.sophisticateditemactions.compat.create;

import net.p3pp3rf1y.sophisticateditemactions.client.ClientCompatRenderHelper;

public class CreateClientCompat {
	private CreateClientCompat() {
	}

	public static void init() {
		ClientCompatRenderHelper.registerEntityBlockHighlightSink(RenderedEntityBlockHighlightRenderer::addHighlightedPositions);
		ClientCompatRenderHelper.registerRenderCallback(RenderedEntityBlockHighlightRenderer::render);
	}
}
