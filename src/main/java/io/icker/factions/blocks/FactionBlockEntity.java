package io.icker.factions.blocks;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

public class FactionBlockEntity extends BlockEntity {
	private UUID owner;
	private Faction faction;
	private int tickCounter = 0;

	public FactionBlockEntity(BlockPos pos, BlockState state) {
		super(FactionsMod.FACTIONS_BLOCK_ENTITY, pos, state);
	}

	@Override
	public void writeNbt(NbtCompound nbt) {
		super.writeNbt(nbt);
		if (owner != null) {
			nbt.putString("owner", owner.toString());
		}
		if (faction != null) {
			nbt.putString("faction", faction.getName());
		}
	}

	@Override
	public void readNbt(NbtCompound nbt) {
		super.readNbt(nbt);
		if (nbt.contains("owner")) {
			owner = UUID.fromString(nbt.getString("owner"));
		}
		if (nbt.contains("faction")) {
			String factionName = nbt.getString("faction");
			faction = Faction.getByName(factionName);
		}
	}

	@Override
	public NbtCompound toInitialChunkDataNbt() {
		return createNbt();
	}

	public void setOwner(UUID owner) {
		this.owner = owner;
		markDirty();
	}

	public UUID getOwner() {
		return owner;
	}

	public void setFaction(Faction faction) {
		this.faction = faction;
		markDirty();
	}

	public Faction getFaction() {
		return faction;
	}

	public static void tick(World world, BlockPos pos, BlockState state, FactionBlockEntity blockEntity) {
		blockEntity.tickCounter++;
		if (blockEntity.tickCounter >= 20) {
			if (blockEntity.faction != null) {
				ChunkPos chunkPos = world.getChunk(pos).getPos();
				List<Claim> claims = blockEntity.faction.getClaims();

				for (Claim claim : claims) {
					if (chunkPos.x == claim.x && chunkPos.z == claim.z && world.getRegistryKey().getValue().toString().equals(claim.level)) {
						blockEntity.faction.incrementCounter();
						blockEntity.tickCounter = 0;
					}
				}
			}
		}
	}
}
