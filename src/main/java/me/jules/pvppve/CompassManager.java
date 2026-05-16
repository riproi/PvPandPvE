package me.jules.pvppve;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

public class CompassManager {
    private final PvpPvePlugin plugin;
    private final PlayerManager playerManager;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final HashMap<UUID, UUID> targets = new HashMap<>();
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();

    public CompassManager(PvpPvePlugin plugin, PlayerManager playerManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        startTrackingTask();
    }

    public void openHeadsGui(Player player) {
        String title = plugin.getConfig().getString("gui.heads-title", "Выберите цель для охоты");
        Inventory gui = Bukkit.createInventory(null, 54, mm.deserialize(title));

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.equals(player)) continue;
            if (playerManager.getMode(onlinePlayer) == PlayerMode.PVP) {
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                meta.setOwningPlayer(onlinePlayer);
                meta.displayName(onlinePlayer.name());
                head.setItemMeta(meta);
                gui.addItem(head);
            }
        }

        player.openInventory(gui);
    }

    public void setTarget(Player hunter, Player victim) {
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(hunter.getUniqueId())) {
            long remaining = (cooldowns.get(hunter.getUniqueId()) + 300000) - now;
            if (remaining > 0) {
                String timeStr = formatTime(remaining);
                hunter.sendMessage(mm.deserialize(plugin.getConfig().getString("messages.cooldown-wait").replace("{time}", timeStr)));
                return;
            }
        }

        targets.put(hunter.getUniqueId(), victim.getUniqueId());
        cooldowns.put(hunter.getUniqueId(), now);
        hunter.setCompassTarget(victim.getLocation());
        hunter.sendMessage(mm.deserialize(plugin.getConfig().getString("messages.target-set").replace("{player}", victim.getName())));
        hunter.closeInventory();
    }

    private String formatTime(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        seconds %= 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void startTrackingTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID hunterId : targets.keySet()) {
                    Player hunter = Bukkit.getPlayer(hunterId);
                    if (hunter == null || !hunter.isOnline()) continue;

                    UUID victimId = targets.get(hunterId);
                    Player victim = Bukkit.getPlayer(victimId);

                    if (victim != null && victim.isOnline() && playerManager.getMode(victim) == PlayerMode.PVP) {
                        hunter.setCompassTarget(victim.getLocation());
                    } else {
                        // Victim offline or changed mode? Maybe reset?
                    }
                }
            }
        }.runTaskTimer(plugin, 1800L, 1800L); // 1.5 minutes = 1800 ticks
    }

    public void removeHunter(Player player) {
        targets.remove(player.getUniqueId());
        cooldowns.remove(player.getUniqueId());
    }
}
