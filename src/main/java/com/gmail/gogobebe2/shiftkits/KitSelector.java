package com.gmail.gogobebe2.shiftkits;

import com.gmail.gogobebe2.shiftkits.kitgroups.KitGroup;
import com.gmail.gogobebe2.shiftkits.kitgroups.KitGroupInstances;
import com.gmail.gogobebe2.shiftkits.requirements.Cost;
import com.gmail.gogobebe2.shiftkits.requirements.Requirement;
import com.gmail.gogobebe2.shiftstats.ShiftStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitScheduler;

import java.sql.SQLException;
import java.util.*;

public class KitSelector {
    private static Map<UUID, KitSelector> kitSelectors = new HashMap<>();

    private static KitSelectorListener kitSelectorListener = new KitSelectorListener();

    private static final ItemStack selector = initSelector();

    private Inventory kitListMenu = Bukkit.createInventory(null,
            roundUpToNearestMultiple(KitGroupInstances.getInstances().size(), 9),
            ChatColor.BOLD + "" + ChatColor.AQUA + "Kit Selection Menu");

    private UUID playerUUID;

    private Map<String, Kit> kitsOwned;

    private KitSelector(UUID playerUUID) throws SQLException, ClassNotFoundException {
        this.playerUUID = playerUUID;
        BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(ShiftKits.instance, new Runnable() {
            @Override
            public void run() {
                // To reduce possible lag, check if inventory is open:
                if (!kitListMenu.getViewers().isEmpty()) {
                    try {
                        updateKitListMenu();
                    } catch (SQLException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 0L, 2L);
    }

    private void updateKitListMenu() throws SQLException, ClassNotFoundException {
        int index = 0;

        Map<String, Kit> kits = new HashMap<>();
        Player player = Bukkit.getPlayer(playerUUID);

        for (KitGroup kitGroup : KitGroupInstances.getInstances()) {
            String kitName = kitGroup.getName();
            int level = 0;
            for (String kitID : ShiftStats.getAPI().getKits(player.getUniqueId())) {
                if (kitID.contains(kitName)) {
                    level = Integer.parseInt(kitID.replace("-" + kitName, ""));
                    break;
                }
            }

            Kit kit;

            if (level == 3) kit = kitGroup.getLevel3();
            else if (level == 2) kit =  kitGroup.getLevel2();
            else kit =  kitGroup.getLevel1();

            ItemStack button = new ItemStack(kit.getIcon(), 1);
            ItemMeta meta = button.getItemMeta();
            String displayName = ChatColor.AQUA + "" + ChatColor.BOLD + kitName + ChatColor.BLUE + " - Level " + level;
            meta.setDisplayName(displayName);
            kits.put(displayName, kit);
            button.setItemMeta(meta);

            kitListMenu.setItem(index, button);
        }

        player.updateInventory();
        this.kitsOwned = kits;
    }

    private boolean canBuy(Kit kit) throws SQLException, ClassNotFoundException {
        return kit.getRequirement().satisfies(Bukkit.getPlayer(playerUUID));
    }

    private void buy(Kit kit) throws SQLException, ClassNotFoundException {
        Requirement requirement = kit.getRequirement();
        String id = kit.getId();
        Player player = Bukkit.getPlayer(playerUUID);

        player.sendMessage(ChatColor.GREEN + "You just unlocked the " + id + " kit with "
                + requirement.getDescription());
        if (requirement instanceof Cost) ((Cost) requirement).takeXP(player);
        ShiftStats.getAPI().addKit(player.getUniqueId(), id);
    }

    private static ItemStack initSelector() {
        ItemStack selector = new ItemStack(Material.EMERALD, 1);
        ItemMeta meta = selector.getItemMeta();
        meta.setDisplayName(ChatColor.BOLD + "" + ChatColor.AQUA + "Kit Selector");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.BLUE + "Choose a kit before you can play.");
        meta.setLore(lore);
        selector.setItemMeta(meta);
        return selector;
    }

    private static int roundUpToNearestMultiple(double number, double factor) {
        return (int) (Math.ceil(number / factor) * factor);
    }

    protected static KitSelectorListener getListener() {
        return kitSelectorListener;
    }

    private static class KitSelectorListener implements Listener {
        @EventHandler
        private static void onPlayerJoin(PlayerJoinEvent event) {
            UUID playerUUID = event.getPlayer().getUniqueId();
            try {
                kitSelectors.put(playerUUID, new KitSelector(playerUUID));
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        @EventHandler
        private static void onInteractEvent(PlayerInteractEvent event) {
            Action action = event.getAction();
            if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR
                    || action == Action.LEFT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR) {
                ItemStack item = event.getItem();
                if (item.getType() == selector.getType() && item.getItemMeta().equals(selector.getItemMeta())) {
                    Player player = event.getPlayer();
                    player.sendMessage(ChatColor.GREEN + "Opening kit selection menu...");
                    player.openInventory(kitSelectors.get(player.getUniqueId()).kitListMenu);
                }
            }
        }

        @EventHandler
        private static void onInventoryClick(InventoryClickEvent event) {
            Player player = (Player) event.getWhoClicked();
            ItemStack button = event.getCurrentItem();
            Inventory inventory = event.getInventory();
            String inventoryName = inventory.getName();

            KitSelector kitSelector = kitSelectors.get(player.getUniqueId());

            String buyOrSellKitMenuNameSuffix = ChatColor.BOLD + "" + ChatColor.AQUA + "Buy or Sell Kit Menu - ";
            if (inventoryName.equals(kitSelector.kitListMenu.getName())) {

                String kitDisplayName = button.getItemMeta().getDisplayName();

                Kit kit = kitSelector.kitsOwned.get(kitDisplayName);
                short level = kit.getLevel();

                Inventory buyOrSellMenu = Bukkit.createInventory(null, 9, buyOrSellKitMenuNameSuffix + kitDisplayName);

                ItemStack buyButton = new ItemStack(Material.GOLD_INGOT, 1);
                ItemMeta buyButtonMeta = buyButton.getItemMeta();

                KitGroup kitGroup = KitGroupInstances.getKitGroupInstance(kit.getId().replace(level + "-", ""));
                assert kitGroup != null;

                Kit nextKit;

                if (level == 0) {
                    // He does not have the kit...
                    buyButtonMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Purchase kit");
                    nextKit = kitGroup.getLevel1();
                }
                else if (level == 1 || level == 2) {
                    // He has the kit...
                    buyButtonMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Upgrade kit");
                    if (level == 1) nextKit = kitGroup.getLevel2();
                    else nextKit = kitGroup.getLevel3();
                }
                else {
                    buyButtonMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Kit is already max level");
                    buyButton.setType(Material.BARRIER);
                    player.sendMessage(ChatColor.RED + "Error! This kit is already it's max level.");
                    return;
                }

                List<String> buyButtonLore = new ArrayList<>();
                buyButtonLore.add(ChatColor.GOLD + "Level " + nextKit.getLevel() + " " + kitGroup.getName());
                buyButtonLore.add(ChatColor.RED + "Requirement:");
                buyButtonLore.add(ChatColor.DARK_RED + "You need to have " + nextKit.getRequirement().getDescription()
                        + " to unlock this kit");

                buyButtonMeta.setLore(buyButtonLore);
                buyButton.setItemMeta(buyButtonMeta);

                buyOrSellMenu.setItem(3, buyButton);
                player.closeInventory();
                player.openInventory(buyOrSellMenu);
            }
            else if (inventoryName.contains(buyOrSellKitMenuNameSuffix)) {
                // TODO:
                // if they clicked the select button, give the kit to the player, send them a message that they got the kit and remove the kit selector item.
                // if they clicked the buy button, unlock the kit, send them a message that they got the kit and give it to them using sql.
                player.closeInventory();
            }
        }
    }
}

