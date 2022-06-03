package me.saif.blockcooldown;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BlockCooldown extends JavaPlugin implements Listener {

    private final Map<UUID, Long> blockCooldownMap = new HashMap<>();
    private final Map<UUID, Long> crystalCooldownMap = new HashMap<>();
    private final Map<UUID, Long> toCooldown = new HashMap<>();

    private long enderCrystalCooldownTime;
    private long groupCooldownTime;
    private long blockPlaceCooldownTime;

    private String blockPlaceCooldownMessage;
    private String crystalExplodeCooldownMessage;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        this.enderCrystalCooldownTime = this.getConfig().getLong("crystal-explode-cooldown-time", 3000L);
        this.groupCooldownTime = this.getConfig().getLong("group-cooldown-time", 10000L);
        this.blockPlaceCooldownTime = this.getConfig().getLong("block-place-cooldown-time", 3000L);

        this.blockPlaceCooldownMessage = ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("block-place-cooldown-message", "&4You cannot place a block for the next <remaining>."));
        this.crystalExplodeCooldownMessage = ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("crystal-explode-cooldown-message", "&4You cannot explode an end crystal for the next <remaining>."));

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onBlockPlace(BlockPlaceEvent event) {
        if (!isInGroup(event.getPlayer()))
            return;

        Player player = event.getPlayer();
        long lastUse = this.blockCooldownMap.getOrDefault(event.getPlayer().getUniqueId(), 0L);
        long remaining = this.blockPlaceCooldownTime - (System.currentTimeMillis() - lastUse);
        if (remaining <= 0 || player.hasPermission("blockcooldown.bypass")) {
            this.blockCooldownMap.put(player.getUniqueId(), System.currentTimeMillis());
        } else {
            player.sendMessage(this.blockPlaceCooldownMessage.replace("<remaining>", format(remaining)));
            event.setCancelled(true);
        }
    }

    //blown up
    @EventHandler(ignoreCancelled = true)
    private void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof EnderCrystal))
            return;

        Player player = ((Player) event.getDamager());

        if (!isInGroup(player))
            return;

        long lastUse = this.crystalCooldownMap.getOrDefault(player.getUniqueId(), 0L);
        long remaining = this.enderCrystalCooldownTime - (System.currentTimeMillis() - lastUse);
        if (remaining <= 0 || player.hasPermission("crystalcooldown.bypass")) {
            this.crystalCooldownMap.put(player.getUniqueId(), System.currentTimeMillis());
        } else {
            player.sendMessage(this.crystalExplodeCooldownMessage.replace("<remaining>", format(remaining)));
            event.setCancelled(true);
        }
    }

    //placing crystal
    @EventHandler(ignoreCancelled = true)
    private void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        if (event.getClickedBlock().getType() != Material.OBSIDIAN && event.getClickedBlock().getType() != Material.BEDROCK)
            return;

        if (event.getItem() == null || event.getItem().getType() != Material.END_CRYSTAL)
            return;

        this.putInGroup(event.getPlayer());
    }

    //damaged by cystal
    @EventHandler
    private void onEndCrystalDamageEvent(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof EnderCrystal))
            return;
        if (!(event.getEntity() instanceof Player))
            return;

        Player player = ((Player) event.getEntity());
        this.putInGroup(player);
    }

    @EventHandler
    private void onPlayerLeave(PlayerQuitEvent event) {
        this.removeFromMaps(event.getPlayer());
    }

    public String format(long value) {
        int seconds = (int) (value / 1000) % 60;
        int minutes = (int) ((value / (1000 * 60)) % 60);
        int hours = (int) ((value / (1000 * 60 * 60)) % 24);
        int days = (int) ((value / (1000 * 60 * 60 * 24)));
        String time = "";

        if (days != 0)
            time = days + " day" + (days == 1 ? "" : "s");
        if (hours != 0)
            time = time + (time.equals("") ? "" : " ") + hours + " hour" + (hours == 1 ? "" : "s");
        if (minutes != 0)
            time = time + (time.equals("") ? "" : " ") + minutes + " minute" + (minutes == 1 ? "" : "s");
        if (seconds != 0)
            time = time + (time.equals("") ? "" : " ") + seconds + " second" + (seconds == 1 ? "" : "s");
        if (time.equals("")) {
            time = value + "ms";
        }
        return time;
    }

    private boolean isInGroup(Player player) {
        long lastUse = this.toCooldown.getOrDefault(player.getUniqueId(), 0L);
        long remaining = this.groupCooldownTime - (System.currentTimeMillis() - lastUse);
        if (remaining > 0)
            return true;
        else {
            this.toCooldown.remove(player.getUniqueId());
            return false;
        }
    }

    private void putInGroup(Player player) {
        if (!isInGroup(player)) {
            this.crystalCooldownMap.put(player.getUniqueId(), System.currentTimeMillis());
            this.blockCooldownMap.put(player.getUniqueId(), System.currentTimeMillis());
        }
        this.toCooldown.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private void removeFromMaps(Player player) {
        this.toCooldown.remove(player.getUniqueId());
        this.crystalCooldownMap.remove(player.getUniqueId());
        this.blockCooldownMap.remove(player.getUniqueId());
    }
}
