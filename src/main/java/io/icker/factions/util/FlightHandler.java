package io.icker.factions.util;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.LuckPermsProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.UUID;

public class FlightHandler {
	private static final HashSet<UUID> flightEnabledPlayers = new HashSet<>();

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				tick(player, server);
			}
		});
	}

	private static void tick(ServerPlayerEntity player, MinecraftServer server) {
		if (player.isCreative() || player.isSpectator()) {
			return;
		}

		UUID playerId = player.getUuid();

		User user = Command.getUser(player);
		if (user == null) {
			removeFlightIfNecessary(player, playerId);
			return;
		}

		Faction faction = user.getFaction();
		if (faction == null) {
			removeFlightIfNecessary(player, playerId);
			return;
		}

		BlockPos factionBlockPos = faction.getFactionBlockPos();
		if (factionBlockPos == null) {
			removeFlightIfNecessary(player, playerId);
			return;
		}

		// Check if the player is in the same dimension as the faction block
		if (!player.getWorld().getRegistryKey().equals(faction.getWorldKey())) {
			removeFlightIfNecessary(player, playerId);
			return;
		}

		// LuckPerms check
		LuckPerms luckPerms = LuckPermsProvider.get();
		if (luckPerms == null) {
			removeFlightIfNecessary(player, playerId);
			return;
		}

		boolean hasFlightPermission = false;

		// Check if any online member of the faction is in the 'infinity' group
		if (factionHasInfinityMember(faction, luckPerms, server)) {
			hasFlightPermission = true;
		} else {
			// Check if the player themselves is in group 'legend' or 'infinity'
			net.luckperms.api.model.user.User luckPermsUser = luckPerms.getPlayerAdapter(ServerPlayerEntity.class).getUser(player);
			if (luckPermsUser == null) {
				removeFlightIfNecessary(player, playerId);
				return;
			}

			boolean inLegendGroup = luckPermsUser.getInheritedGroups(QueryOptions.defaultContextualOptions()).stream()
					.anyMatch(group -> group.getName().equalsIgnoreCase("legend"));

			boolean inInfinityGroup = luckPermsUser.getInheritedGroups(QueryOptions.defaultContextualOptions()).stream()
					.anyMatch(group -> group.getName().equalsIgnoreCase("infinity"));

			if (inLegendGroup || inInfinityGroup) {
				hasFlightPermission = true;
			}
		}

		if (!hasFlightPermission) {
			removeFlightIfNecessary(player, playerId);
			return;
		}

		double deltaX = player.getX() - factionBlockPos.getX();
		double deltaZ = player.getZ() - factionBlockPos.getZ();
		double distanceSquared = deltaX * deltaX + deltaZ * deltaZ;
		double maxDistanceSquared = 64 * 64;

		if (distanceSquared <= maxDistanceSquared) {
			// Enable flight
			if (!player.getAbilities().allowFlying) {
				player.getAbilities().allowFlying = true;
				player.sendAbilitiesUpdate();
				flightEnabledPlayers.add(playerId);
			}
		} else {
			// Disable flight if we enabled it
			removeFlightIfNecessary(player, playerId);
		}
	}

	private static boolean factionHasInfinityMember(Faction faction, LuckPerms luckPerms, MinecraftServer server) {
		for (User member : faction.getUsers()) {
			UUID memberUUID = member.getID();
			ServerPlayerEntity memberPlayer = server.getPlayerManager().getPlayer(memberUUID);
			if (memberPlayer == null) {
				continue; // Member is offline
			}

			net.luckperms.api.model.user.User luckPermsUser = luckPerms.getPlayerAdapter(ServerPlayerEntity.class).getUser(memberPlayer);
			if (luckPermsUser == null) {
				continue;
			}

			boolean inInfinityGroup = luckPermsUser.getInheritedGroups(QueryOptions.defaultContextualOptions()).stream()
					.anyMatch(group -> group.getName().equalsIgnoreCase("infinity"));

			if (inInfinityGroup) {
				return true;
			}
		}
		return false;
	}

	private static void removeFlightIfNecessary(ServerPlayerEntity player, UUID playerId) {
		if (flightEnabledPlayers.contains(playerId)) {
			player.getAbilities().allowFlying = false;
			player.getAbilities().flying = false;
			player.sendAbilitiesUpdate();
			flightEnabledPlayers.remove(playerId);
		}
	}
}
