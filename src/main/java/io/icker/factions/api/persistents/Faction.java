package io.icker.factions.api.persistents;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.database.Database;
import io.icker.factions.database.Field;
import io.icker.factions.database.Name;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Name("Faction")
public class Faction {
    private static final HashMap<UUID, Faction> STORE = Database.load(Faction.class, Faction::getID);

    @Field("ID")
    private UUID id;

    @Field("Name")
    private String name;

    @Field("Description")
    private String description;

    @Field("MOTD")
    private String motd;

    @Field("Color")
    private String color;

    @Field("Open")
    private boolean open;

    @Field("Power")
    private int power;

    @Field("Home")
    private Home home;

    @Field("Safe")
    private SimpleInventory safe = new SimpleInventory(54);

    @Field("Invites")
    public ArrayList<UUID> invites = new ArrayList<>();

    @Field("Relationships")
    private ArrayList<Relationship> relationships = new ArrayList<>();

    @Field("GuestPermissions")
    public ArrayList<Relationship.Permissions> guest_permissions = new ArrayList<>(FactionsMod.CONFIG.RELATIONSHIPS.DEFAULT_GUEST_PERMISSIONS);

    @Field("FactionCounter")
    private int counter = 0;

    @Field("FactionBlockPos")
    private String factionBlockPosString;

    public Faction(String name, String description, String motd, Formatting color, boolean open, int power, int counter) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.motd = motd;
        this.description = description;
        this.color = color.getName();
        this.open = open;
        this.power = power;
        this.counter = counter;
        this.factionBlockPosString = null;
    }

    @SuppressWarnings("unused")
    public Faction() {}

    @SuppressWarnings("unused")
    public String getKey() {
        return id.toString();
    }

    public RegistryKey<World> getWorldKey() {
        BlockPos fBlock = getFactionBlockPos(); // Get faction block position
        if (fBlock == null) {
            return null; // If there is no faction block set, return null
        }

        List<Claim> claims = getClaims(); // Get all claims for this faction
        for (Claim claim : claims) {
            // Assuming the faction block is in this chunk, you may need a chunk-position check
            if (claim.x == fBlock.getX() >> 4 && claim.z == fBlock.getZ() >> 4) {
                // Convert the level string to a RegistryKey<World>
                return RegistryKey.of(RegistryKeys.WORLD, new Identifier(claim.level));
            }
        }

        return null; // Return null if no matching claim is found
    }

    @Nullable
    public static Faction get(UUID id) {
        return STORE.get(id);
    }

    @Nullable
    public static Faction getByName(String name) {
        return STORE.values()
                .stream()
                .filter(f -> f.name.equals(name))
                .findFirst()
                .orElse(null);
    }

    public static void add(Faction faction) {
        STORE.put(faction.id, faction);
    }

    public static Collection<Faction> all() {
        return STORE.values();
    }

    @SuppressWarnings("unused")
    public static List<Faction> allBut(UUID id) {
        return STORE.values()
                .stream()
                .filter(f -> f.id != id)
                .toList();
    }

    public UUID getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Formatting getColor() {
        return Formatting.byName(color);
    }

    public String getDescription() {
        return description;
    }

    public String getMOTD() {
        return motd;
    }

    public int getPower() {
        return power;
    }

    public SimpleInventory getSafe() {
        return safe;
    }

    public DefaultedList<ItemStack> clearSafe() {
        DefaultedList<ItemStack> stacks = this.safe.stacks;
        this.safe = new SimpleInventory(54);
        return stacks;
    }

    // Function to clear blocked items from the safe
    public DefaultedList<ItemStack> clearBlockedItems(PlayerEntity player) {
        DefaultedList<ItemStack> removedItems = DefaultedList.of();
        for (int i = 0; i < this.safe.size(); i++) {
            ItemStack item = this.safe.getStack(i);
            String itemName = Registries.ITEM.getId(item.getItem()).getPath();
            if (FactionsMod.CONFIG.SAFE.BLOCKED_ITEMS.contains(itemName)) {
                removedItems.add(item);
                player.dropItem(item, false); // Drop the blocked item
                this.safe.setStack(i, ItemStack.EMPTY); // Remove the blocked item from the safe
            }
        }
        return removedItems; // Return the removed blocked items
    }

    public boolean isOpen() {
        return open;
    }

    public void setName(String name) {
        this.name = name;
        FactionEvents.MODIFY.invoker().onModify(this);
    }

    public void setDescription(String description) {
        this.description = description;
        FactionEvents.MODIFY.invoker().onModify(this);
    }

    public void setMOTD(String motd) {
        this.motd = motd;
        FactionEvents.MODIFY.invoker().onModify(this);
    }

    public void setColor(Formatting color) {
        this.color = color.getName();
        FactionEvents.MODIFY.invoker().onModify(this);
    }

    public void setOpen(boolean open) {
        this.open = open;
        FactionEvents.MODIFY.invoker().onModify(this);
    }

    public int adjustPower(int adjustment) {
        int maxPower = calculateMaxPower();
        int newPower = Math.min(Math.max(0, power + adjustment), maxPower);
        int oldPower = this.power;

        if (newPower == oldPower) return 0;

        power = newPower;
        FactionEvents.POWER_CHANGE.invoker().onPowerChange(this, oldPower);
        return Math.abs(newPower - oldPower);
    }

    public List<User> getUsers() {
        return User.getByFaction(id);
    }

    public List<Claim> getClaims() {
        return Claim.getByFaction(id);
    }

    public void removeAllClaims() {
        Claim.getByFaction(id)
                .stream()
                .forEach(Claim::remove);
        FactionEvents.REMOVE_ALL_CLAIMS.invoker().onRemoveAllClaims(this);
    }

    public void addClaim(int x, int z, String level) {
        Claim.add(new Claim(x, z, level, id));
    }

    public boolean isInvited(UUID playerID) {
        return invites.stream().anyMatch(invite -> invite.equals(playerID));
    }

    public Home getHome() {
        return home;
    }

    public void setHome(Home home) {
        this.home = home;
        FactionEvents.SET_HOME.invoker().onSetHome(this, home);
    }

    public Relationship getRelationship(UUID target) {
        return relationships.stream().filter(rel -> rel.target.equals(target)).findFirst().orElse(new Relationship(target, Relationship.Status.NEUTRAL));
    }

    public Relationship getReverse(Relationship rel) {
        return Faction.get(rel.target).getRelationship(id);
    }

    public boolean isMutualAllies(UUID target) {
        Relationship rel = getRelationship(target);
        return rel.status == Relationship.Status.ALLY && getReverse(rel).status == Relationship.Status.ALLY;
    }

    public List<Relationship> getMutualAllies() {
        return relationships.stream().filter(rel -> isMutualAllies(rel.target)).toList();
    }

    public List<Relationship> getEnemiesWith() {
        return relationships.stream().filter(rel -> rel.status == Relationship.Status.ENEMY).toList();
    }

    public List<Relationship> getEnemiesOf() {
        return relationships.stream().filter(rel -> getReverse(rel).status == Relationship.Status.ENEMY).toList();
    }

    public void removeRelationship(UUID target) {
        relationships = new ArrayList<>(relationships.stream().filter(rel -> !rel.target.equals(target)).toList());
    }

    public void setRelationship(Relationship relationship) {
        if (getRelationship(relationship.target) != null) {
            removeRelationship(relationship.target);
        }
        if (relationship.status != Relationship.Status.NEUTRAL || !relationship.permissions.isEmpty())
            relationships.add(relationship);
    }

    public void remove() {
        for (User user : getUsers()) {
            user.leaveFaction();
        }
        for (Relationship rel : relationships) {
            Faction.get(rel.target).removeRelationship(id);
        }
        removeAllClaims();
        STORE.remove(id);
        FactionEvents.DISBAND.invoker().onDisband(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Faction faction = (Faction) o;
        return id.equals(faction.id);
    }

    public static void save() {
        Database.save(Faction.class, STORE.values().stream().toList());
    }

    public int calculateMaxPower(){
        return FactionsMod.CONFIG.POWER.BASE + (getUsers().size() * FactionsMod.CONFIG.POWER.MEMBER);
    }

    public int getCounter() {
        int totalVotePoints = 0;
        for (User user : getUsers()) {
            totalVotePoints += user.getVotePoints();
        }
        return counter + totalVotePoints;
    }

    public void incrementCounter() {
        counter++;
    }

    public void setCounter(int counter) {
        this.counter = counter;
        save(); // Save the faction's data if necessary
    }


    public void resetCounter() {
        counter = 0;
    }

    public void setFactionBlockPos(BlockPos pos) {
        this.factionBlockPosString = posToString(pos);
    }

    public BlockPos getFactionBlockPos() {
        return posFromString(factionBlockPosString);
    }

    public boolean hasFactionBlock() {
        return factionBlockPosString != null;
    }

    public void removeFactionBlock() {
        this.factionBlockPosString = null;
    }

    private static String posToString(BlockPos pos) {
        return pos == null ? "" : pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static BlockPos posFromString(String posString) {
        if (posString == null || posString.isEmpty()) {
            return null;
        }
        String[] parts = posString.split(",");
        return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }
}
