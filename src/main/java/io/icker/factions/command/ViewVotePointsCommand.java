package io.icker.factions.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.UserCache;

import java.util.Optional;
import java.util.UUID;

public class ViewVotePointsCommand implements Command {
	private int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();

		ServerPlayerEntity player = null;
		String username = null;
		UUID userId;
		if (context.getNodes().size() > 2) { // Check if username argument is present
			username = StringArgumentType.getString(context, "username");
			MinecraftServer server = source.getServer();
			PlayerManager playerManager = server.getPlayerManager();
			player = playerManager.getPlayer(username);

			if (player == null) {
				userId = getOfflinePlayerUUID(server, username);
				if (userId == null) {
					source.sendError(Text.literal("Player not found: " + username));
					return 0;
				}
			} else {
				userId = player.getUuid();
			}
		} else {
			player = source.getPlayer();
			userId = player.getUuid();
		}

		User user = User.get(userId);
		int votePoints = user.getVotePoints();
		if (player != null) {
			new Message("You have " + votePoints + " vote points.").send(player, false);
		} else {
			source.sendFeedback(Text.literal(username + " has " + votePoints + " vote points."), false);
		}
		return 1;
	}

	private UUID getOfflinePlayerUUID(MinecraftServer server, String username) {
		UserCache userCache = server.getUserCache();
		Optional<GameProfile> profile = userCache.findByName(username);
		return profile.map(GameProfile::getId).orElse(null);
	}

	public LiteralCommandNode<ServerCommandSource> getNode() {
		return CommandManager
				.literal("viewVotePoints")
				.requires(source -> source.hasPermissionLevel(0)) // Any player can run this command
				.executes(this::run)
				.then(CommandManager.argument("username", StringArgumentType.string()).executes(this::run))
				.build();
	}
}
