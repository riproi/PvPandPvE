package me.jules.pvppve;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

public class CombatListener implements Listener {
    private final PvpPvePlugin plugin;
    private final PlayerManager playerManager;
    private final CombatManager combatManager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public CombatListener(PvpPvePlugin plugin, PlayerManager playerManager, CombatManager combatManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.combatManager = combatManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player p) {
            attacker = p;
        }

        if (attacker == null || attacker.equals(victim)) return;

        if (playerManager.getMode(victim) == PlayerMode.PVP && playerManager.getMode(attacker) == PlayerMode.PVP) {
            combatManager.enterCombat(victim, attacker);
            combatManager.enterCombat(attacker, victim);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (combatManager.isInCombat(player)) {
            player.setHealth(0); // Kill player on logout
            // exitCombat will be called by onDeath if health is set to 0,
            // but for safety we call it here too if death event doesn't trigger immediately
            combatManager.exitCombat(player);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (combatManager.isInCombat(player)) {
            combatManager.exitCombat(player);
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (combatManager.isInCombat(player)) {
            String message = event.getMessage().split(" ")[0].toLowerCase();
            List<String> blocked = plugin.getConfig().getStringList("blocked-commands");
            if (blocked.contains(message)) {
                event.setCancelled(true);
                player.sendMessage(mm.deserialize(plugin.getConfig().getString("messages.command-blocked")));
            }
        }
    }

    @EventHandler
    public void onCommandSend(PlayerCommandSendEvent event) {
        if (combatManager.isInCombat(event.getPlayer())) {
            List<String> blocked = plugin.getConfig().getStringList("blocked-commands");
            event.getCommands().removeIf(cmd -> blocked.contains("/" + cmd.toLowerCase()));
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory().getType() == InventoryType.ENDER_CHEST) {
            Player player = (Player) event.getPlayer();
            if (combatManager.isInCombat(player)) {
                event.setCancelled(true);
                player.sendMessage(mm.deserialize(plugin.getConfig().getString("messages.enderchest-blocked")));
            }
        }
    }
}
