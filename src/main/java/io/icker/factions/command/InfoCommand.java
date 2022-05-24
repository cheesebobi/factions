package io.icker.factions.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Member;
import io.icker.factions.config.Config;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.UserCache;
import net.minecraft.util.Util;

import java.util.List;
import java.util.stream.Collectors;

public class InfoCommand implements Command {
    private int self(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Member member = Member.get(player.getUuid());
        if (!member.isInFaction()) {
            new Message("Command can only be used whilst in a faction").fail().send(player, false);
            return 0;
        }

        return info(player, member.getFaction());
    }

    private int any(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String factionName = StringArgumentType.getString(context, "faction");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = Faction.getByName(factionName);
        if (faction == null) {
            new Message("Faction does not exist").fail().send(player, false);
            return 0;
        }

        return info(player, faction);
    }

    public static int info(ServerPlayerEntity player, Faction faction) {
        List<Member> members = faction.getMembers();

        String memberText = members.size() + (Config.MAX_FACTION_SIZE != -1 ? "/" + Config.MAX_FACTION_SIZE : (" member" + (members.size() != 1 ? "s" : "")));

        UserCache cache = player.getServer().getUserCache();
        String membersList = members.stream()
                .map(member -> cache.getByUuid(member.getID()).orElse(new GameProfile(Util.NIL_UUID, "{Uncached Player}")).getName())
                .collect(Collectors.joining(", "));

        int requiredPower = faction.getClaims().size() * Config.CLAIM_WEIGHT;
        int maxPower = members.size() * Config.MEMBER_POWER + Config.BASE_POWER;

        new Message("")
                .add(new Message(memberText).hover(membersList))
                .filler("·")
                .add(
                    new Message(Formatting.GREEN.toString() + faction.getPower() + slash() + requiredPower + slash() + maxPower)
                    .hover("Current / Required / Max")
                )
                .prependFaction(faction)
                .send(player, false);

        return 1;
    }

    private static String slash() {
        return Formatting.GRAY + " / " + Formatting.GREEN;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("info")
            .requires(Requires.hasPerms("factions.info", 0))
            .executes(this::self)
            .then(
                CommandManager.argument("faction", StringArgumentType.greedyString())
                .executes(this::any)
            )
            .build();
    }
}