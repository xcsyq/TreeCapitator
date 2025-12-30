package com.treecapitator;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class TreeBreakListener implements Listener {

    private final TreeCapitator plugin;
    private final Set<Material> logs = new HashSet<>();
    private final Set<Material> axes = new HashSet<>();
    private final Set<UUID> activeChops = new HashSet<>();

    public TreeBreakListener(TreeCapitator plugin) {
        this.plugin = plugin;
        initMaterials();
    }

    private void initMaterials() {
        logs.add(Material.OAK_LOG);
        logs.add(Material.SPRUCE_LOG);
        logs.add(Material.BIRCH_LOG);
        logs.add(Material.JUNGLE_LOG);
        logs.add(Material.ACACIA_LOG);
        logs.add(Material.DARK_OAK_LOG);
        logs.add(Material.MANGROVE_LOG);
        logs.add(Material.CHERRY_LOG);
        logs.add(Material.CRIMSON_STEM);
        logs.add(Material.WARPED_STEM);
        logs.add(Material.STRIPPED_OAK_LOG);
        logs.add(Material.STRIPPED_SPRUCE_LOG);
        logs.add(Material.STRIPPED_BIRCH_LOG);
        logs.add(Material.STRIPPED_JUNGLE_LOG);
        logs.add(Material.STRIPPED_ACACIA_LOG);
        logs.add(Material.STRIPPED_DARK_OAK_LOG);
        logs.add(Material.STRIPPED_MANGROVE_LOG);
        logs.add(Material.STRIPPED_CHERRY_LOG);
        logs.add(Material.STRIPPED_CRIMSON_STEM);
        logs.add(Material.STRIPPED_WARPED_STEM);
        axes.add(Material.WOODEN_AXE);
        axes.add(Material.STONE_AXE);
        axes.add(Material.IRON_AXE);
        axes.add(Material.GOLDEN_AXE);
        axes.add(Material.DIAMOND_AXE);
        axes.add(Material.NETHERITE_AXE);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        UUID playerId = player.getUniqueId();
        if (activeChops.contains(playerId)) return;
        if (!logs.contains(block.getType())) return;
        ItemStack tool = player.getInventory().getItemInMainHand();
        boolean requireAxe = plugin.getConfig().getBoolean("require-axe", true);
        boolean requireSneak = plugin.getConfig().getBoolean("require-sneak", false);
        if (requireAxe && !axes.contains(tool.getType())) return;
        if (requireSneak && !player.isSneaking()) return;
        if (!player.hasPermission("treecapitator.use")) return;
        Material logType = block.getType();
        int maxBlocks = plugin.getConfig().getInt("max-blocks", 256);
        Set<Block> treeLogs = findConnectedLogs(block, logType, maxBlocks);
        if (treeLogs.size() <= 1) return;
        activeChops.add(playerId);
        try {
            int blocksBroken = 0;
            for (Block log : treeLogs) {
                if (log.equals(block)) continue;
                if (requireAxe && isToolBroken(tool)) break;
                log.breakNaturally(tool);
                blocksBroken++;
                if (player.getGameMode() != GameMode.CREATIVE && plugin.getConfig().getBoolean("apply-durability", true)) {
                    applyDurability(tool, player);
                }
            }
            if (blocksBroken > 0) {
                player.playSound(player.getLocation(), Sound.BLOCK_WOOD_BREAK, 1.0f, 1.0f);
            }
        } finally {
            activeChops.remove(playerId);
        }
    }

    private Set<Block> findConnectedLogs(Block start, Material logType, int maxBlocks) {
        Set<Block> foundLogs = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();
        queue.add(start);
        while (!queue.isEmpty() && foundLogs.size() < maxBlocks) {
            Block current = queue.poll();
            if (foundLogs.contains(current)) continue;
            if (!isSameLogType(current.getType(), logType)) continue;
            foundLogs.add(current);
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;
                        Block adjacent = current.getRelative(x, y, z);
                        if (!foundLogs.contains(adjacent) && isSameLogType(adjacent.getType(), logType)) {
                            queue.add(adjacent);
                        }
                    }
                }
            }
        }
        return foundLogs;
    }

    private boolean isSameLogType(Material type1, Material type2) {
        String name1 = type1.name().replace("STRIPPED_", "");
        String name2 = type2.name().replace("STRIPPED_", "");
        return name1.equals(name2);
    }

    private void applyDurability(ItemStack tool, Player player) {
        if (tool == null || tool.getType().isAir()) return;
        ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable)) return;
        Damageable damageable = (Damageable) meta;
        int unbreakingLevel = tool.getEnchantmentLevel(Enchantment.UNBREAKING);
        if (unbreakingLevel > 0) {
            double chance = 1.0 / (unbreakingLevel + 1);
            if (Math.random() > chance) return;
        }
        damageable.setDamage(damageable.getDamage() + 1);
        tool.setItemMeta(meta);
        if (damageable.getDamage() >= tool.getType().getMaxDurability()) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
        }
    }

    private boolean isToolBroken(ItemStack tool) {
        if (tool == null || tool.getType().isAir()) return true;
        ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable)) return false;
        Damageable damageable = (Damageable) meta;
        return damageable.getDamage() >= tool.getType().getMaxDurability();
    }
}
