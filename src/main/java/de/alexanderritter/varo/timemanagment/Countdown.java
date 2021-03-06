package de.alexanderritter.varo.timemanagment;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitRunnable;

import de.alexanderritter.varo.events.IngameEvents;
import de.alexanderritter.varo.events.SpectatorListener;
import de.alexanderritter.varo.events.StandStill;
import de.alexanderritter.varo.ingame.PlayerManager;
import de.alexanderritter.varo.ingame.TeamChest;
import de.alexanderritter.varo.ingame.VaroPlayer;
import de.alexanderritter.varo.main.Varo;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.IChatBaseComponent.ChatSerializer;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle.EnumTitleAction;

public class Countdown extends BukkitRunnable {
	
	Varo plugin;
	int time;
	boolean running;
	StandStill standstill;
	
	public Countdown(Varo plugin, int time) {
		this.plugin = plugin;
		this.time = time;
	}

	@Override
	public void run() {
		switch(time) {
		case 60: case 50: case 40: case 30: case 15: case 10: case 5: case 4:
			Bukkit.broadcastMessage(Varo.prefix + ChatColor.DARK_GREEN + "Varo beginnt in " + 
		ChatColor.GOLD + time + ChatColor.DARK_GREEN + " Sekunden.");
			break;
		case 3: case 2: case 1:
			Bukkit.broadcastMessage(Varo.prefix + ChatColor.DARK_GREEN + "Varo beginnt in " + 
					ChatColor.GOLD + time + ChatColor.DARK_GREEN + " Sekunden.");
			for(Player online : Bukkit.getOnlinePlayers()) {
				online.playSound(online.getLocation(), Sound.SUCCESSFUL_HIT, 1, 1);
				IChatBaseComponent chatTitle = ChatSerializer.a("{\"text\": \"" + time + "\",color:" + ChatColor.GREEN.name().toLowerCase() + "}");
				PacketPlayOutTitle title = new PacketPlayOutTitle(EnumTitleAction.TITLE, chatTitle);
				PacketPlayOutTitle length = new PacketPlayOutTitle(3, 20, 3);
				((CraftPlayer) online).getHandle().playerConnection.sendPacket(title);
				((CraftPlayer) online).getHandle().playerConnection.sendPacket(length);
			}
			break;
		case 0:
			Bukkit.broadcastMessage(ChatColor.RED + "Es geht los! Kämpfe um dein Überleben!");
			for(Player online : Bukkit.getOnlinePlayers()) {
				online.playSound(online.getLocation(), Sound.SUCCESSFUL_HIT, 1, 2);
				IChatBaseComponent chatTitle = ChatSerializer.a("{\"text\": \"" + "START" + "\",color:" + ChatColor.DARK_GREEN.name().toLowerCase() + "}");
				PacketPlayOutTitle title = new PacketPlayOutTitle(EnumTitleAction.TITLE, chatTitle);
				PacketPlayOutTitle length = new PacketPlayOutTitle(3, 20, 3);
				((CraftPlayer) online).getHandle().playerConnection.sendPacket(title);
				((CraftPlayer) online).getHandle().playerConnection.sendPacket(length);
			}
			Bukkit.getServer().getScheduler().cancelAllTasks();
			
			HandlerList.unregisterAll(plugin);
			Bukkit.getPluginManager().registerEvents(new IngameEvents(plugin), plugin);
			Bukkit.getPluginManager().registerEvents(new TeamChest(plugin), plugin);
			Bukkit.getPluginManager().registerEvents(new SpectatorListener(plugin), plugin);
			
			stop();
		default:
			break;
		}
		time--;
	}
	
	public boolean isRunning() {
		return running;
	}
	
	public void start(int time) {
		this.running = true;
		this.time = time;
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(plugin.getRegistration().getAllUUIDs().contains(p.getUniqueId())) {
				PlayerManager.addIngamePlayer(p, plugin.getRegistration().loadPlayer(p));
				VaroPlayer ip = PlayerManager.getIngamePlayer(p);
				ip.setupScoreboard();
				String name = p.getName();
				if(name.length() > 16) {
					name = p.getName().substring(0, 16);
				}
				p.setPlayerListName(ip.getColor() + name);
			} else p.kickPlayer("Du bist nicht für Varo registriert. Bye bye");
		}
		for(Player online : Bukkit.getOnlinePlayers()) plugin.preparePlayer(online);
		for(World world : Bukkit.getWorlds()) plugin.prepareWorld(world);
		standstill = new StandStill();
		Bukkit.getPluginManager().registerEvents(standstill, plugin);
		this.runTaskTimer(plugin, 0, 20);
	}
	
	public void stop() {
		this.running = false;
		plugin.initializeGametime();
		standstill.unregister();
		plugin.start();
		this.cancel();
	}
	
	public void pause() {
		this.running = false;
		plugin.getSettings().setRunning(false);
		HandlerList.unregisterAll(plugin);
		for(Player p : Bukkit.getOnlinePlayers()) {
			p.teleport(plugin.getSettings().getLobby());
			p.sendMessage(Varo.prefix + ChatColor.RED + "Der Countdown wurde abgebrochen!");
		}
		this.cancel();
	}

}
