package me.jules.pvppve;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CombatManager {
    private final PvpPvePlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<UUID, Long> combatTimers = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> opponents = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();
    private final Map<UUID, Scoreboard> oldScoreboards = new ConcurrentHashMap<>();

    public CombatManager(PvpPvePlugin plugin) {
        this.plugin = plugin;
        startCombatTask();
    }

    public void enterCombat(Player player, Player opponent) {
        boolean alreadyInCombat = combatTimers.containsKey(player.getUniqueId());
        combatTimers.put(player.getUniqueId(), System.currentTimeMillis());
        opponents.put(player.getUniqueId(), opponent.getUniqueId());

        if (!alreadyInCombat) {
            player.sendMessage(mm.deserialize(plugin.getConfig().getString("messages.combat-started-chat")));

            String titleStr = plugin.getConfig().getString("messages.combat-started-title");
            String subtitleStr = plugin.getConfig().getString("messages.combat-started-subtitle").replace("{player}", opponent.getName());
            Title title = Title.title(mm.deserialize(titleStr), mm.deserialize(subtitleStr),
                    Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1000), Duration.ofMillis(100)));
            player.showTitle(title);

            setupBossBar(player);
            setupScoreboard(player, opponent);
        }
    }

    private void setupBossBar(Player player) {
        BossBar bar = BossBar.bossBar(mm.deserialize("Combat"), 1f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
        player.showBossBar(bar);
        bossBars.put(player.getUniqueId(), bar);
    }

    private void setupScoreboard(Player player, Player opponent) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (!oldScoreboards.containsKey(player.getUniqueId())) {
            oldScoreboards.put(player.getUniqueId(), player.getScoreboard());
        }

        Scoreboard board = manager.getNewScoreboard();
        Objective obj = board.registerNewObjective("combat", Criteria.DUMMY, mm.deserialize("<red>В БОЮ"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        obj.getScore(opponent.getName()).setScore(1);
        obj.getScore("HP: " + (int)opponent.getHealth()).setScore(0);

        player.setScoreboard(board);
    }

    private void updateScoreboard(Player player, Player opponent) {
        Scoreboard board = player.getScoreboard();
        Objective obj = board.getObjective("combat");
        if (obj == null) {
            setupScoreboard(player, opponent);
            return;
        }
        board.getEntries().forEach(board::resetScores);
        obj.getScore(opponent.getName()).setScore(1);
        obj.getScore("HP: " + (int)opponent.getHealth()).setScore(0);
    }

    private void startCombatTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            int combatTimeLimit = plugin.getConfig().getInt("combat-time", 30) * 1000;
            Set<UUID> toRemove = new HashSet<>();

            for (Map.Entry<UUID, Long> entry : combatTimers.entrySet()) {
                UUID uuid = entry.getKey();
                Player player = Bukkit.getPlayer(uuid);
                long elapsed = now - entry.getValue();

                if (elapsed > combatTimeLimit) {
                    toRemove.add(uuid);
                } else {
                    if (player != null && player.isOnline()) {
                        updateBossBar(player, (combatTimeLimit - elapsed) / 1000);
                        Player opponent = Bukkit.getPlayer(opponents.get(uuid));
                        if (opponent != null && opponent.isOnline()) {
                             updateScoreboard(player, opponent);
                        }
                    }
                }
            }

            for (UUID uuid : toRemove) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    exitCombatInternal(player);
                } else {
                    // Clean up memory if player offline
                    combatTimers.remove(uuid);
                    opponents.remove(uuid);
                    bossBars.remove(uuid);
                    oldScoreboards.remove(uuid);
                }
            }
        }, 20L, 20L);
    }

    private void updateBossBar(Player player, long secondsLeft) {
        BossBar bar = bossBars.get(player.getUniqueId());
        if (bar != null) {
            String text = plugin.getConfig().getString("messages.bossbar-text").replace("{time}", String.valueOf(secondsLeft));
            bar.name(mm.deserialize(text));
            float progress = (float) secondsLeft / plugin.getConfig().getInt("combat-time", 30);
            bar.progress(Math.max(0f, Math.min(1f, progress)));
        }
    }

    public void exitCombat(Player player) {
        UUID opponentId = opponents.get(player.getUniqueId());
        exitCombatInternal(player);

        // If this method was called (e.g. because of death/quit), we also end combat for the opponent
        if (opponentId != null) {
            Player opponent = Bukkit.getPlayer(opponentId);
            if (opponent != null && opponent.isOnline() && isInCombat(opponent)) {
                // Only if they are fighting each other
                if (player.getUniqueId().equals(opponents.get(opponent.getUniqueId()))) {
                    exitCombatInternal(opponent);
                }
            }
        }
    }

    private void exitCombatInternal(Player player) {
        combatTimers.remove(player.getUniqueId());
        opponents.remove(player.getUniqueId());

        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }

        Scoreboard old = oldScoreboards.remove(player.getUniqueId());
        if (old != null) {
            player.setScoreboard(old);
        } else {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    public boolean isInCombat(Player player) {
        return combatTimers.containsKey(player.getUniqueId());
    }
}
