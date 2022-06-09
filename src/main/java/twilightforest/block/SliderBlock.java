package twilightforest.block;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import twilightforest.entity.SlideBlock;
import twilightforest.init.TFEntities;
import twilightforest.util.TFDamageSources;

import javax.annotation.Nullable;

public class SliderBlock extends RotatedPillarBlock implements SimpleWaterloggedBlock {

	public static final IntegerProperty DELAY = IntegerProperty.create("delay", 0, 3);
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	private static final int TICK_TIME = 80;
	private static final int OFFSET_TIME = 20;
	private static final int PLAYER_RANGE = 32;
	private static final float BLOCK_DAMAGE = 5;
	private static final VoxelShape Y_BB = Shapes.create(new AABB(0.3125, 0, 0.3125, 0.6875, 1F, 0.6875));
	private static final VoxelShape Z_BB = Shapes.create(new AABB(0.3125, 0.3125, 0, 0.6875, 0.6875, 1F));
	private static final VoxelShape X_BB = Shapes.create(new AABB(0, 0.3125, 0.3125, 1F, 0.6875, 0.6875));

	public SliderBlock() {
		super(Properties.of(Material.METAL, MaterialColor.DIRT).strength(2.0F, 10.0F).randomTicks().noOcclusion());
		this.registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Y).setValue(DELAY, 0).setValue(WATERLOGGED, false));
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		FluidState fluidstate = context.getLevel().getFluidState(context.getClickedPos());
		boolean flag = fluidstate.getType() == Fluids.WATER;
		return super.getStateForPlacement(context).setValue(WATERLOGGED, Boolean.valueOf(flag));
	}

	@Override
	public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos) {
		if (stateIn.getValue(WATERLOGGED)) {
			worldIn.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(worldIn));
		}

		return super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(DELAY, WATERLOGGED);
	}

	@Override
	@Deprecated
	public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(AXIS)) {
			case X -> X_BB;
			case Z -> Z_BB;
			default -> Y_BB;
		};
	}

	@Override
	@Deprecated
	public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
		if (!world.isClientSide && this.isConnectedInRange(world, pos)) {
			//TODO calls for a creakstart sound effect, but it doesnt exist in the game files
			//world.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, TFSounds.SLIDER, SoundCategory.BLOCKS, 0.75F, 1.5F);

			SlideBlock slideBlock = new SlideBlock(TFEntities.SLIDER.get(), world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, state);
			world.addFreshEntity(slideBlock);
		}

		scheduleBlockUpdate(world, pos);
	}

	/**
	 * Check if there is any players in range, and also recursively check connected blocks
	 */
	public boolean isConnectedInRange(Level world, BlockPos pos) {
		Direction.Axis axis = world.getBlockState(pos).getValue(AXIS);

		return switch (axis) {
			case Y -> this.anyPlayerInRange(world, pos) || this.isConnectedInRangeRecursive(world, pos, Direction.UP) || this.isConnectedInRangeRecursive(world, pos, Direction.DOWN);
			case X -> this.anyPlayerInRange(world, pos) || this.isConnectedInRangeRecursive(world, pos, Direction.WEST) || this.isConnectedInRangeRecursive(world, pos, Direction.EAST);
			case Z -> this.anyPlayerInRange(world, pos) || this.isConnectedInRangeRecursive(world, pos, Direction.NORTH) || this.isConnectedInRangeRecursive(world, pos, Direction.SOUTH);
			//default -> this.anyPlayerInRange(world, pos);
		};
	}

	private boolean isConnectedInRangeRecursive(Level world, BlockPos pos, Direction dir) {
		BlockPos dPos = pos.relative(dir);

		if (world.getBlockState(pos) == world.getBlockState(dPos)) {
			return this.anyPlayerInRange(world, dPos) || this.isConnectedInRangeRecursive(world, dPos, dir);
		} else {
			return false;
		}
	}

	private boolean anyPlayerInRange(Level world, BlockPos pos) {
		return world.getNearestPlayer(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, PLAYER_RANGE, false) != null;
	}

	public void scheduleBlockUpdate(Level world, BlockPos pos) {
		int offset = world.getBlockState(pos).getValue(DELAY);
		int update = TICK_TIME - ((int) (world.getGameTime() - (offset * OFFSET_TIME)) % TICK_TIME);
		world.scheduleTick(pos, this, update);
	}

	@Override
	@Deprecated
	public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
		scheduleBlockUpdate(world, pos);
	}

	@Override
	@Deprecated
	public void entityInside(BlockState state, Level worldIn, BlockPos pos, Entity entity) {
		entity.hurt(TFDamageSources.SLIDER, BLOCK_DAMAGE);
		if (entity instanceof LivingEntity) {
			double kx = (pos.getX() + 0.5 - entity.getX()) * 2.0;
			double kz = (pos.getZ() + 0.5 - entity.getZ()) * 2.0;

			((LivingEntity) entity).knockback(2, kx, kz);
		}
	}
}
