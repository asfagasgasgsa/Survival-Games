package org.mcsg.survivalgames;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcsg.survivalgames.events.*;
import org.mcsg.survivalgames.hooks.HookManager;
import org.mcsg.survivalgames.lobbysigns.LobbySignManager;
import org.mcsg.survivalgames.logging.LoggingManager;
import org.mcsg.survivalgames.logging.QueueManager;
import org.mcsg.survivalgames.stats.StatsManager;
import org.mcsg.survivalgames.util.ChestRatioStorage;
import org.mcsg.survivalgames.util.DatabaseManager;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;

public class SurvivalGames extends JavaPlugin {
    public static Logger logger;
    public static boolean dbcon;
    public static boolean config_todate;
    public static int config_version = 3;
    public static SurvivalGames plugin;
    private static File datafolder;
    private static boolean disabling;
    SurvivalGames p = this;
    private Metrics metrics;
    private LobbySignManager lobbySignManager;
    
    public static File getPluginDataFolder() {
        return datafolder;
    }
    
    public static boolean isDisabling() {
        return disabling;
    }
    
    public static void debug(final int gameid, final String msg) {
        if (SettingsManager.getInstance().getConfig().getBoolean("debug", false))
            $(gameid, "[Debug] " + msg);
    }
    
    public static void $(final int gameid, final String msg) {
        $(gameid, Level.INFO, msg);
    }
    
    public static void $(final int gameid, final Level l, final String msg) {
        if (gameid == 0) {
            logger.log(l, msg);
        } else {
            logger.log(l, "#" + gameid + ": " + msg);
        }
    }
    
    public void onDisable() {
        disabling = false;
        final PluginDescriptionFile pdfFile = this.p.getDescription();
        SettingsManager.getInstance().saveSpawns();
        SettingsManager.getInstance().saveSystemConfig();
        for (final Game g : GameManager.getInstance().getGames()) {
            try {
                g.disable();
            } catch (final Exception e) {
                //will throw useless "tried to register task blah blah error." Use the method below to reset the arena without a task.
            }
            QueueManager.getInstance().rollback(g.getID(), true);
        }
        
        logger.info(pdfFile.getName() + " version " + pdfFile.getVersion() + " has now been disabled and reset");
        plugin = null;
    }
    
    public void onEnable() {
        plugin = this;
        logger = this.p.getLogger();
        datafolder = this.p.getDataFolder();
        this.metrics = new Metrics(this);
        //ensure that all worlds are loaded. Fixes some issues with Multiverse loading after this plugin had started
        this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Startup(), 10);
    }
    
    public void setCommands() {
        this.getCommand("survivalgames").setExecutor(new CommandHandler(this.p));
    }
    
    public WorldEditPlugin getWorldEdit() {
        final Plugin worldEdit = this.getServer().getPluginManager().getPlugin("WorldEdit");
        if (worldEdit instanceof WorldEditPlugin) {
            return (WorldEditPlugin) worldEdit;
        } else {
            return null;
        }
    }
    
    public LobbySignManager getLobbySignManager() {
        return this.lobbySignManager;
    }
    
    //public static void debug(int a) {
    //	if(SettingsManager.getInstance().getConfig().getBoolean("debug", false))
    //		debug(gameid, String.valueOf(a));
    //}
    
    class Startup implements Runnable {
        public void run() {
            final PluginManager pm = SurvivalGames.this.getServer().getPluginManager();
            SurvivalGames.this.setCommands();
            
            SettingsManager.getInstance().setup(SurvivalGames.this.p);
            MessageManager.getInstance().setup();
            GameManager.getInstance().setup(SurvivalGames.this.p);
            
            SurvivalGames.this.lobbySignManager = new LobbySignManager();
            SurvivalGames.this.lobbySignManager.loadSigns();
            
            try { // try loading everything that uses SQL.
                final FileConfiguration c = SettingsManager.getInstance().getConfig();
                if (c.getBoolean("stats.enabled")) DatabaseManager.getInstance().setup(SurvivalGames.this.p);
                QueueManager.getInstance().setup();
                StatsManager.getInstance().setup(SurvivalGames.this.p, c.getBoolean("stats.enabled"));
                dbcon = true;
            } catch (final Exception e) {
                dbcon = false;
                e.printStackTrace();
                logger.severe("!!!Failed to connect to the database. Please check the settings and try again!!!");
                return;
            } finally {
                LobbyManager.createInstance(SurvivalGames.this.lobbySignManager);
            }
            
            ChestRatioStorage.getInstance().setup();
            HookManager.getInstance().setup();
            pm.registerEvents(new PlaceEvent(), SurvivalGames.this.p);
            pm.registerEvents(new BreakEvent(), SurvivalGames.this.p);
            pm.registerEvents(new DeathEvent(), SurvivalGames.this.p);
            pm.registerEvents(new MoveEvent(), SurvivalGames.this.p);
            pm.registerEvents(new CommandCatch(), SurvivalGames.this.p);
            pm.registerEvents(new ChestReplaceEvent(), SurvivalGames.this.p);
            pm.registerEvents(new LogoutEvent(), SurvivalGames.this.p);
            pm.registerEvents(new JoinEvent(SurvivalGames.this.p), SurvivalGames.this.p);
            pm.registerEvents(new TeleportEvent(), SurvivalGames.this.p);
            pm.registerEvents(LoggingManager.getInstance(), SurvivalGames.this.p);
            pm.registerEvents(new SpectatorEvents(), SurvivalGames.this.p);
            pm.registerEvents(new KitEvents(), SurvivalGames.this.p);
            pm.registerEvents(new KeepLobbyLoadedEvent(), SurvivalGames.this.p);
            pm.registerEvents(new LobbyBoardEvents(), SurvivalGames.this.p);
            pm.registerEvents(new BandageUse(), SurvivalGames.this.p);
            pm.registerEvents(new RespawnEvent(), SurvivalGames.this.p);
            pm.registerEvents(new DropItemEvent(), SurvivalGames.this.p);
            pm.registerEvents(new ProjectileShoot(), SurvivalGames.this.p);
            
            for (final Player p : Bukkit.getOnlinePlayers()) {
                if (GameManager.getInstance().getBlockGameId(p.getLocation()) != -1) {
                    p.teleport(SettingsManager.getInstance().getLobbySpawn());
                }
            }
        }
    }
}