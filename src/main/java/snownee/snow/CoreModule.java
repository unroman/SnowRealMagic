package snownee.snow;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.IntegerValue;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import snownee.kiwi.AbstractModule;
import snownee.kiwi.KiwiGO;
import snownee.kiwi.KiwiModule;
import snownee.kiwi.KiwiModule.Name;
import snownee.kiwi.KiwiModule.NoItem;
import snownee.kiwi.KiwiModule.RenderLayer;
import snownee.kiwi.KiwiModule.RenderLayer.Layer;
import snownee.kiwi.loader.Platform;
import snownee.kiwi.loader.event.ClientInitEvent;
import snownee.kiwi.loader.event.InitEvent;
import snownee.snow.block.EntitySnowLayerBlock;
import snownee.snow.block.SnowFenceBlock;
import snownee.snow.block.SnowFenceGateBlock;
import snownee.snow.block.SnowSlabBlock;
import snownee.snow.block.SnowStairsBlock;
import snownee.snow.block.SnowWallBlock;
import snownee.snow.block.entity.SnowBlockEntity;
import snownee.snow.block.entity.SnowCoveredBlockEntity;
import snownee.snow.client.FallingSnowRenderer;
import snownee.snow.client.SnowClient;
import snownee.snow.entity.FallingSnowEntity;
import snownee.snow.loot.NormalizeLoot;
import snownee.snow.mixin.BlockAccess;

@KiwiModule
public class CoreModule extends AbstractModule {

	public static final TagKey<Block> BOTTOM_SNOW = blockTag(SnowRealMagic.MODID, "bottom_snow");

	public static final TagKey<Block> CONTAINABLES = blockTag(SnowRealMagic.MODID, "containables");

	public static final TagKey<Block> NOT_CONTAINABLES = blockTag(SnowRealMagic.MODID, "not_containables");

	public static final TagKey<Block> OFFSET_Y = blockTag(SnowRealMagic.MODID, "offset_y");

	public static final TagKey<Block> CANNOT_ACCUMULATE_ON = blockTag(SnowRealMagic.MODID, "cannot_accumulate_on");

	@NoItem
	@Name("snow")
	@RenderLayer(Layer.CUTOUT)
	public static final KiwiGO<EntitySnowLayerBlock> TILE_BLOCK = go(() -> new EntitySnowLayerBlock(blockProp(Blocks.SNOW).dynamicShape()));

	@NoItem
	@RenderLayer(Layer.CUTOUT)
	public static final KiwiGO<Block> FENCE = go(() -> new SnowFenceBlock(blockProp(Blocks.OAK_FENCE).mapColor(MapColor.SNOW).randomTicks().dynamicShape()));

	@NoItem
	@RenderLayer(Layer.CUTOUT)
	public static final KiwiGO<Block> FENCE2 = go(() -> new SnowFenceBlock(blockProp(Blocks.NETHER_BRICK_FENCE).mapColor(MapColor.SNOW).randomTicks().dynamicShape()));

	@NoItem
	@RenderLayer(Layer.CUTOUT)
	public static final KiwiGO<Block> STAIRS = go(() -> new SnowStairsBlock(blockProp(Blocks.OAK_STAIRS).mapColor(MapColor.SNOW).randomTicks()));

	@NoItem
	@RenderLayer(Layer.CUTOUT)
	public static final KiwiGO<Block> SLAB = go(() -> new SnowSlabBlock(blockProp(Blocks.OAK_SLAB).mapColor(MapColor.SNOW).randomTicks()));

	@NoItem
	@RenderLayer(Layer.CUTOUT)
	public static final KiwiGO<Block> FENCE_GATE = go(() -> new SnowFenceGateBlock(blockProp(Blocks.OAK_FENCE_GATE).mapColor(MapColor.SNOW).randomTicks().dynamicShape()));

	@NoItem
	@RenderLayer(Layer.CUTOUT)
	public static final KiwiGO<Block> WALL = go(() -> new SnowWallBlock(blockProp(Blocks.COBBLESTONE_WALL).mapColor(MapColor.SNOW).randomTicks().dynamicShape()));

	@Name("snow")
	public static final KiwiGO<BlockEntityType<SnowBlockEntity>> TILE = blockEntity(SnowBlockEntity::new, null, TILE_BLOCK);

	public static final KiwiGO<BlockEntityType<SnowCoveredBlockEntity>> TEXTURE_TILE = blockEntity(SnowCoveredBlockEntity::new, null, FENCE, FENCE2, STAIRS, SLAB, FENCE_GATE, WALL);

	@Name("snow")
	public static final KiwiGO<EntityType<FallingSnowEntity>> ENTITY = go(() -> FabricEntityTypeBuilder.<FallingSnowEntity>create(MobCategory.MISC, FallingSnowEntity::new).entityFactory((spawnEntity, world) -> new FallingSnowEntity(world)).dimensions(EntityDimensions.fixed(0.98F, 0.001F)).build());

	public static final KiwiGO<LootPoolEntryType> NORMALIZE = go(() -> new LootPoolEntryType(new NormalizeLoot.Serializer()));

	public static final GameRules.Key<IntegerValue> BLIZZARD_STRENGTH = GameRuleRegistry.register(SnowRealMagic.MODID + ":blizzardStrength", GameRules.Category.MISC, GameRuleFactory.createIntRule(0));

	public static final GameRules.Key<IntegerValue> BLIZZARD_FREQUENCY = GameRuleRegistry.register(SnowRealMagic.MODID + ":blizzardFrequency", GameRules.Category.MISC, GameRuleFactory.createIntRule(10000));

	public CoreModule() {
		if (Platform.isPhysicalClient()) {
			ModelLoadingRegistry.INSTANCE.registerModelProvider((manager, out) -> out.accept(SnowClient.OVERLAY_MODEL));
		}
		UseBlockCallback.EVENT.register(GameEvents::onItemUse);
		PlayerBlockBreakEvents.BEFORE.register(GameEvents::onDestroyedByPlayer);
		decorators.remove(BuiltInRegistries.BLOCK);
	}

	@Override
	protected void init(InitEvent event) {
		event.enqueueWork(() -> {
			BlockBehaviour.StateArgumentPredicate<EntityType<?>> predicate = (blockState, blockGetter, blockPos, entityType) -> {
				return blockState.getValue(BlockStateProperties.LAYERS) <= SnowCommonConfig.mobSpawningMaxLayers;
			};
			((BlockAccess) Blocks.SNOW).getProperties().isValidSpawn(predicate);
			((BlockAccess) TILE_BLOCK.get()).getProperties().isValidSpawn(predicate);
		});
	}

	@Override
	@Environment(EnvType.CLIENT)
	protected void clientInit(ClientInitEvent event) {
		EntityRendererRegistry.register(ENTITY.get(), FallingSnowRenderer::new);
	}

}
