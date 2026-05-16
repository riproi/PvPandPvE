package me.jules.pvppve;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class ModeListener implements Listener {
    private final PvpPvePlugin plugin;
    private final PlayerManager playerManager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ModeListener(PvpPvePlugin plugin, PlayerManager playerManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerManager.setMode(player, PlayerMode.NONE);

        // Remove tracking compass if exists
        player.getInventory().remove(Material.COMPASS);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            playerManager.openSelectionGui(player);
        }, 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playerManager.removePlayer(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        if (playerManager.getMode(player) == PlayerMode.NONE) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                playerManager.openSelectionGui(player);
            }, 1L);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (playerManager.getMode(player) == PlayerMode.NONE) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == Material.AIR) return;

            if (item.getType() == Material.DIAMOND_SWORD) {
                playerManager.setMode(player, PlayerMode.PVP);
                player.sendMessage(mm.deserialize(plugin.getConfig().getString("messages.mode-pvp-selected")));
                playerManager.giveCompass(player);
                player.closeInventory();
            } else if (item.getType() == Material.GRASS_BLOCK) {
                playerManager.setMode(player, PlayerMode.PVE);
                player.sendMessage(mm.deserialize(plugin.getConfig().getString("messages.mode-pve-selected")));
                player.closeInventory();
            }
        }
    }
}
