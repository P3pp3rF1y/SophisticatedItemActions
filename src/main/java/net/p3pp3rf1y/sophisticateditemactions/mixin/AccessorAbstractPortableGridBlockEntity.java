package net.p3pp3rf1y.sophisticateditemactions.mixin;

import com.refinedmods.refinedstorage.common.api.grid.Grid;
import com.refinedmods.refinedstorage.common.storage.portablegrid.AbstractPortableGridBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractPortableGridBlockEntity.class)
public interface AccessorAbstractPortableGridBlockEntity {
	@Invoker(value = "getGrid", remap = false)
	Grid sophisticatedItemActions$invokeGetGrid();
}
