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

public class ExPrivileges extends JavaPlugin {
	
	Logger log = getLogger();
	String prefix = "[ExPrivileges] ";
	String version = "0.1";
	Calendar calendar = new GregorianCalendar();
	File players = new File(this.getDataFolder(), "players.yml");
	FileConfiguration confPlayers = YamlConfiguration.loadConfiguration(players);
	
	public void onEnable(){ 
		
		log.info("ON");
		
		BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                log.info("Проверяю нет ли игроков с истекшим сроком привилегий.");
                Set<String> items = confPlayers.getKeys(false);
                /* Вывод всех игроков
                for( String entry : items ){
                	System.out.print( entry );
                }
                */
                boolean flagReload = false;
                if (items != null) {
	                for (String entry : items) {
	                	if (confPlayers.get(entry+".privilege", null) != null) {
		                	Date date_expires = (Date) confPlayers.get(entry+".date_expires", 0);
		                	if (calendar.getTime().after(date_expires)) {
		                		
		                		String privilegeName = confPlayers.get(entry+".privilege", "").toString();
		                		log.info("Снимаю права "+privilegeName+" c игрока "+entry);
		                		getServer().dispatchCommand(getServer().getConsoleSender(), "pex user "+entry+" group remove "+privilegeName);
		                		confPlayers.set(entry, "");
		                		flagReload = true;
		                	}
	                	}
	                }
                } else {
                	log.info("Файл с игроками пуст!");
                }
                
                if (flagReload) {
                	
                	log.info("Было найдено и удалено несколько пользователей.");
                	getServer().dispatchCommand(getServer().getConsoleSender(), "pex reload");
                	try {
            			confPlayers.save(players);
            		} catch (IOException e) {
            			//e.printStackTrace();
            			log.info("Ошибка при сохранении данных о пользователе в файл!");
            		}
                }
                else {

            		log.info("Пользователей с истекшими привилегиями найдено не было.");
            		
                }
            }
        }, 0L, 864000L);
        
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
		
		if(cmd.getName().equalsIgnoreCase("ep")){
			
			if(args.length != 0) {
				
				switch (args[0]) {
					case "add":
						
						if (args.length == 4) {
							if (player == null) {
								
								addPlayer(args[1], args[2], args[3], sender);
								
							} 
							else {
								
								if (sender.hasPermission("exprivileges.add")) addPlayer(args[1], args[2], args[3], sender);
									            
							}
						} else sender.sendMessage("/ep add [player] [privilege] [days]");
						return true;
					case "reload":
					try {
						confPlayers.load(players);
					} catch (IOException | InvalidConfigurationException e) {
						sender.sendMessage(this.prefix + "Ошибка при перезагрузке плагина.");
						return false;
					}
						sender.sendMessage(this.prefix + "Плагин перезагружен.");
						return true;
					case "version":
						sender.sendMessage(this.prefix + "Версия плагина: "+this.version);
						return true;
					case "remove":
						
						if (args.length == 2) {
							if (player == null) {
								
								removePlayer(args[1], sender);
								
							} 
							else {
								
								if (sender.hasPermission("exprivileges.remove")) removePlayer(args[1], sender);
									            
							}
						} else sender.sendMessage("/ep remove [player]");
						return true;
					case "help":
						sender.sendMessage(this.prefix + "Доступные команды:");
						sender.sendMessage("/ep add [player] [privilege] [days]" + " - добавить игроку привилегию на [days] дней");
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
	
	public int checkCommandAdd(String playerName, String privilegeName, String timeStr) {
		
		int time = Integer.parseInt(timeStr);
		
		if (time != 0) return 1;
		else return 0;
		
	}
	
	public void addPlayer(String playerName, String privilegeName, String timeStr, CommandSender sender) {

		if (checkCommandAdd(playerName, privilegeName, timeStr) == 1) {
			
			sender.sendMessage(this.prefix + "Игрок " + playerName + " был добавлен в группу " + privilegeName + " на " +timeStr+ " дней!");
            //int time = Integer.parseInt(timeStr);
    		getServer().dispatchCommand(getServer().getConsoleSender(), "pex user "+playerName+" group add "+privilegeName);
    		confPlayers.set(playerName+".privilege", privilegeName);
    		confPlayers.set(playerName+".time", timeStr);
    		
    		Date date = calendar.getTime();
    		int time = Integer.parseInt(timeStr);
    		Calendar calendar_expires = calendar;
    		calendar_expires.add(Calendar.DAY_OF_YEAR, time);
    		Date date_expires = calendar_expires.getTime();

    		confPlayers.set(playerName+".date", date);
    		confPlayers.set(playerName+".date_expires", date_expires);
    		// etc.
    		try {
    			confPlayers.save(players);
    		} catch (IOException e) {
    			//e.printStackTrace();
    			sender.sendMessage(this.prefix + "Ошибка при сохранении данных о пользователе в файл!");
    		}
    		

    		
			} else {
			
			sender.sendMessage(this.prefix + "Ошибка во вводе команды!");
			  
          }
	}
	
	public void removePlayer(String playerName, CommandSender sender) {

			sender.sendMessage(this.prefix + "Игрок " + playerName + " был удален из списков плагина. Привилегии сняты не были!");
			confPlayers.set(playerName, "");
			try {
    			confPlayers.save(players);
    		} catch (IOException e) {
    			//e.printStackTrace();
    			log.info("Ошибка при сохранении данных о пользователе в файл!");
    		}
	}
	
	
}
