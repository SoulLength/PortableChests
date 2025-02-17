package cyanogenoid.portablechests;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

import static org.bukkit.Bukkit.getOnlinePlayers;

public class PenaltyMonitor extends BukkitRunnable {
    private final Collection<PotionEffect> penalties;

    PenaltyMonitor(Collection<PotionEffect> penalties) {
        this.penalties = penalties;
    }

    @Override
    public void run() {
        for (Player player:getOnlinePlayers()) {
            if (!player.hasPermission(Permissions.canSkipPenalty) && PortableChests.containsPenaltyContainer(player.getInventory())) {
                applyPenalties(player);
                if (player.getVehicle() != null && player.getVehicle() instanceof LivingEntity) {
                    applyPenalties((LivingEntity) player.getVehicle());
                }
            }
        }
    }

    private void applyPenalties(LivingEntity livingEntity) {
        penalties.forEach(penalty -> addPenalty(livingEntity, penalty));

    }

    private void addPenalty(LivingEntity livingEntity, PotionEffect penalty) {
        livingEntity.addPotionEffect(penalty);
    }
}
