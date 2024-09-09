package io.icker.factions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.blocks.FactionBlock;
import io.icker.factions.blocks.FactionBlockEntity;
import io.icker.factions.command.*;
import io.icker.factions.config.Config;
import io.icker.factions.core.*;
import io.icker.factions.util.Command;
import io.icker.factions.util.DynmapWrapper;
import io.icker.factions.util.PlaceholdersWrapper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FactionsMod implements ModInitializer {
    public static Logger LOGGER = LogManager.getLogger("Factions");
    public static final String MODID = "factions";

    public static Config CONFIG = Config.load();
    public static DynmapWrapper dynmap;

    public static final Block FACTIONS_BLOCK = new FactionBlock(FabricBlockSettings.of(Material.METAL).strength(4.0f).luminance(15));

    public static final Block DISABLED_BLOCK = new Block(FabricBlockSettings.of(Material.METAL).strength(4.0f).luminance(1));
    public static BlockEntityType<FactionBlockEntity> FACTIONS_BLOCK_ENTITY;


    @Override
    public void onInitialize() {
        LOGGER.info("Initialized Factions Mod for Minecraft v1.19");

        dynmap = FabricLoader.getInstance().isModLoaded("dynmap") ? new DynmapWrapper() : null;
        if (FabricLoader.getInstance().isModLoaded("placeholder-api")) {
            PlaceholdersWrapper.init();
        }

        Registry.register(Registries.BLOCK, new Identifier("factions", "factions_block"), FACTIONS_BLOCK);
        Registry.register(Registries.ITEM, new Identifier("factions", "factions_block"), new BlockItem(FACTIONS_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, new Identifier("factions", "disabled_block"), DISABLED_BLOCK);
        Registry.register(Registries.ITEM, new Identifier("factions", "disabled_block"), new BlockItem(DISABLED_BLOCK, new Item.Settings()));

        FACTIONS_BLOCK_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier("factions", "factions_block_entity"),
                FabricBlockEntityTypeBuilder.create(FactionBlockEntity::new, FACTIONS_BLOCK).build()
        );

        ChatManager.register();
        FactionsManager.register();
        InteractionManager.register();
        ServerManager.register();
        SoundManager.register();
        WorldManager.register();

        CommandRegistrationCallback.EVENT.register(FactionsMod::registerCommands);
    }


    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        LiteralCommandNode<ServerCommandSource> factions = CommandManager
            .literal("factions")
            .build();

        LiteralCommandNode<ServerCommandSource> alias = CommandManager
            .literal("f")
            .build();

        dispatcher.getRoot().addChild(factions);
        dispatcher.getRoot().addChild(alias);

        Command[] commands = new Command[] {
                new AdminCommand(),
                new SettingsCommand(),
                new ClaimCommand(),
                new UnclaimCommand(),
                new CreateCommand(),
                new DeclareCommand(),
                new DisbandCommand(),
                new HomeCommand(),
                new InfoCommand(),
                new WhoCommand(),
                new InviteCommand(),
                new JoinCommand(),
                new KickCommand(),
                new LeaveCommand(),
                new ListCommand(),
                new MapCommand(),
                new MemberCommand(),
                new ModifyCommand(),
                new RankCommand(),
                new SafeCommand(),
                new PermissionCommand(),
                new GiveFactionBlockCommand(),
                new IncrementVotePointsCommand(),
                new ViewVotePointsCommand(),
                new SetVotePointsCommand(),
                new TopFactionsCommand(),
                new SetFactionPointsCommand(),
                new SetDiscordCommand()
        };

        for (Command command : commands) {
            factions.addChild(command.getNode());
            alias.addChild(command.getNode());
        }
    }
}
