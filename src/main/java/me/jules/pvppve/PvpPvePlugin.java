package me.jules.pvppve;

import org.bukkit.plugin.java.JavaPlugin;

public class PvpPvePlugin extends JavaPlugin {
    private PlayerManager playerManager;
    private CompassManager compassManager;
    private CombatManager combatManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.playerManager = new PlayerManager(this);
        this.compassManager = new CompassManager(this, playerManager);
        this.combatManager = new CombatManager(this);

        getServer().getPluginManager().registerEvents(new ModeListener(this, playerManager), this);
        getServer().getPluginManager().registerEvents(new DamageListener(this, playerManager), this);
        getServer().getPluginManager().registerEvents(new CompassListener(this, playerManager, compassManager), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this, playerManager, combatManager), this);

        getLogger().info("PvpPvePlugin enabled!");
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    @Override
    public void onDisable() {
        getLogger().info("PvpPvePlugin disabled!");
    }
}
