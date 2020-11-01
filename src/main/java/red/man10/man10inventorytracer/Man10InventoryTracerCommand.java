package red.man10.man10inventorytracer;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.omg.CORBA.INTERNAL;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * Created by sho on 2018/03/24.
 */
public class Man10InventoryTracerCommand implements CommandExecutor {
    private static Man10InventoryTracer plugin;
    public Man10InventoryTracerCommand(Man10InventoryTracer pluginn){
        plugin = pluginn;
    }
    String prefix = "§e[§dMan10Inventory§e]§b§l";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player)){
            return false;
        }
        Player p = (Player) sender;
        if(args.length == 0){
            p.sendMessage("§d=====[Man10InventoryTracer]=====");
            p.sendMessage("§emit view <player> <id>  バックアップ表示");
            p.sendMessage("§emit list <player> <page> バックアップのID表示");
            p.sendMessage("§emit set <player> <id> インベントリを適用");
            p.sendMessage("§emit help ヘルプ表示");
            p.sendMessage("§d==============================");
            p.sendMessage("§d§lCreated By Sho0");
            return false;
        }
        if(args.length == 1){
            if(args[0].equalsIgnoreCase("help")){
                p.sendMessage("§d=====[Man10InventoryTracer]=====");
                p.sendMessage("§emit view <player> <id>  バックアップ表示");
                p.sendMessage("§emit list <player> <page> バックアップのID表示");
                p.sendMessage("§emit set <player> <id> インベントリを適用");
                p.sendMessage("§emit help ヘルプ表示");
                p.sendMessage("§d==============================");
                p.sendMessage("§d§lCreated By Sho0");
                return false;
            }
        }
        if(args.length == 2){
            if(args[0].equalsIgnoreCase("list")){
                if(p.hasPermission("man10.inventorytracer.list")){
                    int page = 0;
                    UUID target = plugin.pda.getUUIDFromName(args[1]);
                    if(target == null){
                        p.sendMessage(prefix + "§4そのプレイヤーは存在しません");
                        return false;
                    }
                    plugin.playerHashmap.put(target, plugin.getInventoryData(target));
                    plugin.playerHashmapTime.put(target, plugin.getInventoryTime(target));
                    if(plugin.playerHashmap.get(target).size() == 0){
                        p.sendMessage(prefix + "§4バックアップデータが存在しません");
                        return false;
                    }
                    if(plugin.playerHashmap.get(target).size()/10 < page){
                        p.sendMessage(prefix + "§4ページが存在しません");
                        return false;
                    }
                    p.sendMessage("§d§l==" + args[1] + "のバックアップ==");
                    page = page * 10;
                    for(int i = page;i < 10 + page;i++){
                        try{
                            p.sendMessage("§a" + i  + "  §4" + plugin.playerHashmapTime.get(target).get(i));
                        }catch (Exception e){
                        }
                    }
                }else{
                    p.sendMessage(prefix + "§4あなたには権限がありません");
                    return false;
                }
            }
        }
        if(args.length == 3){
            if(args[0].equalsIgnoreCase("set")){
                if(p.hasPermission("man10.inventorytracer.set")){
                    UUID target = plugin.pda.getUUIDFromName(args[1]);
                    if(target == null){
                        p.sendMessage(prefix + "§4そのプレイヤーは存在しません");
                        return false;
                    }
                    if(!Bukkit.getPlayer(target).isOnline()) {
                        p.sendMessage(prefix + "§4そのプレイヤーはオンラインではありません");
                        return false;
                    }
                    plugin.playerHashmap.put(target, plugin.getInventoryData(target));
                    plugin.playerHashmapTime.put(target, plugin.getInventoryTime(target));
                    if(plugin.playerHashmap.get(target).size() == 0){
                        p.sendMessage(prefix + "§4バックアップデータが存在しません");
                        return false;
                    }
                    try{
                        int id = Integer.parseInt(args[2]);
                        if(plugin.playerHashmap.get(target).size() < id){
                            p.sendMessage(prefix + "§4そのIDのバックアップは存在しません");
                            return false;
                        }
                        Bukkit.getPlayer(target).getInventory().setContents(plugin.createItemStacks(plugin.playerHashmap.get(target).get(id)));
                        Bukkit.getPlayer(target).sendMessage(prefix + "§d§lあなたのインベントリは" + plugin.playerHashmapTime.get(target).get(id) + "のバックアップから復元されました");
                        p.sendMessage(prefix + "§d§lインベントリの復元が完了しました");
                        return false;
                    }catch (NumberFormatException e){
                        p.sendMessage(prefix + "§4IDは数字でなくてはなりません");
                        return false;
                    }
                }else{
                    p.sendMessage(prefix + "§4あなたには権限がありません");
                }
            }
            if(args[0].equalsIgnoreCase("view")){
                if(p.hasPermission("man10.inventorytracer.view")){
                    UUID target = plugin.pda.getUUIDFromName(args[1]);
                    if(target == null){
                        p.sendMessage(prefix + "§4そのプレイヤーは存在しません");
                        return false;
                    }
                    plugin.playerHashmap.put(target, plugin.getInventoryData(target));
                    plugin.playerHashmapTime.put(target, plugin.getInventoryTime(target));
                    if(plugin.playerHashmap.get(target).size() == 0){
                        p.sendMessage(prefix + "§4バックアップデータが存在しません");
                        return false;
                    }
                    try{
                        int id = Integer.parseInt(args[2]);
                        if(plugin.playerHashmap.get(target).size() < id){
                            p.sendMessage(prefix + "§4そのIDのバックアップは存在しません");
                            return false;
                        }
                        p.openInventory(plugin.createBackUpInventoryString(plugin.playerHashmap.get(target).get(id), args[1]));
                    }catch (NumberFormatException e){
                        p.sendMessage(prefix + "§4IDは数字でなくてはなりません");
                        return false;
                    }
                }else{
                    p.sendMessage(prefix + "§4あなたには権限がありません");
                }
                return false;
            }
            if(args[0].equalsIgnoreCase("list")){
                if(p.hasPermission("man10.inventorytracer.list")){
                    int page = 0;
                    try{
                        page = Integer.parseInt(args[2]);
                    }catch (NumberFormatException e){
                        p.sendMessage(prefix + "§4ページは数字でなくてはなりません");
                        return false;
                    }
                    UUID target = plugin.pda.getUUIDFromName(args[1]);
                    if(target == null){
                        p.sendMessage(prefix + "§4そのプレイヤーは存在しません");
                        return false;
                    }
                    plugin.playerHashmap.put(target, plugin.getInventoryData(target));
                    plugin.playerHashmapTime.put(target, plugin.getInventoryTime(target));
                    if(plugin.playerHashmap.get(target).size() == 0){
                        p.sendMessage(prefix + "§4バックアップデータが存在しません");
                        return false;
                    }
                    if(plugin.playerHashmap.get(target).size()/10 < page){
                        p.sendMessage(prefix + "§4ページが存在しません");
                        return false;
                    }
                    page = page * 10;
                    p.sendMessage("§d§l==" + args[1] + "のバックアップ==");
                    for(int i = page;i < 10 + page;i++){
                        try{
                            p.sendMessage("§a" + i  + "  §4" + plugin.playerHashmapTime.get(target).get(i));
                        }catch (Exception e){
                        }
                    }
                }else{
                    p.sendMessage(prefix + "§4あなたには権限がありません");
                    return false;
                }
            }
        }
        return false;
    }
}
