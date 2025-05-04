package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;

public class GiveFactionBlockCommand implements Command {
	private int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();
		User user = Command.getUser(player);

		if (!user.isInFaction()) {
			new Message("You must be in a faction to use this command").fail().send(player, false);
			return 0;
		}

		if (user.getFaction().hasFactionBlock()) {
			new Message("You already have an active faction block!").fail().send(player, false);
			return 0;
		}

		ItemStack factionBlockItem = new ItemStack(Registries.ITEM.get(new Identifier(FactionsMod.MODID, "factions_block")));
		if (!player.getInventory().insertStack(factionBlockItem)) {
			new Message("Unable to give faction block. Your inventory is full").fail().send(player, false);
			return 0;
		}

		new Message("You have been given a faction block").send(player, false);
		return 1;
	}

	public LiteralCommandNode<ServerCommandSource> getNode() {
		return CommandManager
				.literal("givefactionblock")
				.requires(Requires.isMember())
				.executes(this::run)
				.build();
	}
}
