package cyanogenoid.portablechests;

import cyanogenoid.portablechests.utils.Permissions;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.bukkit.Bukkit.getOnlinePlayers;

public class PenaltyMonitor extends BukkitRunnable {
    private final Collection<PotionEffect> penalties;

    public PenaltyMonitor(Collection<PotionEffect> penalties) {
        this.penalties = penalties;
    }

    @Override
    public void run() {
        for (Player player : getOnlinePlayers()) {
            if (player.hasPermission(Permissions.canSkipPenalty)) {
                continue;
            }
            Entity vehicle = player.getVehicle();
            if (vehicle instanceof ChestedHorse) {
                ChestedHorse chestedHorse = (ChestedHorse) vehicle;
                List<ItemStack> horseItems = Arrays.asList(chestedHorse.getInventory().getContents());
                if (PortableChests.containsPortableContainers(horseItems)) {
                    applyPenalties(player);
                    applyPenalties(chestedHorse);
                    continue;
                }
            }
            List<ItemStack> playerItems = new ArrayList<>(Arrays.asList(player.getInventory().getContents()));
            playerItems.add(player.getItemOnCursor());
            if (PortableChests.containsPortableContainers(playerItems)) {
                applyPenalties(player);
                if (vehicle instanceof LivingEntity) {
                    applyPenalties((LivingEntity) vehicle);
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
