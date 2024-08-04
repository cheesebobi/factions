package io.icker.factions.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;

public class UnclaimCommand implements Command {
	private int remove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();

		ServerPlayerEntity player = source.getPlayer();
		ServerWorld world = player.getWorld();

		ChunkPos chunkPos = world.getChunk(player.getBlockPos()).getPos();
		String dimension = world.getRegistryKey().getValue().toString();

		Claim existingClaim = Claim.get(chunkPos.x, chunkPos.z, dimension);

		if (existingClaim == null) {
			new Message("Cannot remove a claim on an unclaimed chunk")
					.fail()
					.send(player, false);
			return 0;
		}

		User user = Command.getUser(player);
		Faction faction = user.getFaction();

		if (!user.bypass && existingClaim.getFaction().getID() != faction.getID()) {
			new Message("Cannot remove a claim owned by another faction")
					.fail()
					.send(player, false);
			return 0;
		}

		existingClaim.remove();
		new Message(
				"Claim (%d, %d) removed by %s",
				existingClaim.x,
				existingClaim.z,
				player.getName().getString()
		).send(faction);
		return 1;
	}

	private int removeSize(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		int size = IntegerArgumentType.getInteger(context, "size");
		ServerCommandSource source = context.getSource();

		ServerPlayerEntity player = source.getPlayer();
		ServerWorld world = player.getWorld();
		String dimension = world.getRegistryKey().getValue().toString();

		User user = Command.getUser(player);
		Faction faction = user.getFaction();

		for (int x = -size + 1; x < size; x++) {
			for (int y = -size + 1; y < size; y++) {
				ChunkPos chunkPos = world.getChunk(player.getBlockPos().add(x * 16, 0, y * 16)).getPos();
				Claim existingClaim = Claim.get(chunkPos.x, chunkPos.z, dimension);

				if (existingClaim != null && (user.bypass || existingClaim.getFaction().getID() == faction.getID())) existingClaim.remove();
			}
		}

		ChunkPos chunkPos = world.getChunk(player.getBlockPos().add((-size + 1) * 16, 0, (-size + 1) * 16)).getPos();
		new Message(
				"Claims (%d, %d) to (%d, %d) removed by %s ",
				chunkPos.x,
				chunkPos.z,
				chunkPos.x + size - 1,
				chunkPos.z + size - 1,
				player.getName().getString()
		).send(faction);

		return 1;
	}

	private int removeAll(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();

		Faction faction = Command.getUser(player).getFaction();

		faction.removeAllClaims();
		new Message(
				"All claims removed by %s",
				player.getName().getString()
		).send(faction);
		return 1;
	}

	@Override
	public LiteralCommandNode<ServerCommandSource> getNode() {
		return CommandManager
				.literal("unclaim")
				.requires(Requires.isCommander())
				.executes(this::remove) // Default to remove if no subcommand is provided
				.then(
						CommandManager.argument("size", IntegerArgumentType.integer(1, 7))
								.requires(Requires.hasPerms("factions.claim.remove.size", 0))
								.executes(this::removeSize)
				)
				.then(
						CommandManager.literal("all")
								.requires(Requires.hasPerms("factions.claim.remove.all", 0))
								.executes(this::removeAll)
				)
				.build();
	}
}
