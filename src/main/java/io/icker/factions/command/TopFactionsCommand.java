package io.icker.factions.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.UserCache;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TopFactionsCommand implements Command {
	private int runFactions(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();

		List<Faction> topFactions = Faction.all().stream()
				.sorted(Comparator.comparingInt(Faction::getCounter).reversed())
				.limit(10)
				.collect(Collectors.toList());

		StringBuilder message = new StringBuilder("Top 10 Factions:\n");
		for (int i = 0; i < topFactions.size(); i++) {
			Faction faction = topFactions.get(i);
			message.append(String.format("%d. %s - %d points\n", i + 1, faction.getName(), faction.getCounter()));
		}

		source.sendFeedback(Text.literal(message.toString()), false);
		return 1;
	}

	private int runPlayers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		MinecraftServer server = source.getServer();
		UserCache userCache = server.getUserCache();

		List<User> topPlayers = User.all().stream()
				.sorted(Comparator.comparingInt(User::getVotePoints).reversed())
				.limit(10)
				.collect(Collectors.toList());

		StringBuilder message = new StringBuilder("Top 10 Players:\n");
		for (int i = 0; i < topPlayers.size(); i++) {
			User user = topPlayers.get(i);
			Optional<GameProfile> profile = userCache.getByUuid(user.getID());
			String username = profile.map(GameProfile::getName).orElse("Unknown");
			message.append(String.format("%d. %s - %d vote points\n", i + 1, username, user.getVotePoints()));
		}

		source.sendFeedback(Text.literal(message.toString()), false);
		return 1;
	}

	private int runDefault(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		return runFactions(context);
	}

	public LiteralCommandNode<ServerCommandSource> getNode() {
		return CommandManager
				.literal("top")
				.requires(source -> source.hasPermissionLevel(0)) // Any player can run this command
				.executes(this::runDefault)
				.then(CommandManager.literal("factions").executes(this::runFactions))
				.then(CommandManager.literal("players").executes(this::runPlayers))
				.build();
	}
}
