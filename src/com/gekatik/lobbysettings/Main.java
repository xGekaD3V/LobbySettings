package com.gekatik.lobbysettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.FileConfigurationOptions;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin
  implements Listener
{
  private ItemStack matE;
  private ItemStack matD;
  private boolean give;
  private final List<Player> players = new ArrayList();
  private final LinkedList<World> alwaysvisible = new LinkedList();
  int hotbarSlot;
  int expLevel;
  int expBar;
  int food;
  String joinNormal;
  String joinVip;
  String joinStaff;
  String LeaveMessage;

  public void onEnable()
  {
    long start = System.currentTimeMillis();
    FileConfiguration config = getConfig();
    config.addDefault("config.toggleMaterialEnabled", "ink_sack;10;&5&lPlayer Visibility&5 -&a ON");
    config.addDefault("config.toggleMaterialDisabled", "ink_sack;8;&5&lPlayer Visibility&5 -&c OFF");
    config.addDefault("config.giveOnJoin", Boolean.valueOf(true));
    String[] worlds = { "world_nether" };
    config.addDefault("config.alwaysVisibleWorld", Arrays.asList(worlds));
    config.options().copyDefaults(true);
    saveConfig();
    reloadConfig();
    this.matE = getItemStack(config.getString("config.toggleMaterialEnabled"));
    this.matD = getItemStack(config.getString("config.toggleMaterialDisabled"));
    this.give = config.getBoolean("config.giveOnJoin");
    for (String s : config.getStringList("config.alwaysVisibleWorld")) {
      this.alwaysvisible.add(getServer().getWorld(s));
    }
    getServer().getPluginManager().registerEvents(this, this);
    getLogger().log(Level.INFO, "Enabled in {3000}ms", Long.valueOf(System.currentTimeMillis() - start));
    this.hotbarSlot = getConfig().getInt("HotBar");
    this.expLevel = getConfig().getInt("expLevel");
    this.expBar = getConfig().getInt("expBar");
    this.food = getConfig().getInt("Food");
    this.joinNormal = getConfig().getString("JoinMessage.normal");
    this.LeaveMessage = getConfig().getString("LeaveMessage.normal");
    this.joinVip = getConfig().getString("JoinMessage.vip");
    this.joinStaff = getConfig().getString("JoinMessage.staff");
  }

  public void onDisable()
  {
    getLogger().log(Level.INFO, "Disabled");
  }

  @EventHandler
  public void setHotbar(PlayerJoinEvent e) {
    Player p = e.getPlayer();
    p.getInventory().setHeldItemSlot(this.hotbarSlot);
  }

  @EventHandler
  public void setFood(PlayerJoinEvent e)
  {
    Player p = e.getPlayer();
    p.setFoodLevel(this.food);
  }

  @EventHandler
  public void setExp(PlayerJoinEvent e)
  {
    Player p = e.getPlayer();
    p.setExp(this.expBar);
    p.setLevel(this.expLevel);
  }

  @EventHandler
  public void JoinMessage(PlayerJoinEvent e)
  {
    Player p = e.getPlayer();
    if ((!p.hasPermission("ls.join.vip")) && (!p.hasPermission("ls.join.staff")) && (!p.isOp()))
      e.setJoinMessage(this.joinNormal.replaceAll("&", "§").replaceAll("<player>", p.getName()));
    else if (p.hasPermission("ls.join.vip"))
      e.setJoinMessage(this.joinVip.replaceAll("&", "§").replaceAll("<player>", p.getName()));
    else if ((p.isOp()) || (p.hasPermission("ls.join.staff")))
      e.setJoinMessage(this.joinStaff.replaceAll("&", "§").replaceAll("<player>", p.getName()));
  }

  @EventHandler
  public void LeaveMessage(PlayerQuitEvent e)
  {
    Player p = e.getPlayer();
    e.setQuitMessage(this.LeaveMessage.replaceAll("&", "§").replaceAll("<player>", p.getName()));
  }

  @EventHandler
  public void onInteract(PlayerInteractEvent e) {
    Player p = e.getPlayer();
    if (p.hasPermission("PlayerTogglePrivate.toggle"))
    {
      if ((e
        .getItem() != null) && (
        (e
        .getItem().getType().equals(this.matE.getType())) || (e.getItem().getType().equals(this.matD.getType()))))
        togglePlayerView(p, e.getItem());
    }
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    Player online = e.getPlayer();
    for (Player p : this.players) {
      if ((p.canSee(online)) && (!online.hasPermission("PlayerTogglePrivate.op"))) {
        p.hidePlayer(online);
      }
    }
    if ((this.give) && (online.hasPermission("PlayerTogglePrivate.toggle")) && ((!online.getInventory().contains(this.matE.getType())) || (!online.getInventory().contains(this.matD.getType()))))
      online.getInventory().addItem(new ItemStack[] { this.matD });
  }

  @EventHandler
  public void onChangeWorld(PlayerChangedWorldEvent e)
  {
    Player p = e.getPlayer();
    if ((this.alwaysvisible.contains(p.getWorld())) && (this.players.contains(p)))
    {
      for (Player online : getServer().getOnlinePlayers()) {
        if (!p.canSee(online)) {
          p.showPlayer(online);
        }
      }
      this.players.remove(p);
    }
  }

  private void togglePlayerView(Player p, ItemStack is)
  {
    if (!this.alwaysvisible.contains(p.getWorld()))
    {
      if (!this.players.contains(p))
      {
        for (Player online : getServer().getOnlinePlayers()) {
          if ((p.canSee(online)) && (!online.hasPermission("PlayerTogglePrivate.op"))) {
            p.hidePlayer(online);
          }
        }
        this.players.add(p);
        is.setType(this.matE.getType());
        is.setItemMeta(this.matE.getItemMeta());
        is.setDurability(this.matE.getDurability());
      }
      else
      {
        for (Player online : getServer().getOnlinePlayers()) {
          if (!p.canSee(online)) {
            p.showPlayer(online);
          }
        }
        this.players.remove(p);
        is.setType(this.matD.getType());
        is.setItemMeta(this.matD.getItemMeta());
        is.setDurability(this.matD.getDurability());
      }
    }
    else
    {
      is.setType(this.matD.getType());
      is.setItemMeta(this.matD.getItemMeta());
      is.setDurability(this.matD.getDurability());
    }
  }

  private ItemStack getItemStack(String string)
  {
    String[] s = string.split(";");
    ItemStack is = new ItemStack(Material.valueOf(s[0].toUpperCase()), 1, Short.valueOf(s[1]).shortValue());
    ItemMeta im = is.getItemMeta();
    im.setDisplayName(s[2].replaceAll("&", "§"));
    is.setItemMeta(im);
    return is;
  }
}