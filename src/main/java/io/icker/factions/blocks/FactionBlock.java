package io.icker.factions.blocks;

import java.util.List;
import java.util.UUID;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;

public class FactionBlock extends Block implements BlockEntityProvider {

	public FactionBlock(Settings settings) {
		super(settings);
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new FactionBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		return world.isClient ? null : (BlockEntityTicker<T>) (world1, pos, state1, t) -> {
			if (t instanceof FactionBlockEntity) {
				FactionBlockEntity.tick(world1, pos, state1, (FactionBlockEntity) t);
			}
		};
	}

	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
		super.onPlaced(world, pos, state, placer, itemStack);

		ServerPlayerEntity player = placer instanceof ServerPlayerEntity ? (ServerPlayerEntity) placer : null;

		if (player == null) {
			return;
		}

		Faction blockFaction = Command.getUser(player).getFaction();

		if (blockFaction != null) {
			if (blockFaction.hasFactionBlock()) {
				new Message("Your faction already has a Faction Block placed, this one won't count!").fail().send(player, false);
				return;
			}

			ChunkPos chunkPos = world.getChunk(pos).getPos();
			List<Claim> claims = blockFaction.getClaims();
			for (Claim claim : claims) {
				if (chunkPos.x == claim.x && chunkPos.z == claim.z && world.getRegistryKey().getValue().toString().equals(claim.level)) {
					FactionBlockEntity blockEntity = (FactionBlockEntity) world.getBlockEntity(pos);
					if (blockEntity != null) {
						blockEntity.setOwner(player.getUuid());
						blockEntity.setFaction(blockFaction);
						blockFaction.setFactionBlockPos(pos);
						blockFaction.incrementCounter();
						new Message("You placed a Faction Block which will generate points!").send(player, false);
					}
					return;
				}
			}
		}
	}

	private void resetFactionCounterAndRemoveBlock(World world, BlockPos pos) {
		FactionBlockEntity blockEntity = (FactionBlockEntity) world.getBlockEntity(pos);
		if (blockEntity != null && blockEntity.getFaction() != null) {
			blockEntity.getFaction().resetCounter();
			blockEntity.getFaction().removeFactionBlock();
		} else {
			FactionsMod.LOGGER.error("Block entity is null or faction is null at position: " + pos);
		}
	}

	@Override
	public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		resetFactionCounterAndRemoveBlock(world, pos);
		super.onBreak(world, pos, state, player);
	}

	@Override
	public void onDestroyedByExplosion(World world, BlockPos pos, Explosion explosion) {
		resetFactionCounterAndRemoveBlock(world, pos);
		super.onDestroyedByExplosion(world, pos, explosion);
	}

	@Override
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (state.getBlock() != newState.getBlock()) {
			resetFactionCounterAndRemoveBlock(world, pos);
			super.onStateReplaced(state, world, pos, newState, moved);
		}
	}
}
