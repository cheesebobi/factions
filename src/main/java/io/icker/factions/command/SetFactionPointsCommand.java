package io.icker.factions.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.util.Command;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class SetFactionPointsCommand implements Command {
	private int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();
		int points = IntegerArgumentType.getInteger(context, "points");

		Faction faction;
		if (context.getNodes().size() > 3) {
			String factionName = StringArgumentType.getString(context, "faction");
			faction = Faction.getByName(factionName);
			if (faction == null) {
				source.sendError(Text.literal("Faction not found: " + factionName));
				return 0;
			}
		} else {
			faction = Command.getUser(player).getFaction();
			if (faction == null) {
				source.sendError(Text.literal("You are not in a faction."));
				return 0;
			}
		}

		faction.setCounter(points);
		source.sendFeedback(Text.literal("Set the points for faction " + faction.getName() + " to " + points + "."), false);
		return 1;
	}

	public LiteralCommandNode<ServerCommandSource> getNode() {
		return CommandManager
				.literal("setFactionPoints")
				.requires(source -> source.hasPermissionLevel(2)) // Set permission level or other requirements
				.then(CommandManager.argument("points", IntegerArgumentType.integer(0))
						.executes(this::run)
						.then(CommandManager.argument("faction", StringArgumentType.string()).executes(this::run)))
				.build();
	}
}
