package cota.milhas.gravePlugin;

import org.bukkit.GameMode;
import org.bukkit.block.Chest;
import org.bukkit.block.data.Rotatable;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.Skull;
import net.kyori.adventure.text.Component;

import java.time.format.DateTimeFormatter;
import java.util.*;

public final class GravePlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("Grave Plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Grave Plugin disabled!");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (!block.hasMetadata("graveBlock")) return;

        event.setDropItems(false);

        if (block.getType() == Material.CHEST) {
            if (block.getState() instanceof Chest chest) {
                for (ItemStack item : chest.getBlockInventory().getContents()) {
                    if (item != null && item.getType() != Material.AIR) {
                        block.getWorld().dropItemNaturally(block.getLocation(), item);
                    }
                }
                chest.getBlockInventory().clear();
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
            return;
        }

        if (event.getKeepInventory()) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        boolean hasItems = false;

        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                hasItems = true;
                break;
            }
        }

        if (!hasItems) {
            return;
        }

        Location loc = player.getLocation();
        event.getDrops().clear();

        Block block1 = loc.getBlock();
        Block block2 = block1.getRelative(BlockFace.EAST);

        block1.setType(Material.CHEST);
        block2.setType(Material.CHEST);

        block1.setMetadata("graveBlock", new FixedMetadataValue(this, true));
        block2.setMetadata("graveBlock", new FixedMetadataValue(this, true));

        org.bukkit.block.data.type.Chest chestData1 = (org.bukkit.block.data.type.Chest) block1.getBlockData();
        chestData1.setType(org.bukkit.block.data.type.Chest.Type.LEFT);
        block1.setBlockData(chestData1, true);

        org.bukkit.block.data.type.Chest chestData2 = (org.bukkit.block.data.type.Chest) block2.getBlockData();
        chestData2.setType(org.bukkit.block.data.type.Chest.Type.RIGHT);
        block2.setBlockData(chestData2, true);

        if (block1.getState() instanceof org.bukkit.block.Chest chest) {
            org.bukkit.inventory.Inventory chestInv = chest.getInventory();

            int chestSlot = 0;

            for (int i = 9; i < 36; i++) {
                ItemStack item = inventory.getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    chestInv.setItem(chestSlot, item.clone());
                }
                chestSlot++;
            }

            for (int i = 0; i < 9; i++) {
                ItemStack item = inventory.getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    chestInv.setItem(chestSlot, item.clone());
                }
                chestSlot++;
            }

            ItemStack helmet = inventory.getHelmet();
            if (helmet != null && helmet.getType() != Material.AIR) {
                chestInv.setItem(chestInv.getSize() - 5, helmet.clone());
            }

            ItemStack chestplate = inventory.getChestplate();
            if (chestplate != null && chestplate.getType() != Material.AIR) {
                chestInv.setItem(chestInv.getSize() - 4, chestplate.clone());
            }

            ItemStack leggings = inventory.getLeggings();
            if (leggings != null && leggings.getType() != Material.AIR) {
                chestInv.setItem(chestInv.getSize() - 3, leggings.clone());
            }

            ItemStack boots = inventory.getBoots();
            if (boots != null && boots.getType() != Material.AIR) {
                chestInv.setItem(chestInv.getSize() - 2, boots.clone());
            }

            ItemStack offhand = inventory.getItemInOffHand();
            if (offhand.getType() != Material.AIR) {
                chestInv.setItem(chestInv.getSize() - 1, offhand.clone());
            }

            inventory.clear();
        }

        Block signBlock = block2.getRelative(BlockFace.EAST);
        signBlock.setType(Material.OAK_WALL_SIGN);
        signBlock.setMetadata("graveBlock", new FixedMetadataValue(this, true));

        BlockData data = signBlock.getBlockData();
        if (data instanceof Directional directional) {
            directional.setFacing(BlockFace.EAST);
            signBlock.setBlockData(directional, false);
        }

        Block headBlock = block2.getRelative(BlockFace.UP);
        headBlock.setType(Material.PLAYER_HEAD);
        headBlock.setMetadata("graveBlock", new FixedMetadataValue(this, true));

        if (headBlock.getBlockData() instanceof Rotatable rotatable) {
            rotatable.setRotation(BlockFace.WEST);
            headBlock.setBlockData(rotatable);
        }

        if (headBlock.getState() instanceof Skull skull) {
            skull.setOwningPlayer(player);
            skull.update();
            headBlock.setMetadata("graveHead", new FixedMetadataValue(this, true));
        }

        if (signBlock.getState() instanceof org.bukkit.block.Sign signState) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            var front = signState.getSide(org.bukkit.block.sign.Side.FRONT);

            front.line(0, Component.text("§cRIP"));
            front.line(1, Component.text(player.getName()));
            front.line(2, Component.text(dtf.format(java.time.LocalDateTime.now())));

            signState.update();
        }

        player.sendMessage("§eYou died at" +
                " §cX:" + loc.getBlockX() +
                " Y:" + loc.getBlockY() +
                " Z:" + loc.getBlockZ());
    }
}
