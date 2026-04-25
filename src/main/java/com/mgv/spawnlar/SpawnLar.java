package com.mgv.spawnlar;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class SpawnLar extends JavaPlugin implements Listener {

    private File locFile;
    private YamlConfiguration locConfig;
    private final Map<UUID, TpData> pending = new HashMap<>();

    private static final int DELAY = 5;
    private static final long GRACE = 100L;

    @Override
    public void onEnable() {
        initLocFile();
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("SpawnLar enabled");
    }

    @Override
    public void onDisable() {
        pending.values().forEach(this::stopTasks);
        pending.clear();
    }

    private void initLocFile() {
        locFile = new File(getDataFolder(), "locations.yml");
        if (!locFile.exists()) {
            getDataFolder().mkdirs();
            String def = "Spawn:\n  world: world\n  x: 0\n  y: 64\n  z: 0\n  yaw: 0\n  pitch: 0\n";
            try {
                Files.write(locFile.toPath(), def.getBytes());
            } catch (Exception ex) {
                getLogger().severe("Could not create locations.yml");
            }
        }
        locConfig = YamlConfiguration.loadConfiguration(locFile);
    }

    public void reloadConfigs() {
        locConfig = YamlConfiguration.loadConfiguration(locFile);
        reloadConfig();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        String name = p.getName();
        FileConfiguration cfg = getConfig();

        if (cfg.getBoolean("Motd-Enabled", true)) {
            for (String line : msgCfg().getStringList("Messages.Motd")) {
                p.sendMessage(col(line.replace("{player}", name)));
            }
        }

        String msg = msgCfg().getString("Messages.Player-join", "{player} joined the server!")
                .replace("{player}", name);
        e.setJoinMessage(col(msg));

        if (cfg.getBoolean("Teleport-On-Join", true) && hasSpawn()) {
            tpToSpawn(p);
        }
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("hub")) {
            return handleHub(s);
        }
        if (cmd.getName().equalsIgnoreCase("spawnlar")) {
            return handleAdmin(s, args);
        }
        return false;
    }

    private boolean handleHub(CommandSender s) {
        if (!(s instanceof Player)) {
            s.sendMessage(col("&cIn-game only"));
            return true;
        }

        Player p = (Player) s;

        if (!hasSpawn()) {
            p.sendMessage(col("&cSpawn not configured"));
            return true;
        }

        if (pending.containsKey(p.getUniqueId())) {
            p.sendMessage(col("&cAlready teleporting"));
            return true;
        }

        beginTp(p);
        return true;
    }

    private boolean handleAdmin(CommandSender s, String[] args) {
        if (!s.hasPermission("spawnlar.admin")) {
            s.sendMessage(col("&cNo permission"));
            return true;
        }

        if (args.length == 0) {
            s.sendMessage(col("&7SpawnLar v" + getDescription().getVersion()));
            s.sendMessage(col("&e/spawnlar reload &7- Reload configurations"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            reloadConfigs();
            s.sendMessage(col("&aConfigurations reloaded"));
            getLogger().info("Configurations reloaded by " + s.getName());
            return true;
        }

        s.sendMessage(col("&cUnknown subcommand"));
        return true;
    }

    private void beginTp(Player p) {
        UUID id = p.getUniqueId();
        TpData data = new TpData();
        data.graceUntil = System.currentTimeMillis() + GRACE;
        pending.put(id, data);

        data.title = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (pending.containsKey(id)) {
                p.sendTitle("", col("&aTeleporting in &e" + data.sec + "&a seconds..."), 10, 70, 20);
            }
        }, 0L, 20L);

        data.counter = Bukkit.getScheduler().runTaskTimer(this, () -> {
            TpData cur = pending.get(id);
            if (cur == null) return;

            if (cur.sec > 0) {
                cur.sec--;
            } else {
                tpToSpawn(p);
                endTp(id, cur);
            }
        }, 0L, 20L);
    }

    private void endTp(UUID id, TpData data) {
        pending.remove(id);
        stopTasks(data);
    }

    private void stopTasks(TpData d) {
        if (d.title != null) d.title.cancel();
        if (d.counter != null) d.counter.cancel();
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Location from = e.getFrom(), to = e.getTo();
        if (to == null) return;

        double eps = 1e-4;
        if (Math.abs(from.getX() - to.getX()) > eps ||
            Math.abs(from.getY() - to.getY()) > eps ||
            Math.abs(from.getZ() - to.getZ()) > eps) {
            cancelIfPending(e.getPlayer());
        }
    }

    @EventHandler
    public void onCmd(PlayerCommandPreprocessEvent e) {
        cancelIfPending(e.getPlayer());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            cancelIfPending((Player) e.getEntity());
        }
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player) return;
        if (e.getEntity() instanceof Player) {
            cancelIfPending((Player) e.getEntity());
        }
    }

    @EventHandler public void onBreak(BlockBreakEvent e) {}
    @EventHandler public void onPlace(BlockPlaceEvent e) {}

    private void cancelIfPending(Player p) {
        TpData data = pending.get(p.getUniqueId());
        if (data == null) return;
        if (System.currentTimeMillis() < data.graceUntil) return;

        endTp(p.getUniqueId(), data);
        p.sendTitle("", col("&cTeleport cancelled"), 10, 40, 20);
    }

    private boolean hasSpawn() {
        return locConfig.isSet("Spawn.world") && locConfig.isSet("Spawn.x") &&
               locConfig.isSet("Spawn.y") && locConfig.isSet("Spawn.z");
    }

    private void tpToSpawn(Player p) {
        String worldName = locConfig.getString("Spawn.world", "world");
        double x = locConfig.getDouble("Spawn.x");
        double y = locConfig.getDouble("Spawn.y", 64);
        double z = locConfig.getDouble("Spawn.z");
        float yaw = (float) locConfig.getDouble("Spawn.yaw");
        float pitch = (float) locConfig.getDouble("Spawn.pitch");

        World w = Bukkit.getWorld(worldName);
        if (w != null) {
            p.teleport(new Location(w, x, y, z, yaw, pitch));
        } else {
            getLogger().warning("World '" + worldName + "' not found");
        }
    }

    private YamlConfiguration msgCfg() {
        File f = new File(getDataFolder(), "messages.yml");
        if (!f.exists()) saveResource("messages.yml", false);
        return YamlConfiguration.loadConfiguration(f);
    }

    private String col(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private static class TpData {
        int sec = DELAY;
        long graceUntil;
        BukkitTask title;
        BukkitTask counter;
    }
}