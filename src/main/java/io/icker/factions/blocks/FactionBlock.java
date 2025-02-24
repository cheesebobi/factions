package io.icker.factions.blocks;

import java.util.List;
import java.util.UUID;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
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
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
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
				replaceWithDisabledBlock(world, pos);
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

			// If no claim matches, replace with disabled block
			replaceWithDisabledBlock(world, pos);
		} else {
			new Message("You must be in a faction to place a Faction Block!").fail().send(player, false);
			replaceWithDisabledBlock(world, pos);
		}
	}

	private void replaceWithDisabledBlock(World world, BlockPos pos) {
		BlockState disabledBlockState = FactionsMod.DISABLED_BLOCK.getDefaultState();
		world.setBlockState(pos, disabledBlockState);
		FactionsMod.LOGGER.info("Faction Block at " + pos + " has been replaced with a FactionsDisabled_Block.");
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

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}

		if (!(player instanceof ServerPlayerEntity)) {
			return ActionResult.PASS;
		}

		ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

		// Get the faction of the player
		User user = Command.getUser(serverPlayer);
		Faction faction = user.getFaction();

		if (faction != null) {
			// Ensure the block is associated with the faction and is allowed to open the safe
			FactionBlockEntity blockEntity = (FactionBlockEntity) world.getBlockEntity(pos);
			if (blockEntity != null && faction.equals(blockEntity.getFaction())) {
				// Clear blocked items and open the safe
				DefaultedList<ItemStack> safeItems = faction.clearBlockedItems(serverPlayer);
				ItemScatterer.spawn(serverPlayer.getWorld(), serverPlayer.getBlockPos(), safeItems);

				// Trigger the OPEN_SAFE event
				PlayerEvents.OPEN_SAFE.invoker().onOpenSafe(serverPlayer, faction);

				return ActionResult.SUCCESS;
			} else {
				new Message("This Faction Block does not belong to your faction!").fail().send(serverPlayer, false);
				return ActionResult.FAIL;
			}
		} else {
			new Message("You are not part of a faction!").fail().send(serverPlayer, false);
			return ActionResult.FAIL;
		}
	}
}
