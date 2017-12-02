package ru.flametaichou;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
//import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public class TempGroups extends JavaPlugin {
	
	Logger log = getLogger(); 
	String prefix = "[TempGroups] ";
	String logPrefix = "[LOG] ";
	String version = "0.5";
	int confMinutes;
	String confPlugin;
	String confDefaultGroup;
	File filePlayers = new File(this.getDataFolder(), "players.yml");
	protected FileConfiguration config;
	FileConfiguration confPlayers = YamlConfiguration.loadConfiguration(filePlayers);
	
	public void loadConfiguration(){
		
	    this.getConfig().addDefault("period", 720);
	    this.getConfig().addDefault("plugin", "pex");
	    this.getConfig().addDefault("gm-default-group", "default");
	    this.getConfig().options().copyDefaults(true); 
	    this.saveConfig();
	}
	
	public void reloadCfg(){
		
		this.reloadConfig();
		confMinutes = this.getConfig().getInt("period");
		confPlugin = this.getConfig().getString("plugin");
		confDefaultGroup = this.getConfig().getString("gm-default-group");
	}
	
	
	public void onEnable(){ 
		
		log.info("ON");
		loadConfiguration();
		reloadCfg();
		
		BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                log.info("Проверяю нет ли игроков с истекшим сроком привилегий.");
                Set<String> playersSet = confPlayers.getKeys(false);
                boolean flagReload = false;
                Set<String> groupsList;
                Date date_expires;
                String groupName;
                if (playersSet != null) {
	                for (String playerSetEntry : playersSet) {
	                    groupsList = confPlayers.getConfigurationSection(playerSetEntry).getKeys(false);
	                    if (groupsList != null) for (String groupSetEntry : groupsList) {
		                	if (!(confPlayers.get(playerSetEntry+"."+groupSetEntry, "").equals(""))) {
			                	date_expires = (Date) confPlayers.get(playerSetEntry+"."+groupSetEntry, 0);

			                	Calendar calendar = new GregorianCalendar();
			                	if (calendar.getTime().after(date_expires)) {
			                		
			                		groupName = groupSetEntry;
			                		log.info("Снимаю права "+groupName+" c игрока "+playerSetEntry);
			                		
			                		switch (confPlugin) {
			                			case "pex":
			                				getServer().dispatchCommand(getServer().getConsoleSender(), "pex user "+playerSetEntry+" group remove "+groupName);
			                				break;
			                			case "gm":
			                				getServer().dispatchCommand(getServer().getConsoleSender(), "manuadd "+playerSetEntry+" "+confDefaultGroup);
			                				break;
			                			case "gm-sub":
			                				getServer().dispatchCommand(getServer().getConsoleSender(), "manudelsub "+playerSetEntry+" "+groupName);
			                				break;
			                		}
			                		
			                		confPlayers.set(playerSetEntry+"."+groupSetEntry, "");
			                		flagReload = true;
			                	}
		                	}
	                    }
	                }
                } else {
                	log.info("Файл с игроками пуст!");
                }
                
                if (flagReload) {
                	
                	log.info("Было найдено и удалено несколько пользователей.");
                	if (confPlugin.equals("pex")) getServer().dispatchCommand(getServer().getConsoleSender(), "pex reload");
                	try {
            			confPlayers.save(filePlayers);
            		} catch (IOException e) {
            			//e.printStackTrace();
            			log.info("Ошибка при сохранении данных о пользователе в файл!");
            		}
                }
                else {

            		log.info("Пользователей с истекшими привилегиями найдено не было.");
            		
                }
            }
        }, 0L, 1200L*confMinutes);
        
		//PluginManager pm = this.getServer().getPluginManager();
		
	}

	public void onDisable(){ 

		log.info("off");
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
		
		Player player = null;
		if (sender instanceof Player) {
			
			player = (Player) sender;
			
		}
		
		if(cmd.getName().equalsIgnoreCase("tg")){
			
			if(args.length != 0) {
				
				switch (args[0]) {
					case "add":
						
						if (args.length == 4) {
							if (player == null) {
								
								addPlayer(args[1], args[2], args[3], sender);
								
							} 
							else {
								
								if (sender.hasPermission("tempgroups.add")) addPlayer(args[1], args[2], args[3], sender);
									            
							}
						} else sender.sendMessage("/ep add [player] [group] [days]");
						return true;
					case "reload":
					try {
						confPlayers.load(filePlayers);
						reloadCfg();
					} catch (IOException | InvalidConfigurationException e) {
						sender.sendMessage(this.prefix + "Ошибка при перезагрузке плагина.");
						log.info(this.logPrefix + "Ошибка при перезагрузке плагина.");
						return false;
					}
						sender.sendMessage(this.prefix + "Плагин перезагружен. Используемый плагин прав: "+confPlugin);
						log.info(this.logPrefix + "Плагин перезагружен. Используемый плагин прав: "+confPlugin);
						return true;
					case "version":
						sender.sendMessage(this.prefix + "Версия плагина: "+this.version);
						return true;
					case "remove":
						
						if (args.length == 3) {
							if (player == null) {
								
								removePlayer(args[1], args[2], sender);
								
							} 
							else {
								
								if (sender.hasPermission("tempgroups.remove")) removePlayer(args[1], args[2], sender);
									            
							}
						} else sender.sendMessage("/ep remove [player] [group]");
						return true;
					case "help":
						sender.sendMessage(this.prefix + "Доступные команды:");
						sender.sendMessage("/ep add [player] [group] [days]" + " - добавить игроку привилегию на [days] дней");
						sender.sendMessage("/ep remove [player]" + " - удалить игрока из списков плагина");
						sender.sendMessage("/ep reload" + " - перезагрузить плагин");
						sender.sendMessage("/ep version" + " - узнать версию плагина");
						sender.sendMessage("/ep help" + " - вывести справку");
						return true;
					}
                } else return false;
					
		}
		return false; 
	}
	
	public int checkCommandAdd(String playerName, String groupName, String timeStr) {
		
		int time = Integer.parseInt(timeStr);
		
		if (time != 0) return 1;
		else return 0;
		
	}
	
	public void addPlayer(String playerName, String groupName, String timeStr, CommandSender sender) {

		if (checkCommandAdd(playerName, groupName, timeStr) == 1) {
			
			sender.sendMessage(this.prefix + "Игрок " + playerName + " был добавлен в группу " + groupName + " на " +timeStr+ " дней!");
			log.info(this.logPrefix + "Игрок " + playerName + " был добавлен в группу " + groupName + " на " +timeStr+ " дней!");

    		switch (confPlugin) {
    			case "pex":
    				getServer().dispatchCommand(getServer().getConsoleSender(), "pex user "+playerName+" group add "+groupName);
    				break;
    			case "gm":
    				getServer().dispatchCommand(getServer().getConsoleSender(), "manuadd "+playerName+" "+groupName);
    				break;
    			case "gm-sub":
    				getServer().dispatchCommand(getServer().getConsoleSender(), "manuaddsub "+playerName+" "+groupName);
    				break;
    		}
    		
    		int time = Integer.parseInt(timeStr);

    		Calendar calendar = new GregorianCalendar();
    		calendar.add(Calendar.DAY_OF_YEAR, time);
    		Date date_expires = calendar.getTime();
    		confPlayers.set(playerName+"."+groupName, date_expires);
    		
    		try {
    			confPlayers.save(filePlayers);
    		} catch (IOException e) {
    			//e.printStackTrace();
    			sender.sendMessage(this.prefix + "Ошибка при сохранении данных о пользователе в файл!");
    			log.info(this.logPrefix + "Ошибка при сохранении данных о пользователе в файл!");
    		}
    		

    		
			} else {
			
			sender.sendMessage(this.prefix + "Ошибка во вводе команды!");
			log.info(this.logPrefix + "Ошибка во вводе команды!");
			  
          }
	}
	
	public void removePlayer(String playerName, String groupName, CommandSender sender) {

			sender.sendMessage(this.prefix + "Дата снятия привилегии "+groupName+" c игрока " + playerName + " была удалена из списков плагина. Права сняты не были!");
			log.info(this.logPrefix + "Дата снятия привилегии "+groupName+" c игрока " + playerName + " была удалена из списков плагина. Права сняты не были!");
			confPlayers.set(playerName+"."+groupName, "");
			try {
    			confPlayers.save(filePlayers);
    		} catch (IOException e) {
    			//e.printStackTrace();
    			sender.sendMessage(this.prefix + "Ошибка при сохранении данных о пользователе в файл!");
    			log.info(this.logPrefix + "Ошибка при сохранении данных о пользователе в файл!");
    		}
	}
	
	
}
