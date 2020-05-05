package net.skyprison.Main;

import java.io.*;
import java.util.*;

import com.bergerkiller.bukkit.common.events.EntityRemoveEvent;
import com.google.common.collect.Lists;
import net.skyprison.Main.Commands.*;
import net.skyprison.Main.Commands.Donations.DonorAdd;
import net.skyprison.Main.Commands.Donations.Purchases;
import net.skyprison.Main.Commands.RanksPkg.*;
import net.skyprison.Main.Commands.Opme.*;
import net.skyprison.Main.Commands.RanksPkg.CbHistory;
import net.skyprison.Main.Commands.RanksPkg.Contraband;
import net.skyprison.Main.Commands.RanksPkg.GuardChat;
import net.skyprison.Main.Commands.RanksPkg.GuardDuty;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;


public class SkyPrisonMain extends JavaPlugin implements Listener {
    FileConfiguration config = this.getConfig();

    private static SkyPrisonMain instance;
    public static SkyPrisonMain getInstance() {
        return instance;
    }

    public void onEnable() {
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        config.addDefault("enable-op-command", true);
        config.addDefault("enable-deop-command", true);
        config.addDefault("deop-on-join", false);
        config.addDefault("builder-worlds", Lists.newArrayList("world"));
        config.addDefault("guard-worlds", Lists.newArrayList("prison"));
        config.addDefault("contrabands", Lists.newArrayList("wooden_sword"));
        config.options().copyDefaults(true);
        saveConfig();
        ArrayList files = new ArrayList();
        files.add("bounties.yml");
        files.add("spongeLocations.yml");
        files.add("regionLocations.yml");
        files.add("dropChest.yml");
        files.add("rewardGUI.yml");
        files.add("donations");
        files.add("watchlist.yml");
        for (int i = 0; i < files.size(); i++) {
            File f = new File(Bukkit.getServer().getPluginManager().getPlugin("SkyPrisonCore")
                    .getDataFolder() + "/" + files.get(i));
            if (!f.exists() && !files.get(i).equals("donations")) {
                try {
                    f.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (!f.exists() && files.get(i).equals("donations")) {
                f.mkdir();
            }
        }
        getCommand("g").setExecutor(new GuardChat());
        getCommand("guardduty").setExecutor(new GuardDuty());
        getCommand("b").setExecutor(new BuildChat());
        getCommand("buildmode").setExecutor(new BuildMode());
        getCommand("spongeloc").setExecutor(new SpongeLoc());
        getCommand("dropchest").setExecutor(new DropChest());
        getCommand("regiontp").setExecutor(new RegionTP());
        getCommand("minetp").setExecutor(new MineTP());
        getCommand("opme").setExecutor(new Opme());
        getCommand("deopme").setExecutor(new Deopme());
        getCommand("rewards").setExecutor(new RewardGUI());
        getCommand("contraband").setExecutor(new Contraband());
        getCommand("cbhistory").setExecutor(new CbHistory());
        getCommand("silentjoin").setExecutor(new SilentJoin());
        getCommand("donoradd").setExecutor(new DonorAdd());
        getCommand("purchases").setExecutor(new Purchases());
        getCommand("bounty").setExecutor(new Bounty());
        getCommand("watchlist").setExecutor(new Watchlist());
        getCommand("watchlistadd").setExecutor(new WatchlistAdd());
        getCommand("watchlistdel").setExecutor(new WatchlistDelete());
        getCommand("watchlisttoggle").setExecutor(new WatchlistToggle());
        if (config.getBoolean("enable-op-command")) {
            getCommand("op").setExecutor(new Op());
        } else {
            getCommand("op").setExecutor(new Opdisable());
        }
        if (config.getBoolean("enable-deop-command")) {
            getCommand("deop").setExecutor(new Deop());
        } else {
            getCommand("deop").setExecutor(new Opdisable());
        }
    }

    public void onDisable() {
    }

    //
    // Creates lists of people that have been /cb, and also creates the list containing all of the contraband
    //
    public List<Material> contraband() {
        ArrayList arr = (ArrayList) config.getList("contrabands");
        List<Material> contraband = Lists.newArrayList();
        for(int i = 0; i < arr.size(); i++) {
                contraband.add(Material.getMaterial(arr.get(i).toString().toUpperCase()));
        }
        return contraband;
    }
    public ArrayList<Player> cbed = new ArrayList();
    public HashMap<Player, Player> cbedMap = new HashMap();
    public Map<Player, Map.Entry<Player, Long>> hitcd = new HashMap();
    public Map<String, HashMap<String, Inventory>> cbGuards = new HashMap();
    public boolean isGuardGear(ItemStack i) {
        if (i != null) {
            if (i.getType() == Material.CHAINMAIL_HELMET || i.getType() == Material.CHAINMAIL_CHESTPLATE || i.getType() == Material.CHAINMAIL_LEGGINGS || i.getType() == Material.CHAINMAIL_BOOTS || i.getType() == Material.DIAMOND_SWORD) {
                return true;
            } else if (i.getType() == Material.BOW) {
                if (i.getItemMeta().hasDisplayName() && i.getItemMeta().getDisplayName().contains("Guard Bow")) {
                    return true;
                } else {
                    return false;
                }
            } else if (i.getType() == Material.SHIELD) {
                if (i.getItemMeta().hasDisplayName() && i.getItemMeta().getDisplayName().contains("Guard Shield")) {
                    return true;
                } else {
                    return false;
                }
            } else if (i.getType() == Material.LEAD && i.getItemMeta().hasDisplayName() && i.getItemMeta().getDisplayName().contains("Cuffs")) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private void InvGuardGearDelPlyr(Player player) {
        for (int n = 0; n < player.getInventory().getSize(); n++) {
            ItemStack i = player.getInventory().getItem(n);
            if (i != null &&
                    isGuardGear(i)) {
                i.setAmount(0);
            }
        }
    }

    private void InvGuardGearDelOther(Player player) {
        boolean deletedsomething = false;
        for (int n = 0; n < player.getOpenInventory().getTopInventory().getSize(); n++) {
            ItemStack i = player.getOpenInventory().getTopInventory().getItem(n);
            if (i != null &&
                    isGuardGear(i)) {
                i.setAmount(0);
                deletedsomething = true;
            }
        }

        if (deletedsomething) {
            player.closeInventory();
        }
    }

    private void cbedRemInv(Player target, Player guard) {
        int m = 0;
        target.closeInventory();
        Inventory cbInv = Bukkit.getServer().createInventory(null, 27, ChatColor.DARK_RED + "Contraband! " + ChatColor.RED + target.getName());
        for (int n = 0; n < target.getInventory().getSize(); n++) {
            ItemStack i = target.getInventory().getItem(n);
            if (i != null) {
                for (Material cb : contraband()) {
                    if (i.getType() == cb && m < 27) {
                        ItemStack newcb = new ItemStack(i.getType());
                        ItemMeta newmeta = i.getItemMeta();
                        newcb.setItemMeta(newmeta);
                        cbInv.setItem(m, newcb);
                        m++;
                        i.setAmount(0);
                    }
                }
            }
        }
        if(this.cbGuards.get(guard.getName().toLowerCase()) == null) {
            HashMap<String, Inventory> cbArchive = new HashMap<String, Inventory>();
            cbArchive.put("blank", cbInv);
            this.cbGuards.put(guard.getName().toLowerCase(), cbArchive);
        }
        HashMap<String, Inventory> cbArchive = this.cbGuards.get(guard.getName().toLowerCase());
        cbArchive.put(target.getName().toLowerCase(), cbInv);
        this.cbGuards.put(guard.getName().toLowerCase(), cbArchive);
        guard.openInventory(cbInv);
        guard.sendMessage("[" + ChatColor.BLUE + "Contraband" + ChatColor.WHITE + "]: " + ChatColor.GOLD + target.getName() + ChatColor.YELLOW + " has handed over their contraband!");
    }

    public static void wlistCleanup(File f, YamlConfiguration yamlf) {
        long current = System.currentTimeMillis()/1000L;
        for (String key : yamlf.getConfigurationSection("wlist").getKeys(false)) {
            long expire = yamlf.getLong("wlist."+key);
            if(current>expire) {
                yamlf.set("wlist."+key, null);
            }
        }
    }

    //
    // EventHandlers regarding RanksPkg
    //
    /*@EventHandler
    public void cbinvclose(InventoryCloseEvent event) {
        HumanEntity human = event.getPlayer();
        if(human instanceof Player) {
            Player closer = Bukkit.getPlayer(human.getUniqueId());
            Map.Entry<Player, Long> lasthit = (Map.Entry) this.hitcd.get();
            if(event.getInventory() == this.cbhist.)
        }
    }*/

    @EventHandler
    public void cbedChat(AsyncPlayerChatEvent event) {
        final Player target = event.getPlayer();
        String[] args = event.getMessage().split(" ");
        if (this.cbed.contains(target)) {
            if (args[0].equalsIgnoreCase("yes")) {
                event.setCancelled(true);
                target.sendMessage("[" + ChatColor.BLUE + "Contraband" + ChatColor.WHITE + "]: " + ChatColor.RED + "You have selected to turn over your contraband. All contraband items have been removed from your inventory!");
                getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                    public void run() {
                        SkyPrisonMain.this.cbed.remove(target);
                        SkyPrisonMain.this.cbedRemInv(target, (Player) SkyPrisonMain.this.cbedMap.get(target));
                        SkyPrisonMain.this.cbedMap.remove(target);
                    }
                }, 5L);
            } else if (args[0].equalsIgnoreCase("no")) {
                event.setCancelled(true);
                getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                    public void run() {
                        SkyPrisonMain.this.cbed.remove(target);
                        Bukkit.getServer().dispatchCommand(SkyPrisonMain.this.getServer().getConsoleSender(), "jail " + target.getName());
                        target.sendMessage("[" + ChatColor.BLUE + "Contraband" + ChatColor.WHITE + "]: " + ChatColor.RED + "You have selected to go to jail. All contraband items will remain in your inventory!");
                        ((Player) SkyPrisonMain.this.cbedMap.get(target)).sendMessage("[" + ChatColor.BLUE + "Contraband" + ChatColor.WHITE + "]: " + ChatColor.GOLD + target.getName() + ChatColor.YELLOW + " has gone to jail!");
                        SkyPrisonMain.this.cbedMap.remove(target);
                    }
                }, 5L);
            } else {
                target.sendMessage("[" + ChatColor.BLUE + "Contraband" + ChatColor.WHITE + "]: " + ChatColor.RED + "Please respond Yes or No before you proceed...");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void guardhit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player damager = (Player) event.getDamager();
            Player damagee = (Player) event.getEntity();
            if(damager.hasPermission("skyprisoncore.guard.onduty") && damagee.hasPermission("skyprisoncore.guard.onduty")) {
                event.setCancelled(true);
            } else if (damagee.hasPermission("skyprisoncore.showhit")) {
                Map.Entry<Player, Long> lasthit = (Map.Entry) this.hitcd.get(damager);
                if (this.hitcd.get(damager) == null || (lasthit.getKey() == damagee && System.currentTimeMillis() / 1000L - ((Long) lasthit.getValue()).longValue() > 5L) || lasthit.getKey() !=damagee) {
                    damagee.sendMessage(ChatColor.RED + "You have been hit by " + damager.getName());
                    this.hitcd.put(damager, new AbstractMap.SimpleEntry(damagee, Long.valueOf(System.currentTimeMillis() / 1000L)));
                }
            }
        }
    }

    @EventHandler
    public void moveEvent(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (this.cbed.contains(player)) {
            event.setCancelled(true);
        }
        if (player.hasPermission("skyprisoncore.guard.onduty")) {
            ArrayList arr = (ArrayList) config.getList("guard-worlds");
            Boolean inWorld = false;
            for(int i = 0; i < arr.size(); i++) {
                if(player.getWorld().getName().equalsIgnoreCase((String) arr.get(i))) {
                    inWorld = true;
                    break;
                }
            }
            if(inWorld == false) {
                event.setCancelled(true);
                player.sendMessage("" + ChatColor.RED + "Please go off duty when leaving the prison world!");
            }
        }
        if(player.hasPermission("skyprisoncore.builder.onduty")) {
            ArrayList arr = (ArrayList) config.getList("builder-worlds");
            Boolean inWorld = false;
            for(int i = 0; i < arr.size(); i++) {
                if(player.getWorld().getName().equalsIgnoreCase((String) arr.get(i))) {
                    inWorld = true;
                    break;
                }
            }
            if(inWorld == false) {
                event.setCancelled(true);
                player.sendMessage("" + ChatColor.RED + "Please go off duty when leaving the build worlds!");
            }
        }
    }

    private RewardGUI RewardGUI = new RewardGUI();

    @EventHandler
    public boolean invclick(InventoryClickEvent event) {
        if (ChatColor.stripColor(event.getView().getTitle()).equalsIgnoreCase("prison secrets") || ChatColor.stripColor(event.getView().getTitle()).equalsIgnoreCase("free secrets")) {
            if (event.getCurrentItem() != null) {
                event.setCancelled(true);
                if (event.getCurrentItem().getType() == Material.PAPER) {
                    if (event.getSlot() == 53) {
                        RewardGUI.openGUI((Player) event.getWhoClicked(), 1);
                    } else if (event.getSlot() == 45) {
                        RewardGUI.openGUI((Player) event.getWhoClicked(), 0);
                    }
                }
            }
        }
        if (ChatColor.stripColor(event.getView().getTitle()).equalsIgnoreCase("bounties")) {
            if (event.getCurrentItem() != null) {
                event.setCancelled(true);
            }
        }
        Player player = (Player) event.getWhoClicked();
        if (cbed.contains(player)) {
            event.setCancelled(true);
            if (player.getOpenInventory().getTitle().equalsIgnoreCase(ChatColor.DARK_RED + "You've been caught with contraband!")) {
                if (event.getSlot() == 11) {
                    player.sendMessage("[" + ChatColor.BLUE + "Contraband" + ChatColor.WHITE + "]: " + ChatColor.RED + "You have selected to turn over your contraband. All contraband items have been removed from your inventory!");
                    cbed.remove(player);
                    cbedMap.get(player).sendMessage("[" + ChatColor.BLUE + "Contraband" + ChatColor.WHITE + "]: " + ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " has given up their contraband!");
                    cbedRemInv(player, (Player) this.cbedMap.get(player));
                    cbedMap.remove(player);
                    return true;
                }
                if (event.getSlot() == 15) {
                    this.cbed.remove(player);
                    Bukkit.getServer().dispatchCommand(getServer().getConsoleSender(), "jail " + player.getName());
                    player.sendMessage("[" + ChatColor.BLUE + "Contraband" + ChatColor.WHITE + "]: " + ChatColor.RED + "You have selected to go to jail. All contraband items will remain in your inventory!");
                    ((Player) this.cbedMap.get(player)).sendMessage("[" + ChatColor.BLUE + "Contraband" + ChatColor.WHITE + "]: " + ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " has gone to jail!");
                    this.cbedMap.remove(player);
                    return true;
                }
            } else {
                return false;
            }
        }
        if (!player.hasPermission("skyprisoncore.guard.itembypasss")
                && !player.getLocation().getWorld().getName().equalsIgnoreCase("prison")
                && !player.getLocation().getWorld().getName().equalsIgnoreCase("events")
                && !player.getOpenInventory().getType().equals(InventoryType.CREATIVE)) {
            if (isGuardGear(event.getCurrentItem())) {
                event.setCancelled(true);
            }
            if (player.getOpenInventory().getType() != InventoryType.PLAYER) {
                InvGuardGearDelOther(player);
            }
            InvGuardGearDelPlyr(player);
            return true;
        } else {
            return false;
        }
    }

    @EventHandler
    public void pickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (!player.hasPermission("skyprisoncore.guard.itembypass")
                    && !event.getEntity().getLocation().getWorld().getName().equalsIgnoreCase("events")) {
                if (isGuardGear(event.getItem().getItemStack())) {
                    event.setCancelled(true);
                }
                InvGuardGearDelPlyr(player);
            }
        }
    }

    //
// EventHandlers regarding DropParty Chest
//
    @EventHandler
    public void voidFall(EntityRemoveEvent event) {
        if (event.getEntity().getLocation().getY() < -63) {
            if (event.getEntityType() == EntityType.DROPPED_ITEM) {
                Item item = (Item) event.getEntity();
                ItemStack sItem = item.getItemStack();
                File f = new File("plugins/SkyPrisonCore/dropChest.yml");
                YamlConfiguration yamlf = YamlConfiguration.loadConfiguration(f);
                for (int i = 0; i < 54; i++) {
                    if (!yamlf.contains("items." + i)) {
                        yamlf.set("items." + i + ".item", sItem);
                        try {
                            yamlf.save(f);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            }
        }
    }
    //
    // EventHandlers regarding Sponge Event
    //
    @EventHandler
    public void spongeEvent(BlockDamageEvent event) {
        Block b = event.getBlock();
        Location loc = b.getLocation();
        if (b.getType() == Material.SPONGE) {
            if (loc.getWorld().getName().equalsIgnoreCase("prison") || loc.getWorld().getName().equalsIgnoreCase("event_world")) {
                File f = new File("plugins/SkyPrisonCore/spongeLocations.yml");
                YamlConfiguration yamlf = YamlConfiguration.loadConfiguration(f);
                Set setList = yamlf.getConfigurationSection("locations").getKeys(false);
                for (int i = 0; i < setList.size(); i++) {
                    if (yamlf.contains("locations." + i)) {
                        World w = Bukkit.getServer().getWorld(yamlf.getString("locations." + i + ".world"));
                        Location spongeLoc = new Location(w, yamlf.getDouble("locations." + i + ".x"), yamlf.getDouble("locations." + i + ".y"), yamlf.getDouble("locations." + i + ".z"));
                        spongeLoc = spongeLoc.getBlock().getLocation();
                        if (loc.equals(spongeLoc)) {
                            loc.getBlock().setType(Material.AIR);
                            for (int v = 0; v < setList.size(); v++) {
                                Random random = new Random();
                                int rand = random.nextInt(setList.size());
                                Location placeSponge = new Location(w, yamlf.getDouble("locations." + rand + ".x"), yamlf.getDouble("locations." + rand + ".y"), yamlf.getDouble("locations." + rand + ".z"));
                                placeSponge = placeSponge.getBlock().getLocation();
                                if (!placeSponge.equals(loc)) {
                                    for (Player online : Bukkit.getServer().getOnlinePlayers()) {
                                        online.sendMessage(ChatColor.WHITE + "[" + ChatColor.YELLOW + "Sponge" + ChatColor.WHITE + "] " + ChatColor.GOLD + event.getPlayer().getName() + ChatColor.YELLOW + " has found the sponge! A new one will be hidden somewhere in prison.");
                                    }
                                    ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
                                    String command = "cmi usermeta " + event.getPlayer().getName() + " increment spongefound +1";
                                    Bukkit.dispatchCommand(console, command);
                                    command = "tokensadd " + event.getPlayer().getName() + " 25";
                                    Bukkit.dispatchCommand(console, command);
                                    break;
                                }
                            }
                            break;
                        }
                    } else {
                        continue;
                    }
                }
            }
        }
    }

    //
// EventHandlers regarding Villager Trading
//
    @EventHandler
    public void villagerTrade(InventoryOpenEvent event) {
        if (event.getInventory().getType() != InventoryType.MERCHANT) {
        } else if (event.getInventory().getType().equals(InventoryType.MERCHANT)) {
            Player player = (Player) event.getPlayer();
            player.sendMessage(ChatColor.RED + "Villager trading has been disabled");
            event.setCancelled(true);
        }
    }

    //
// EventHandlers regarding Farming & Mining
//
    @EventHandler
    public void cactusGrow(ItemSpawnEvent event) {
        ItemStack b = event.getEntity().getItemStack();
        Location loc = event.getLocation();
        if (b.getType() == Material.CACTUS && loc.getWorld().getName().equalsIgnoreCase("world")) {
            int random = (int) (Math.random() * 10 + 1);
            if (random == 10) {
            }
        }
    }

    @EventHandler
    public void blockbreak(BlockBreakEvent event) {
        Block b = event.getBlock();
        Location loc = b.getLocation();
        if(!event.isCancelled()) {
            if (b.getType() == Material.SNOW_BLOCK && loc.getWorld().getName().equalsIgnoreCase("prison")) {
                event.setDropItems(false);
                Location cob = loc.add(0.5D, 0.0D, 0.5D);
                ItemStack snowblock = new ItemStack(Material.SNOW_BLOCK, 1);
                loc.getWorld().dropItem(cob, snowblock);
            } else if (b.getType() == Material.SNOW_BLOCK && loc.getWorld().getName().equalsIgnoreCase("events")) {
                event.setDropItems(false);
            } else if (b.getType() == Material.BIRCH_LOG && loc.getWorld().getName().equalsIgnoreCase("prison")) {
                Location newLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ());
                Block newB = newLoc.getBlock();
                if (newB.getType() == Material.GRASS_BLOCK || newB.getType() == Material.DIRT) {
                    getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                        public void run() {
                            loc.getBlock().setType(Material.BIRCH_SAPLING);
                        }
                    }, 2L);
                }
            } else if (b.getType() == Material.WHEAT && loc.getWorld().getName().equalsIgnoreCase("prison")) {
                if (!event.getPlayer().isOp()) {
                    BlockData bdata = b.getBlockData();
                    if (bdata instanceof Ageable) {
                        Ageable age = (Ageable) bdata;
                        if (age.getAge() != age.getMaximumAge()) {
                            event.setCancelled(true);
                            event.getPlayer().sendMessage(ChatColor.RED + "" + ChatColor.ITALIC + "This wheat isn't ready for harvest..");
                        } else {
                            getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                                public void run() {
                                    loc.getBlock().setType(Material.WHEAT);
                                }
                            }, 2L);
                        }
                    }
                }
            }
        }
    }

    //
// EventHandlers regarding Opme commands
//
    @EventHandler
    public void deopOnJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        if (config.getBoolean("deop-on-join")) {
            if (!p.hasPermission("skyprisoncore.deop.joinbypass")) {
                if (p.isOp()) {
                    p.setOp(false);
                }
            }
        }
    }

    @EventHandler
    public void disableCommands(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String[] args = event.getMessage().split(" ");
        if (player.equals(Bukkit.getPlayer("blueberry09"))) {
            if (args[0].equalsIgnoreCase("/cmi") && args[1].equalsIgnoreCase("cuff") && (args[2].equalsIgnoreCase("false") || args[3].equalsIgnoreCase("false"))) {
                event.setCancelled(true);
            }
        }
        if (event.getMessage().startsWith("/") && event.getMessage().contains(":op")
                | event.getMessage().contains(":OP") | event.getMessage().contains(":Op")
                | event.getMessage().contains(":oP") | event.getMessage().contains(":deop")
                | event.getMessage().contains(":DEOP") | event.getMessage().contains(":Deop")
                | event.getMessage().contains(":dEop") | event.getMessage().contains(":deOp")
                | event.getMessage().contains(":deoP") | event.getMessage().contains(":DEop")
                | event.getMessage().contains(":dEOp") | event.getMessage().contains(":deOP")
                | event.getMessage().contains(":DeoP") | event.getMessage().contains(":DEOp")
                | event.getMessage().contains(":dEOP") | event.getMessage().contains(":DeOP")
                | event.getMessage().contains(":DEoP")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.WHITE + "Unknown command. Type " + '"' + "/help" + '"' + " for help.");
        }
    }

    //
    //Event Handlers regarding silent join/leave
    //

    @EventHandler
    public void silentlogoff(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if(!player.getPlayer().equals(Bukkit.getPlayer("DrakePork"))) {
            if (player.hasPermission("cmi.messages.disablequit")) {
                Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "s " + player.getName() + " has left silently...");
            }
        }
    }

    @EventHandler
    public void silentjoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if(!player.getPlayer().equals(Bukkit.getPlayer("DrakePork"))) {
            if (player.hasPermission("cmi.messages.disablelogin")) {
                Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "s " + player.getName() + " has joined silently...");
            }
        }
    }

    //
    // Event Handlers regarding watchlist
    //
    @EventHandler
    public void watchlistjoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        File f = new File("plugins/SkyPrisonCore/watchlist.yml");
        YamlConfiguration yamlf = YamlConfiguration.loadConfiguration(f);
        for (String key : yamlf.getConfigurationSection("wlist").getKeys(false)) {
            if (key.equalsIgnoreCase(player.getName())) {
                if((System.currentTimeMillis()/1000L)<yamlf.getLong("wlist."+player.getName()+".expires")) {
                    for (Player online : Bukkit.getServer().getOnlinePlayers()) {
                        if (online.hasPermission("skyprisoncore.watchlist.basic") && !player.hasPermission("skyprisoncore.watchlist.silent")) {
                            online.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.DARK_RED + "WATCHLIST" + ChatColor.DARK_GRAY + "]" + ChatColor.WHITE + ": " + ChatColor.RED + player.getName() + ChatColor.YELLOW + " has just logged on and is on the watchlist. Please use /watchlist <player> to see why...");
                        }
                    }
                } else {//players watchlist time has expired and will be removed
                    yamlf.getConfigurationSection(key).set("wlist." + player.getName(), null);
                    try {
                        yamlf.save(f);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    //
    // Event Handlers regarding bounties
    //

    @EventHandler
    public void bountyDeath(EntityDeathEvent event) {
        if(event.getEntity() instanceof Player) {
            Player killed = (Player) event.getEntity();
            if(killed.getKiller() instanceof Player && !killed.equals(killed.getKiller())) {
                final File f = new File(Bukkit.getServer().getPluginManager().getPlugin("SkyPrisonDonations")
                        .getDataFolder() + "/bounties.yml");
                if (!f.exists()) {
                    try {
                        f.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Player killer = killed.getKiller();
                FileConfiguration bounty = YamlConfiguration.loadConfiguration(f);
                Set<String> bountyList = bounty.getKeys(false);
                for (String bountyPlayer : bountyList) {
                    if(killed.getUniqueId().equals(UUID.fromString(bountyPlayer))) {
                        bounty.set(bountyPlayer, null);
                        try {
                            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "money give " + killer.getName() + " " + bounty.getInt(bountyPlayer + ".bounty-prize"));
                            bounty.save(f);
                            for (Player online : Bukkit.getServer().getOnlinePlayers()) {
                                online.sendMessage(ChatColor.WHITE + "[" + ChatColor.RED + "Bounties" + ChatColor.WHITE + "]" + ChatColor.YELLOW + " " + killer.getName() + " has claimed the bounty on " + killed.getName() + "!");
                            }
                            killer.sendMessage(ChatColor.WHITE + "[" + ChatColor.RED + "Bounties" + ChatColor.WHITE + "] " + ChatColor.YELLOW + "You have claimed the bounty on " + killed.getName());
                        } catch (final IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }

            }
        }
    }


}


