package io.icker.factions.core;

import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.persistents.Faction;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;



public class CustomContainerScreenHandler extends GenericContainerScreenHandler {
    private final Faction faction;

    public CustomContainerScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerType<?> type, Faction faction, int multiplier) {
        super(type, syncId, playerInventory, faction.getSafe(), faction.getSafe().size() / multiplier);
        this.faction = faction;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        PlayerEvents.CLOSE_SAFE.invoker().onCloseSafe(player, faction);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}