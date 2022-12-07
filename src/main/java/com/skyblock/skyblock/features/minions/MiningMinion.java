package com.skyblock.skyblock.features.minions;

import com.skyblock.skyblock.Skyblock;
import com.skyblock.skyblock.SkyblockPlayer;
import com.skyblock.skyblock.enums.MiningMinionType;
import com.skyblock.skyblock.features.island.IslandManager;
import com.skyblock.skyblock.utilities.Util;
import com.skyblock.skyblock.utilities.item.ItemBuilder;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.PacketPlayOutBlockBreakAnimation;
import net.minecraft.server.v1_8_R3.PacketPlayOutBlockChange;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class MiningMinion extends MinionBase {

    private BukkitRunnable task;
    private int i;

    private final MiningMinionType type;

    public MiningMinion(MiningMinionType minion, UUID uuid) {
        super(
                uuid,
                minion,
                minion.getName(),
                minion.getRecipe(),
                minion.getHand(),
                minion.getHead(),
                minion.getLeatherArmorColor(),
                minion.getTimeBetweenActions(),
                minion.getGetMaximumStorage()
        );

        this.task = null;
        this.i = 0;

        this.type = minion;

        this.resourcesGenerated = 0;
        this.timeBetweenActions = minion.getTimeBetweenActions().apply(this.level);
        this.maxStorage = minion.getGetMaximumStorage().apply(this.level);
    }

    public MiningMinion(MiningMinionType minion) {
        this(minion, UUID.randomUUID());
    }

    @Override
    public void spawn(SkyblockPlayer player, Location location, int level) {
        if (!location.getWorld().getName().startsWith(IslandManager.ISLAND_PREFIX)) return;

        Skyblock.getPlugin().getMinionHandler().initializeMinion(player, this, location);

        if (this.minion != null) this.minion.remove();

        this.level = level;

        this.resourcesGenerated = 0;
        this.timeBetweenActions = this.type.getTimeBetweenActions().apply(this.level);
        this.maxStorage = this.type.getGetMaximumStorage().apply(this.level);

        this.text = location.getWorld().spawn(location.clone().add(0, 1, 0), ArmorStand.class);
        this.text.setCustomName("");
        this.text.setCustomNameVisible(false);
        this.text.setGravity(false);
        this.text.setVisible(false);
        this.text.setSmall(true);
        this.text.setMarker(true);

        this.minion = location.getWorld().spawn(location, ArmorStand.class);
        this.minion.setCustomName("");
        this.minion.setCustomNameVisible(false);
        this.minion.setGravity(false);
        this.minion.setVisible(false);
        this.minion.setSmall(true);
        this.minion.setArms(true);
        this.minion.setBasePlate(false);
        this.minion.setCanPickupItems(false);

        ItemStack head = Util.IDtoSkull(new ItemBuilder("", Material.SKULL_ITEM, 1, (short) 3).toItemStack(), this.head.apply(this.level));
        this.minion.setHelmet(head);

        ItemStack hand = this.hand.apply(this.level);
        this.minion.setItemInHand(hand);

        this.minion.setChestplate(Util.colorLeatherArmor(new ItemBuilder("", Material.LEATHER_CHESTPLATE, 1).toItemStack(), this.leatherArmorColor));
        this.minion.setLeggings(Util.colorLeatherArmor(new ItemBuilder("", Material.LEATHER_LEGGINGS, 1).toItemStack(), this.leatherArmorColor));
        this.minion.setBoots(Util.colorLeatherArmor(new ItemBuilder("", Material.LEATHER_BOOTS, 1).toItemStack(), this.leatherArmorColor));

        this.minion.setMetadata("minion", new FixedMetadataValue(Skyblock.getPlugin(), true));
        this.minion.setMetadata("minion_id", new FixedMetadataValue(Skyblock.getPlugin(), this.uuid.toString()));

        this.text.setMetadata("minion", new FixedMetadataValue(Skyblock.getPlugin(), true));
        this.text.setMetadata("minion_id", new FixedMetadataValue(Skyblock.getPlugin(), this.uuid.toString()));

        this.i = 0;

        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                if (minion == null || minion.isDead()) {
                    cancel();
                    return;
                }

                int ticksBetweenActions = getTimeBetweenActions.apply(level) * 20;

                if (i >= ticksBetweenActions) {
                    i = 0;

                    tick(player, location);
                } else {
                    i++;
                }
            }
        };

        this.task.runTaskTimer(Skyblock.getPlugin(Skyblock.class), 0, 1);
    }

    @Override
    public void pickup(SkyblockPlayer player, Location location) {
        // TODO: Add pickup code + minion itemstack parser in ItemHandler
    }

    @Override
    protected void tick(SkyblockPlayer player, Location location) {
        List<Block> blocks = new ArrayList<>();

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                Block block = location.clone().add(x, -1, z).getBlock();

                blocks.add(block);
            }
        }

        List<Block> air = blocks.stream().filter(block -> block.getType().equals(Material.AIR)).collect(Collectors.toList());
        List<Block> ores = blocks.stream().filter(block -> block.getType().equals(this.type.getMaterial())).collect(Collectors.toList());

        if (air.size() != 0) {
            this.text.setCustomNameVisible(false);

            Block block = air.get(Skyblock.getPlugin().getRandom().nextInt(air.size()));

            Material toSet = this.type.getMaterial();

            block.setType(toSet);

            for (Player target : location.getWorld().getPlayers()) {
                PacketPlayOutBlockBreakAnimation packet = new PacketPlayOutBlockBreakAnimation(0, new BlockPosition(block.getX(), block.getY(), block.getZ()), 50);
                PacketPlayOutBlockChange packet1 = new PacketPlayOutBlockChange(((CraftWorld) location.getWorld()).getHandle(), new BlockPosition(block.getX(), block.getY(), block.getZ()));
                ((CraftPlayer) target).getHandle().playerConnection.sendPacket(packet);
                ((CraftPlayer) target).getHandle().playerConnection.sendPacket(packet1);
            }
        } else {
            Block block;
            try {
                block = ores.get(Skyblock.getPlugin().getRandom().nextInt(ores.size()));
            } catch (IllegalArgumentException ex) {
                this.text.setCustomName(ChatColor.RED + "I need more space!");
                this.text.setCustomNameVisible(true);

                return;
            }

            this.text.setCustomNameVisible(false);

            for (int i = 0; i < 10; i++) {
                int finalI = i;

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        PacketPlayOutBlockBreakAnimation packet = new PacketPlayOutBlockBreakAnimation(0, new BlockPosition(block.getX(), block.getY(), block.getZ()), finalI);
                        PacketPlayOutBlockChange packet2 = new PacketPlayOutBlockChange(((CraftWorld) location.getWorld()).getHandle(), new BlockPosition(block.getX(), block.getY(), block.getZ()));

                        for (Player target : location.getWorld().getPlayers()) ((CraftPlayer) target).getHandle().playerConnection.sendPacket(packet);

                        if (finalI == 9) {
                            block.setType(Material.AIR);

                            for (Player target : location.getWorld().getPlayers()) ((CraftPlayer) target).getHandle().playerConnection.sendPacket(packet2);

                            collect(player);
                        }
                    }
                }.runTaskLater(Skyblock.getPlugin(Skyblock.class), i * 10);
            }
        }
    }

    @Override
    public void collect(SkyblockPlayer player, int slot) {
        List<Integer> possibleSlots = new ArrayList<>();

        int slt = 21;
        for (int i = 0; i < 15; i++) {
            if (Math.floor(this.maxStorage / 64F) > i) possibleSlots.add(slt);

            if (slt == 25) {
                slt = 30;
            } else if (slt == 34) {
                slt = 39;
            } else {
                slt++;
            }
        }

        if (!possibleSlots.contains(slot)) return;

        int inventoryIndex = possibleSlots.indexOf(slot);

        ItemStack toCollect = this.inventory.get(inventoryIndex);

        if (toCollect == null || toCollect.getType().equals(Material.AIR)) return;

        if (player.getBukkitPlayer().getInventory().firstEmpty() == -1) {
            player.getBukkitPlayer().sendMessage(ChatColor.RED + "Your inventory does not have enough free space to add all items!");
            return;
        }

        player.getBukkitPlayer().getInventory().addItem(toCollect);

        this.inventory.set(inventoryIndex, new ItemStack(Material.AIR));

        player.getBukkitPlayer().updateInventory();
        this.showInventory(player);
    }

    @Override
    public void collect(SkyblockPlayer player) {
        ItemStack[] drops = this.type.getCalculateDrops().apply(this.level);

        List<ItemStack> newInventory = new ArrayList<>(this.inventory);

        for (ItemStack drop : drops) {
            if (drop == null || drop.getType().equals(Material.AIR)) continue;

            newInventory.add(drop);
        }

        if (newInventory.size() > (this.maxStorage / 64)) {
            this.text.setCustomName(ChatColor.RED + "My storage is fulL! :(");
            this.text.setCustomNameVisible(true);
            return;
        }

        this.inventory = newInventory;

        for (ItemStack drop : drops) {
            this.resourcesGenerated += drop.getAmount();
        }

        this.text.setCustomNameVisible(false);
    }

    @Override
    public void upgrade(SkyblockPlayer player, int level) {
        // TODO: Implement upgrade system
    }

    @Override
    protected void showInventory(SkyblockPlayer player) {
        this.gui = Bukkit.createInventory(null, 54, StringUtils.capitalize(this.type.name().toLowerCase()) + " Minion " + Util.toRoman(this.level));

        Util.fillEmpty(this.gui);

        this.gui.setItem(4, MinionHandler.createMinionPreview.apply(this));

        this.gui.setItem(3, MinionHandler.MINION_INVENTORY_IDEAL_LAYOUT);
        this.gui.setItem(53, MinionHandler.MINION_INVENTORY_PICKUP_MINION);

        this.gui.setItem(10, MinionHandler.MINION_INVENTORY_UPGRADE_SKIN_SLOT);
        this.gui.setItem(19, MinionHandler.MINION_INVENTORY_UPGRADE_FUEL_SLOT);
        this.gui.setItem(28, MinionHandler.MINION_INVENTORY_UPGRADE_AUTOMATED_SHIPPING_SLOT);
        this.gui.setItem(37, MinionHandler.MINION_INVENTORY_UPGRADE_SLOT);
        this.gui.setItem(46, MinionHandler.MINION_INVENTORY_UPGRADE_SLOT);
        this.gui.setItem(48, MinionHandler.MINION_INVENTORY_COLLECT_ALL);

        int slot = 21;
        for (int i = 0; i < 15; i++) {
            if (Math.floor(this.maxStorage / 64F) > i) {
                this.gui.setItem(slot, new ItemStack(Material.AIR));
            } else {
                this.gui.setItem(slot, new ItemBuilder(ChatColor.YELLOW + "Storage unlocked at tier " + Util.toRoman(this.type.getLevelRequirementForStorageSlot().apply(i)), Material.STAINED_GLASS_PANE).toItemStack());
            }

            if (slot == 25) {
                slot = 30;
            } else if (slot == 34) {
                slot = 39;
            } else {
                slot++;
            }
        }

        for (ItemStack item : this.inventory) {
            this.gui.addItem(item);
        }

        player.getBukkitPlayer().openInventory(this.gui);
    }

}
