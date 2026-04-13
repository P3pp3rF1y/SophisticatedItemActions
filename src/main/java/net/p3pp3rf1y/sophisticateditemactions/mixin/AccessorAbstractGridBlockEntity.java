package net.p3pp3rf1y.sophisticateditemactions.mixin;

import com.refinedmods.refinedstorage.api.network.node.NetworkNode;
import com.refinedmods.refinedstorage.common.api.support.network.AbstractNetworkNodeContainerBlockEntity;
import com.refinedmods.refinedstorage.common.grid.AbstractGridBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractNetworkNodeContainerBlockEntity.class)
public interface AccessorAbstractGridBlockEntity {
	@Accessor(value = "mainNetworkNode", remap = false)
	NetworkNode sophisticatedItemActions$getMainNetworkNode();
}
