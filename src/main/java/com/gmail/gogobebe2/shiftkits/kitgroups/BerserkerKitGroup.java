package com.gmail.gogobebe2.shiftkits.kitgroups;

import com.gmail.gogobebe2.shiftkits.Kit;
import com.gmail.gogobebe2.shiftkits.MagicKit;
import com.gmail.gogobebe2.shiftkits.requirements.Cost;
import org.bukkit.ChatColor;
import org.bukkit.EntityEffect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BerserkerKitGroup implements KitGroup {
    @Override
    public Kit getLevel1() {
        return getLevel(2, 10, 15000, 1);
    }

    @Override
    public Kit getLevel2() {
        return getLevel(3, 20, 40000, 2);
    }

    @Override
    public Kit getLevel3() {
        return getLevel(4, 30, 100000, 3);
    }

    @Override
    public String getName() {
        return "Berserker";
    }

    private Kit getLevel(int roseBudAmount, final int strengthDuration, int cost, int level) {
        Map<Integer, ItemStack> items = new HashMap<>();

        items.put(0, new ItemStack(Material.STONE_PICKAXE, 1));
        items.put(1, new ItemStack(Material.WOOD_SWORD));

        ItemStack roseBud = new ItemStack(Material.RED_ROSE, roseBudAmount);
        ItemMeta meta = roseBud.getItemMeta();
        final String BLOODLUST_DISPLAYNAME = ChatColor.DARK_RED + "" + ChatColor.BOLD + "Activate Bloodlust";
        meta.setDisplayName(BLOODLUST_DISPLAYNAME);
        final List<String> lore = new ArrayList<>();
        lore.add(ChatColor.RED + "Right click deals 3 hearts damage to you;");
        lore.add(ChatColor.RED + "Gives strength II for " + strengthDuration + " seconds");
        meta.setLore(lore);
        roseBud.setItemMeta(meta);

        items.put(2, roseBud);

        return new MagicKit(getName(), level, new Cost(cost), items, Material.RED_ROSE, new Listener() {
            @EventHandler
            private void onPlayerInteract(PlayerInteractEvent event) {
                ItemStack item = event.getItem();
                ItemMeta meta = item.getItemMeta();
                if ((event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR)
                        && item.getType() == Material.RED_ROSE
                        && meta.getDisplayName().equals(BLOODLUST_DISPLAYNAME)
                        && meta.getLore().equals(lore)) {
                    Player player = event.getPlayer();
                    player.setHealth(player.getHealth() - 6);
                    player.playEffect(EntityEffect.HURT);
                    player.playSound(player.getLocation(), Sound.HURT_FLESH, 1, 1);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, strengthDuration * 20, 1));
                    player.sendMessage(ChatColor.RED + "Your deadly inhumane lust takes over!");
                    item.setAmount(item.getAmount() - 1);
                    event.setCancelled(true);
                }
            }
        });
    }
}