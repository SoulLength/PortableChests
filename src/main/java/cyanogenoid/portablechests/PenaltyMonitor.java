package cyanogenoid.portablechests;

import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.Objects;

import static org.bukkit.Bukkit.getOnlinePlayers;

public class PenaltyMonitor extends BukkitRunnable {
    private final Collection<PotionEffect> penalties;

    PenaltyMonitor(Collection<PotionEffect> penalties) {
        this.penalties = penalties;
    }

    @Override
    public void run() {
        getOnlinePlayers().stream()
                .filter(player -> !player.hasPermission(Permissions.canSkipPenalty))
                .map(HumanEntity::getInventory)
                .filter(PortableChests::containsPenaltyContainer)
                .map(PlayerInventory::getHolder)
                .filter(Objects::nonNull)
                .forEach(this::applyPenalties);
    }

    private void applyPenalties(HumanEntity humanEntity) {
        penalties.forEach(penalty -> addPenalty(humanEntity, penalty));

    }

    private void addPenalty(HumanEntity humanEntity, PotionEffect penalty) {
        humanEntity.addPotionEffect(penalty);
    }
}
