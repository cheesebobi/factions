package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;

public class SafeCommand implements Command {

    private int run(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();

        User user = Command.getUser(player);
        Faction faction = user.getFaction();

        DefaultedList<ItemStack> safe = faction.clearBlockedItems(player);
        ItemScatterer.spawn(player.getWorld(), player.getBlockPos(), safe);

        PlayerEvents.OPEN_SAFE.invoker().onOpenSafe(player, Command.getUser(player).getFaction());
        return 1;
    }

    @Override
    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("safe")
            .requires(
                Requires.multiple(
                    Requires.hasPerms("faction.safe", 0),
                    Requires.isMember(),
                    s -> FactionsMod.CONFIG.SAFE != null
                )
            )
            .executes(this::run)
            .build();
    }
}
