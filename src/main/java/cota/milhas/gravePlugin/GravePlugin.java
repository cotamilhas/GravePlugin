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
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.Skull;
import net.kyori.adventure.text.Component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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

        if (block.getType() == Material.CHEST && block.getState() instanceof Chest chest) {
            for (ItemStack item : chest.getBlockInventory().getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    block.getWorld().dropItemNaturally(block.getLocation(), item);
                }
            }
            chest.getBlockInventory().clear();
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) return;
        if (event.getKeepInventory()) return;

        boolean hasDrops = event.getDrops().stream().anyMatch(i -> i != null && i.getType() != Material.AIR);
        if (!hasDrops) return;

        Location loc = findSpot(player.getLocation());
        if (loc == null) {
            getLogger().warning("Could not create a grave, items will drop on the ground (vanilla).");
            return;
        }

        List<ItemStack> dropsCopy = new ArrayList<>(event.getDrops());
        event.getDrops().clear();

        // Armazena os itens para uso posterior
        List<ItemStack> itemsToStore = new ArrayList<>();

        // Coleta todos os itens do inventário do jogador
        var inventory = player.getInventory();

        for (int i = 9; i < 36; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                itemsToStore.add(item.clone());
            }
        }

        for (int i = 0; i < 9; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                itemsToStore.add(item.clone());
            }
        }

        ItemStack helmet = inventory.getHelmet();
        if (helmet != null && helmet.getType() != Material.AIR) {
            itemsToStore.add(helmet.clone());
        }

        ItemStack chestplate = inventory.getChestplate();
        if (chestplate != null && chestplate.getType() != Material.AIR) {
            itemsToStore.add(chestplate.clone());
        }

        ItemStack leggings = inventory.getLeggings();
        if (leggings != null && leggings.getType() != Material.AIR) {
            itemsToStore.add(leggings.clone());
        }

        ItemStack boots = inventory.getBoots();
        if (boots != null && boots.getType() != Material.AIR) {
            itemsToStore.add(boots.clone());
        }

        ItemStack offhand = inventory.getItemInOffHand();
        if (offhand != null && offhand.getType() != Material.AIR) {
            itemsToStore.add(offhand.clone());
        }

        inventory.clear();

        Block leftBlock = loc.getBlock();
        Block rightBlock = leftBlock.getRelative(BlockFace.EAST);

        leftBlock.setType(Material.CHEST);
        rightBlock.setType(Material.CHEST);

        leftBlock.setMetadata("graveBlock", new FixedMetadataValue(this, true));
        rightBlock.setMetadata("graveBlock", new FixedMetadataValue(this, true));

        Bukkit.getScheduler().runTask(this, () -> {
            try {
                org.bukkit.block.data.type.Chest leftData = (org.bukkit.block.data.type.Chest) Bukkit.createBlockData(Material.CHEST);
                org.bukkit.block.data.type.Chest rightData = (org.bukkit.block.data.type.Chest) Bukkit.createBlockData(Material.CHEST);

                leftData.setFacing(BlockFace.NORTH);
                rightData.setFacing(BlockFace.NORTH);

                leftData.setType(org.bukkit.block.data.type.Chest.Type.LEFT);
                rightData.setType(org.bukkit.block.data.type.Chest.Type.RIGHT);

                leftBlock.setBlockData(leftData, true);
                rightBlock.setBlockData(rightData, true);

                // Aguarda a atualização do bloco
                Chest leftChest = (Chest) leftBlock.getState();

                // Adiciona os itens ao baú
                int slot = 0;
                for (ItemStack item : itemsToStore) {
                    if (slot >= leftChest.getInventory().getSize()) break;
                    leftChest.getInventory().setItem(slot, item);
                    slot++;
                }
                leftChest.update();

            } catch (Exception e) {
                getLogger().warning("Failed to configure double chest. Items will drop on the ground. Error: " + e.getMessage());
                for (ItemStack item : itemsToStore) {
                    if (item != null && item.getType() != Material.AIR) {
                        player.getWorld().dropItemNaturally(loc, item);
                    }
                }
                leftBlock.setType(Material.AIR);
                rightBlock.setType(Material.AIR);
                return;
            }

            Block signBlock = rightBlock.getRelative(BlockFace.EAST);
            Block headBlock = rightBlock.getRelative(BlockFace.UP);

            if (signBlock.getType().isAir()) {
                signBlock.setType(Material.OAK_WALL_SIGN);
                signBlock.setMetadata("graveBlock", new FixedMetadataValue(this, true));
                BlockData data = signBlock.getBlockData();
                if (data instanceof Directional directional) {
                    directional.setFacing(BlockFace.EAST);
                    signBlock.setBlockData(directional, false);
                }

                // Atualiza o estado do sign
                if (signBlock.getState() instanceof org.bukkit.block.Sign signState) {
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    var front = signState.getSide(org.bukkit.block.sign.Side.FRONT);
                    front.line(0, Component.text("§cRIP"));
                    front.line(1, Component.text(player.getName()));
                    front.line(2, Component.text(dtf.format(java.time.LocalDateTime.now())));
                    signState.update();
                }
            }

            if (headBlock.getType().isAir()) {
                headBlock.setType(Material.PLAYER_HEAD);
                headBlock.setMetadata("graveBlock", new FixedMetadataValue(this, true));
                if (headBlock.getBlockData() instanceof Rotatable rotatable) {
                    rotatable.setRotation(BlockFace.WEST);
                    headBlock.setBlockData(rotatable);
                }

                // Atualiza o estado da cabeça
                if (headBlock.getState() instanceof Skull skull) {
                    skull.setOwningPlayer(player);
                    skull.update();
                }
            }

            player.sendMessage("§eYou died at §cX:" + loc.getBlockX() + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ());
        });
    }

    private Location findSpot(Location deathLoc) {
        int radius = 5;
        int minY = deathLoc.getWorld().getMinHeight();
        int maxY = deathLoc.getWorld().getMaxHeight() - 2;

        int[] dyOptions = {0, 1, -1, 2, -2};

        for (int dy : dyOptions) {
            for (int r = 0; r <= radius; r++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        Location cand = deathLoc.clone().add(dx, dy, dz);
                        int y = cand.getBlockY();
                        if (y <= minY || y >= maxY) continue;

                        if (isAirSpaceForGrave(cand)) {
                            return cand;
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean isAirSpaceForGrave(Location base) {
        Block ground = base.getBlock().getRelative(BlockFace.DOWN);
        if (!ground.getType().isSolid() || isContainer(ground.getType())) return false;

        Block chest1 = base.getBlock();
        Block chest2 = chest1.getRelative(BlockFace.EAST);
        Block sign = chest2.getRelative(BlockFace.EAST);
        Block head = chest2.getRelative(BlockFace.UP);

        return chest1.getType().isAir()
                && chest2.getType().isAir()
                && sign.getType().isAir()
                && head.getType().isAir();
    }

    private boolean isContainer(Material mat) {
        return mat == Material.CHEST
                || mat == Material.TRAPPED_CHEST
                || mat == Material.BARREL
                || mat.name().endsWith("SHULKER_BOX")
                || mat == Material.ENDER_CHEST;
    }
}