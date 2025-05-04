package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class SetDiscordCommand implements Command {
	private int set(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();
		String discordUsername = StringArgumentType.getString(context, "username");

		User user = Command.getUser(player);
		user.setDiscordUsername(discordUsername);

		new Message("Your Discord username has been set to: " + discordUsername)
				.send(player, false);
		return 1;
	}

	private int remove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();

		User user = Command.getUser(player);
		user.setDiscordUsername("");

		new Message("Your Discord username has been removed.")
				.send(player, false);
		return 1;
	}

	@Override
	public LiteralCommandNode<ServerCommandSource> getNode() {
		return CommandManager
				.literal("discord")
				.then(CommandManager.literal("set")
						.then(CommandManager.argument("username", StringArgumentType.greedyString())
								.executes(this::set)))
				.then(CommandManager.literal("remove")
						.executes(this::remove))
				.build();
	}
}
