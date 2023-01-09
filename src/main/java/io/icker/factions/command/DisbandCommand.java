package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.text.Message;
import io.icker.factions.text.TranslatableText;
import io.icker.factions.util.Command;
import net.minecraft.item.ItemStack;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;

public class DisbandCommand implements Command {
    private int run(CommandContext<ServerCommandSource> context, boolean confirm) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        User user = Command.getUser(player);
        Faction faction = user.getFaction();
        assert faction != null;

        if (!faction.getSafe().isEmpty() && !confirm) {
            new Message().append(new TranslatableText("disband.safe.not-empty")).append(new TranslatableText("disband.safe.continue").hover("disband.safe.click").click("/f disband confirm")).send(player, false);
            return 0;
        }

        DefaultedList<ItemStack> safe = faction.clearSafe();

        ItemScatterer.spawn(player.getWorld(), player.getBlockPos(), safe);

        new Message().append(new TranslatableText("disband", player.getName().getString())).send(faction);
        faction.remove();

        PlayerManager manager = source.getServer().getPlayerManager();
        for (ServerPlayerEntity p : manager.getPlayerList()) {
            manager.sendCommandTree(p);
        }
        return 1;
    }

    @Override
    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("disband")
            .requires(Requires.multiple(Requires.isOwner(), Requires.hasPerms("factions.disband", 0)))
            .executes(context -> this.run(context, false))
            .then(CommandManager.literal("confirm").executes(context -> this.run(context, true)))
            .build();
    }
}