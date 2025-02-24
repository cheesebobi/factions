package io.icker.factions.blocks;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.*;

public class FactionBlockEntity extends BlockEntity {
	private UUID owner;
	private Faction faction;
	private int tickCounter = 0;
	private Set<UUID> intruders = new HashSet<>();

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
			blockEntity.tickCounter = 0;

			if (blockEntity.faction != null) {
				// Existing code to increment counter
				ChunkPos chunkPos = world.getChunk(pos).getPos();
				List<Claim> claims = blockEntity.faction.getClaims();

				for (Claim claim : claims) {
					if (chunkPos.x == claim.x && chunkPos.z == claim.z && world.getRegistryKey().getValue().toString().equals(claim.level)) {
						blockEntity.faction.incrementCounter();
					}
				}
			}

			// Intruder detection
			blockEntity.checkForIntruders(world, pos);
		}
	}

	private void checkForIntruders(World world, BlockPos pos) {
		double radius = 100.0;

		// Define the area as a Box
		Box area = new Box(
				pos.getX() - radius, -64, pos.getZ() - radius,
				pos.getX() + radius, world.getHeight(), pos.getZ() + radius
		);

		// Get all ServerPlayerEntities within the area
		List<ServerPlayerEntity> nearbyPlayers = world.getEntitiesByClass(
				ServerPlayerEntity.class,
				area,
				player -> true // No additional filtering
		);

		Set<UUID> currentIntruders = new HashSet<>();

		for (ServerPlayerEntity player : nearbyPlayers) {
			UUID playerUUID = player.getUuid();

			User user = Command.getUser(player);
			if ((user != null && user.getFaction() != null && user.getFaction().equals(faction)) || player.isCreative() || player.isSpectator()) {
				// Player is a member of the faction; skip
				continue;
			}

			// Player is an intruder
			currentIntruders.add(playerUUID);

			if (!intruders.contains(playerUUID)) {
				// New intruder detected
				intruders.add(playerUUID);

				// Send alert to faction members
				sendIntruderAlert(world, player);
			}
		}

		// Remove players who are no longer intruders
		intruders.retainAll(currentIntruders);
	}

	private void sendIntruderAlert(World world, ServerPlayerEntity intruder) {
		String intruderName = intruder.getName().getString();

		// Collect Discord IDs of faction members
		List<String> discordUsernames = faction.getUsers().stream()
				.map(member -> member.getDiscordUsername())
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

		// Create a data object to send
		Map<String, Object> data = new HashMap<>();
		data.put("intruderName", intruderName);
		data.put("discordUsernames", discordUsernames);

		// Convert data to JSON
		Gson gson = new Gson();
		String jsonData = gson.toJson(data);

		Text message = Text.literal("Intruder detected near your faction block: " + intruderName)
				.formatted(Formatting.RED);

		for (User member : faction.getUsers()) {
			UUID memberUUID = member.getID();
			ServerPlayerEntity memberPlayer = world.getServer().getPlayerManager().getPlayer(memberUUID);
			if (memberPlayer != null) {
				memberPlayer.sendMessage(message, false);
			}
		}

		// Send HTTP POST request
		try {
			URL url = new URL("https://factiondiscordhook.kubanbt.net/alert");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json; utf-8");
			conn.setDoOutput(true);

			// Optional: Add authentication token
			conn.setRequestProperty("Authorization", "Bearer "+FactionsMod.CONFIG.DISCORD_AUTH_KEY);

			try (OutputStream os = conn.getOutputStream()) {
				byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
				os.write(input, 0, input.length);
			}

			int code = conn.getResponseCode();
			if (code != 200) {
				// Handle error
				System.err.println("Failed to send alert to Discord bot, response code: " + code);
			}

			conn.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
