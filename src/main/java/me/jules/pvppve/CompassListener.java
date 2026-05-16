package me.jules.pvppve;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class CompassListener implements Listener {
    private final PvpPvePlugin plugin;
    private final PlayerManager playerManager;
    private final CompassManager compassManager;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final PlainTextComponentSerializer pts = PlainTextComponentSerializer.plainText();

    public CompassListener(PvpPvePlugin plugin, PlayerManager playerManager, CompassManager compassManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.compassManager = compassManager;
    }

    private boolean isTrackingCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(PlayerManager.COMPASS_KEY, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction().isRightClick() && isTrackingCompass(event.getItem())) {
            compassManager.openHeadsGui(event.getPlayer());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().title().equals(mm.deserialize(plugin.getConfig().getString("gui.heads-title")))) {
            event.setCancelled(true);
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                Player hunter = (Player) event.getWhoClicked();
                ItemMeta meta = event.getCurrentItem().getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    String targetName = pts.serialize(meta.displayName());
                    Player victim = plugin.getServer().getPlayer(targetName);
                    if (victim != null) {
                        compassManager.setTarget(hunter, victim);
                    }
                }
            }
            return;
        }

        // Protect compass from moving
        if (isTrackingCompass(event.getCurrentItem()) || isTrackingCompass(event.getCursor())) {
             event.setCancelled(true);
             return;
        }

        // Block interaction with slot 8 if it contains the compass (in player inventory)
        if (event.getSlot() == 8 && event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.PLAYER) {
            if (isTrackingCompass(event.getClickedInventory().getItem(8))) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isTrackingCompass(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(this::isTrackingCompass);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (playerManager.getMode(player) == PlayerMode.PVP) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                playerManager.giveCompass(player);
            }, 1L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        compassManager.removeHunter(event.getPlayer());
        event.getPlayer().getInventory().remove(Material.COMPASS);
    }
}
