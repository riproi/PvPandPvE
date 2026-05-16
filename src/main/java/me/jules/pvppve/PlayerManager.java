package me.jules.pvppve;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayerManager {
    private final PvpPvePlugin plugin;
    private final HashMap<UUID, PlayerMode> playerModes = new HashMap<>();
    private final MiniMessage mm = MiniMessage.miniMessage();
    public static final NamespacedKey COMPASS_KEY = new NamespacedKey("pvppve", "tracking_compass");

    public PlayerManager(PvpPvePlugin plugin) {
        this.plugin = plugin;
    }

    public void setMode(Player player, PlayerMode mode) {
        playerModes.put(player.getUniqueId(), mode);
    }

    public PlayerMode getMode(Player player) {
        return playerModes.getOrDefault(player.getUniqueId(), PlayerMode.NONE);
    }

    public void removePlayer(Player player) {
        playerModes.remove(player.getUniqueId());
    }

    public void openSelectionGui(Player player) {
        String title = plugin.getConfig().getString("gui.selection-title", "Выберите режим игры");
        Inventory gui = Bukkit.createInventory(null, 9, mm.deserialize(title));

        ItemStack pvpItem = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta pvpMeta = pvpItem.getItemMeta();
        pvpMeta.displayName(mm.deserialize(plugin.getConfig().getString("gui.pvp-item.name")));
        List<Component> pvpLore = plugin.getConfig().getStringList("gui.pvp-item.lore").stream()
                .map(mm::deserialize).collect(Collectors.toList());
        pvpMeta.lore(pvpLore);
        pvpItem.setItemMeta(pvpMeta);

        ItemStack pveItem = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta pveMeta = pveItem.getItemMeta();
        pveMeta.displayName(mm.deserialize(plugin.getConfig().getString("gui.pve-item.name")));
        List<Component> pveLore = plugin.getConfig().getStringList("gui.pve-item.lore").stream()
                .map(mm::deserialize).collect(Collectors.toList());
        pveMeta.lore(pveLore);
        pveItem.setItemMeta(pveMeta);

        gui.setItem(3, pvpItem);
        gui.setItem(5, pveItem);

        player.openInventory(gui);
    }

    public void giveCompass(Player player) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        meta.displayName(mm.deserialize(plugin.getConfig().getString("items.compass-name")));
        List<Component> lore = plugin.getConfig().getStringList("items.compass-lore").stream()
                .map(mm::deserialize).collect(Collectors.toList());
        meta.lore(lore);
        meta.getPersistentDataContainer().set(COMPASS_KEY, PersistentDataType.BYTE, (byte) 1);
        compass.setItemMeta(meta);

        player.getInventory().setItem(8, compass);
    }
}
