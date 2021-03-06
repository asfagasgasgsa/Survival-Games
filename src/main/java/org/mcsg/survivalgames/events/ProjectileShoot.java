package org.mcsg.survivalgames.events;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.mcsg.survivalgames.*;

/**
 * Created for the AddstarMC Project. Created by Narimm on 10/01/2019.
 */
public class ProjectileShoot implements Listener {
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileShoot(final ProjectileHitEvent event) {
        final ProjectileSource source = event.getEntity().getShooter();
        if (source instanceof Player && event.getHitEntity() instanceof Player) {
            final Game g = GameManager.getInstance().getGame(GameManager.getInstance().getPlayerGameId((Player) source));
            if (g == null || g.isPlayerinactive((Player) source)) {
                return;
            }
            if (g.getMode() == Game.GameMode.DISABLED) {
                return;
            }
            if (g.getMode() != Game.GameMode.INGAME) {
                return;
            }
            final Player hit = (Player) event.getHitEntity();
            if (g.isSpectator(hit)) {
                MessageManager.getInstance().broadcastFMessage(MessageManager.PrefixType.INFO, "game.projectilewarning", "player-" + hit.getDisplayName());
                g.removeSpectator(hit);
                hit.teleport(SettingsManager.getInstance().getLobbySpawn());
                MessageManager.getInstance().logMessage(MessageManager.PrefixType.INFO, hit.getDisplayName() + " was teleported for projectile interference");
            }
        }
        
    }
}
