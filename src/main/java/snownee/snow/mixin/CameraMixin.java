package snownee.snow.mixin;

import java.util.Arrays;

import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import snownee.snow.SnowCommonConfig;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	private boolean initialized;
	@Shadow
	private BlockGetter level;
	@Shadow
	private Vec3 position;
	@Final
	@Shadow
	private Vector3f forwards;

	@Inject(at = @At("HEAD"), method = "getFluidInCamera", cancellable = true)
	private void srm_getFluidInCamera(CallbackInfoReturnable<FogType> ci) {
		if (!initialized || !SnowCommonConfig.thinnerBoundingBox) {
			return;
		}
		Camera.NearPlane camera$nearplane = getNearPlane();
		Vec3 forward = new Vec3(forwards).scale(0.05F);
		for (Vec3 vec3 : Arrays.asList(forward, camera$nearplane.getTopLeft(), camera$nearplane.getTopRight(), camera$nearplane.getBottomLeft(), camera$nearplane.getBottomRight())) {
			Vec3 vec31 = position.add(vec3);
			BlockPos blockpos = BlockPos.containing(vec31);
			BlockState blockstate = level.getBlockState(blockpos);
			if (blockstate.getBlock() instanceof SnowLayerBlock) {
				VoxelShape shape = blockstate.getVisualShape(level, blockpos, CollisionContext.empty());
				HitResult hitResult = shape.clip(position, vec31, blockpos);
				if (hitResult != null && hitResult.getType() != Type.MISS) {
					ci.setReturnValue(FogType.POWDER_SNOW);
					return;
				}
			}
		}
	}

	@Shadow
	public abstract Camera.NearPlane getNearPlane();
}
