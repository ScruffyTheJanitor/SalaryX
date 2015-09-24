// i physically cringe...
// wrote this when i was 12

package net.scruffix.scruffy;

// Imports
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class SalaryX extends JavaPlugin {

	// Chat colors
	ChatColor green = ChatColor.GREEN;
	ChatColor red = ChatColor.RED;
	ChatColor blue = ChatColor.BLUE;
	ChatColor aqua = ChatColor.AQUA;
	ChatColor white = ChatColor.WHITE;
	int totalTime = 0;
	double totalCash = 0;
	Player x;
	
	private static final Logger log = Logger.getLogger("Minecraft");
	public static Economy econ = null;
	public static Permission perms = null;

	PluginDescriptionFile pdf = this.getDescription();

	@Override
	public void onEnable() {
		loadConfig();
		setupEconomy(); // Set up the Vault economy
		if (!setupEconomy()) {
			log.severe(String.format(
					"[%s] - Disabled due to no Vault dependency found!",
					getDescription().getName()));
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		setupPermissions();
		getLogger().info("SalaryX v" + pdf.getVersion() + " Enabled!");

	}

	public void onDisable() {
		log.info(String.format("[%s] Disabled Version %s", getDescription()
				.getName(), getDescription().getVersion()));
	}

	public void loadConfig() {
		// getConfig().addDefault("Do not touch", " this file."); // server or
		// player
		// getConfig().options().copyDefaults(true);
		// saveConfig();
	}

	public boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer()
				.getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}

	public boolean setupPermissions() {
		RegisteredServiceProvider<Permission> rsp = getServer()
				.getServicesManager().getRegistration(Permission.class);
		perms = rsp.getProvider();
		return perms != null;
	}

	public double getBalance(Player s) {
		return econ.getBalance(s); // Return the balance of the player 's'
	}

	@SuppressWarnings("deprecation")
	// Check if the player is online
	private boolean isOnline(String x) { 
		if (Bukkit.getServer().getPlayerExact(x) != null)
			return true;
		return false;
	}
	
	// Calculate the total time that the payment was running
	public void addTotalTime(double time) {
		totalTime+=time;
	}
	// Return the totalTime variable
	public int getTotalTime() {
		return totalTime;
	}
	// Calculate the total cash that was exchanged
	public void addTotalCash(double cash) {
		totalCash+=cash;
	}
	// return the totalCash variable
	public double getTotalCash() {
		return totalCash;
	}
	public void setPlayer(String p) {
		// Set x to p's username
		x = Bukkit.getServer().getPlayerExact(p);
	}

	final Timer payment = new Timer(); // The timertask to schedule payments

	@SuppressWarnings({ "deprecation", "unused" })
	public boolean onCommand(final CommandSender sender, Command cmd, String label, final String[] args) {

		final Player s = (Player) sender; // The sender variable 's'
		
		int delay = 0; // The delay for the timer

		String payPermission = "salaryx.pay";
		// Check for permission
		if (!s.hasPermission(payPermission)) {
			s.sendMessage("You do not have permission to run this command! " + red + payPermission); // Then do this
			return true;
		}

		if (cmd.getName().equalsIgnoreCase("salary")) { // If they typed /salary

			// Store a list of all the targets
			List<String> targetList = new ArrayList<String>();

			// Check if there are 4 arguments and the first one is "pay"
			if (args.length == 4 && args[0].equalsIgnoreCase("pay")) {
				
				setPlayer(args[1]);
				// Check if the player is indeed a player
				if (sender instanceof Player) {
					if (isOnline(args[1])) { // If the target is online
						
						double has = getBalance(s); // The balance of the sender
						final double amount = Double.parseDouble(args[2]); // Amount being sent
						String interval1 = (args[3] + "000"); // Convert from milliseconds to seconds by adding "000"
						final int interval2 = Integer.parseInt(args[3]); // Convert the string to an int
						final int interval = Integer.parseInt(interval1);  // Convert interval2 to an int
						final Player x = Bukkit.getServer().getPlayerExact(args[1]); // The target
						
						// Check if they try to do less than 5 seconds
						if (interval < 5) {
							s.sendMessage(green + "[$] " + aqua + "You can only pay at a rate of 5 seconds or more!");
							return true;
						}

						// If what they have is greater than the amount they're trying to send, continue.
						if (has > amount) {
							// If the sender is not the target, OR if the sender is an op, continue
							if (s != x || s.isOp()) {
								// Create a timertask (SHOULD BE A THREAD IN A SEPERATE CLASS)
								payment.scheduleAtFixedRate(new TimerTask() {
											
								@Override
								public void run() {
										// Call the addTotalTime and addTotalCash methods
										addTotalTime(interval2);
										addTotalCash(amount);
										// If the sender goes offline, or the target goes offline, cancel the timertask
										if (!isOnline(s.getName())
												|| !isOnline(args[1])) {
											payment.cancel(); 
											getLogger().info("Timer cancelled! Either the target or the sender logged off.");
											return;
										}
										// Take 'amount' out of the sender's pocket
										econ.withdrawPlayer(s, amount);
										// Add 'amount' to the target's pocket
										EconomyResponse r = econ.depositPlayer(x,amount);
										// Notify both parties
										s.sendMessage(green + "[$] "+ aqua + "-$" + amount+ " " + "(" + args[1] + ")"); // Sent
										x.sendMessage(green + "[$] "+ aqua + "+$" + amount+ " " + "("+ sender.getName()+ ")"); // Recieved
									}
								}, delay, interval); 
								return true;
							} else { // Exception
								s.sendMessage(green + "[$] " + aqua+ "You cannot pay yourself!");
								return true;
							}
						} else { // Exception
							s.sendMessage(green + "[$] " + aqua+ "You cannot afford to pay " + args[1]+ "!");
							return true;
						}
					} else { // Exception
						s.sendMessage(green + "[$] " + aqua + "Player "+ args[1] + " is not online!");
						return true;
					}
				} else { // Exception
					getLogger().info("Command only available to players!");
					return true;
				}
				// Cancel command
			} else if (args.length == 1 && args[0].equalsIgnoreCase("cancel")) {
				Player target; // Make this variable equal to the target. Find a way to fetch it from the other method.
				// Notify both parties
				s.sendMessage(green + "[$] " + aqua + "All outgoing payments cancelled!"); 
				x.sendMessage(green + "[$] " + aqua + s.getName() + " has cancelled your salary. You have been paid $" + totalCash + " over a period of " + totalTime + " seconds.");
				try {
					// Cancel the payment, and log it to the console
					payment.cancel();
					getLogger().info(s.getName()+ " has cancelled outgoing payments to "+ targetList.size() + " players.");
				} catch (Exception e) {
					System.out.println(e);
				}

				return true;

			} else {
				s.sendMessage(green + "[$] " + aqua + "Incorrect syntax!");
				return true;
			}

		}

		return false;
	}
}