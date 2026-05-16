package me.jules.pvppve;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class DamageListener implements Listener {
    private final PvpPvePlugin plugin;
    private final PlayerManager playerManager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public DamageListener(PvpPvePlugin plugin, PlayerManager playerManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player p) {
            attacker = p;
        }

        if (attacker == null) return;

        PlayerMode victimMode = playerManager.getMode(victim);
        PlayerMode attackerMode = playerManager.getMode(attacker);

        if (victimMode == PlayerMode.PVE) {
            attacker.sendMessage(mm.deserialize(plugin.getConfig().getString("messages.cannot-damage-pve")));
            event.setCancelled(true);
            return;
        }

        if (attackerMode == PlayerMode.PVE) {
            attacker.sendMessage(mm.deserialize(plugin.getConfig().getString("messages.pve-cannot-damage")));
            event.setCancelled(true);
            return;
        }

        if (victimMode == PlayerMode.NONE || attackerMode == PlayerMode.NONE) {
            event.setCancelled(true);
        }
    }
}
