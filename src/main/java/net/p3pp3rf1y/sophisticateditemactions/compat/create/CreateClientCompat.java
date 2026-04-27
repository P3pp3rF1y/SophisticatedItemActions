package net.p3pp3rf1y.sophisticateditemactions.compat.create;

import net.p3pp3rf1y.sophisticateditemactions.client.ClientCompatRenderHelper;

public class CreateClientCompat {
	private CreateClientCompat() {}

	public static void init() {
		ClientCompatRenderHelper.registerEntityBlockHighlightRenderer(RenderedEntityBlockHighlightRenderer::addHighlightedPositions);
		ClientCompatRenderHelper.registerCustomGeometryRenderer(RenderedEntityBlockHighlightRenderer::render);
	}
}
