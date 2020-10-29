package red.man10.man10inventorytracer;


import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Man10InventoryTracer extends JavaPlugin implements Listener {

    public BiMap<ItemStack, Integer> map = HashBiMap.create();


    public MySQLManager mysql = null;

    ExecutorService service = Executors.newFixedThreadPool(20);

    public HashMap<UUID, List<String>> playerHashmap = new HashMap<>();
    public HashMap<UUID, List<String>> playerHashmapTime = new HashMap<>();


    String tableInventory = "CREATE TABLE `man10_inventory_database` (\n" +
            "\t`id` BIGINT(20) NOT NULL AUTO_INCREMENT,\n" +
            "\t`name` VARCHAR(32) NULL DEFAULT '0',\n" +
            "\t`uuid` VARCHAR(64) NULL DEFAULT '0',\n" +
            "\t`data` TEXT NULL,\n" +
            "\t`date_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,\n" +
            "\tPRIMARY KEY (`id`)\n" +
            ")\n" +
            "COLLATE='utf8_general_ci'\n" +
            "ENGINE=InnoDB\n" +
            ";\n";

    String tableItem = "CREATE TABLE `man10_item_database` (\n" +
            "\t`id` BIGINT(20) NOT NULL AUTO_INCREMENT,\n" +
            "\t`type` VARCHAR(128) NULL DEFAULT '0',\n" +
            "\t`display_name` VARCHAR(512) NULL DEFAULT '0',\n" +
            "\t`data` TEXT NULL,\n" +
            "\t`date_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,\n" +
            "\tPRIMARY KEY (`id`)\n" +
            ")\n" +
            "COLLATE='utf8_general_ci'\n" +
            "ENGINE=InnoDB\n" +
            ";\n";

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        Bukkit.getServer().getPluginManager().registerEvents(this,this);
        getCommand("man10inventorytracer").setExecutor(new Man10InventoryTracerCommand(this));
        getCommand("mit").setExecutor(new Man10InventoryTracerCommand(this));
        mysql = new MySQLManager(this, "Man10Inventory");
        mysql.execute(tableInventory);
        mysql.execute(tableItem);
        Thread t = new Thread(
                () -> {
                    ResultSet rs = mysql.query("SELECT id,data FROM man10_item_database");
                    try {
                        while(rs.next()){
                            map.put(itemFromBase64(rs.getString("data"), 1),rs.getInt("id"));
                        }
                        rs.close();
                        mysql.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
        );
        t.start();
        new BukkitRunnable() {
            @Override
            public void run() {
                Thread t = new Thread(
                        () -> {
                            String str = "INSERT INTO man10_inventory_database (`id`,`name`,`uuid`,`data`,`date_time`) VALUES " +"";
                            for(Player p : Bukkit.getServer().getOnlinePlayers()){
                                str += getRegisterPlayerInventoryQuery(p);
                            }
                            String FinalQuery = str.substring(0, str.length() - 1);
                            mysql.execute(FinalQuery);
                        }
                );
                t.start();
            }
        }.runTaskTimer(this,  2400, 2400);
    }

    public int getItemId(ItemStack item){
        if(item == null){
            return 0;
        }
        int id = -1;
        if(!map.containsKey(item)){
            id = mysql.executegetid("INSERT INTO man10_item_database (`id`,`type`,`display_name`,`data`,`date_time`) VALUES ('0','" + getMysqlRealScapeString(item.getType().name()) + "','" + getMysqlRealScapeString(item.getItemMeta().getDisplayName()) + "','" + getMysqlRealScapeString(itemToBase64(item)) + "','" + mysql.currentTimeNoBracket() + "');");
            map.put(item, id);
        }else{
            return map.get(item);
        }
        return id;
    }


    public ItemStack getItem(int id){
        if(id == 0){
            return null;
        }
        if(id == -1){
            return null;
        }
        String data = null;
        if(!map.containsValue(id)){
            ResultSet rs = mysql.query("SELECT data FROM man10_item_database WHERE id ='" + id  + "'");
            try {
                while (rs.next()){
                    data = rs.getString("data");
                }
                rs.close();
                mysql.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }else{
            return map.inverse().get(id);
        }
        ItemStack item = itemFromBase64(data, 1);
        this.map.put(item, id);
        return item;
    }

    public void registerPlayerInventory(Player p){
        Runnable r = () -> {
            Inventory inv = p.getInventory();
            String query = "";
            for(int i = 0; i < inv.getContents().length; i++){
                query += getItemId(inv.getContents()[i]) + "|" + inv.getContents()[i].getAmount() + ",";
            }
            String FinalQuery = query.substring(0, query.length() - 1);
            mysql.execute("INSERT INTO man10_inventory_database (`id`,`name`,`uuid`,`data`,`date_time`) VALUES ('0','" + p.getName() + "','" + p.getUniqueId() + "','" + FinalQuery + "','" + mysql.currentTimeNoBracket() + "');");
        };
        service.submit(r);
    }

    public String getRegisterPlayerInventoryQuery(Player p){
            Inventory inv = p.getInventory();
            String query = "";
            for(int i = 0; i < inv.getContents().length; i++){
                query += getItemId(inv.getContents()[i]);
            }
            String FinalQuery = query.substring(0, query.length() - 1);
        String queryy = "('0','" + p.getName() + "','" + p.getUniqueId() + "','" + FinalQuery + "','" + mysql.currentTimeNoBracket() + "'),";
        return queryy;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        Thread t = new Thread(
                () -> {
                    String str = "INSERT INTO man10_inventory_database (`id`,`name`,`uuid`,`data`,`date_time`) VALUES" +"";
                    for(Player p : Bukkit.getServer().getOnlinePlayers()){
                        str += getRegisterPlayerInventoryQuery(p);
                    }
                    String FinalQuery = str.substring(0, str.length() - 1);
                    Bukkit.getLogger().info(FinalQuery);
                    mysql.execute(FinalQuery);
                }
        );
        t.start();
    }



    @EventHandler
    public void onLeave(PlayerQuitEvent e){
        registerPlayerInventory(e.getPlayer());
    }

    public Inventory createBackUpInventory(int id){
        String data = "";
        ResultSet rs = mysql.query("SELECT data FROM man10_inventory_database WHERE id ='" + id + "'");
        try {
            while (rs.next()){
                data = rs.getString("data");
            }
            rs.close();
            mysql.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Inventory inv = Bukkit.createInventory(null, 45, "test");
        String[] stirngs = data.split(",");
        for(int i = 0;i < stirngs.length - 5;i++){
            String[] a = stirngs[i].split("|");
            ItemStack item = getItem(Integer.parseInt(stirngs[i]));
            if (item != null) {
                item.setAmount(Integer.parseInt(a[1]));
            }
            inv.setItem(i + 9, item);
        }
        for(int i = 0;i < 5;i++){
            String[] a = stirngs[i].split("|");
            ItemStack item = getItem(Integer.parseInt(stirngs[i + 36]));
            if (item != null) {
                item.setAmount(Integer.parseInt(a[1]));
            }
            inv.setItem(i, item);
        }

        return inv;
    }

    public Inventory createBackUpInventoryString(String string, String name){
        String data = string;
        Inventory inv = Bukkit.createInventory(null, 45, "§d§l" + name + "のインベントリ");
        String[] stirngs = data.split(",");
        for(int i = 0;i < stirngs.length - 5;i++){
            inv.setItem(i + 9, getItem(Integer.parseInt(stirngs[i])));
        }
        for(int i = 0;i < 5;i++){
            inv.setItem(i, getItem(Integer.parseInt(stirngs[i + 36])));
        }

        return inv;
    }

    public ItemStack[] createItemStacks(String data){
        String[] stirngs = data.split(",");
        ItemStack[] items = new ItemStack[stirngs.length];
        for(int i = 0;i < stirngs.length;i++){
            items[i] = getItem(Integer.parseInt(stirngs[i]));
        }
        return items;
    }

    public static synchronized String getMysqlRealScapeString(String str) {
        String data = null;
        if (str != null && str.length() > 0) {
            str = str.replace("\\", "\\\\");
            str = str.replace("'", "\\'");
            str = str.replace("\0", "\\0");
            str = str.replace("\n", "\\n");
            str = str.replace("\r", "\\r");
            str = str.replace("\"", "\\\"");
            str = str.replace("\\x1a", "\\Z");
            data = str;
        }
        return data;
    }

    public static ItemStack itemFromBase64(String data, int amount) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack[] items = new ItemStack[dataInput.readInt()];

            // Read the serialized inventory
            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }

            dataInput.close();
            return items[0];
        } catch (Exception e) {
            return null;
        }
    }

    public static String itemToBase64(ItemStack item) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            ItemStack[] items = new ItemStack[1];
            item.setAmount(1);
            items[0] = item;
            dataOutput.writeInt(items.length);

            for (int i = 0; i < items.length; i++) {
                dataOutput.writeObject(items[i]);
            }

            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    public List<String> getInventoryData(UUID uuid){
        ResultSet rs = mysql.query("SELECT data,date_time FROM man10_inventory_database WHERE uuid = '" + uuid + "'ORDER BY id DESC");
        List<String> list = new ArrayList<>();
        try {
            while (rs.next()){
                list.add(rs.getString("data"));
            }
            rs.close();
            mysql.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<String> getInventoryTime(UUID uuid){
        ResultSet rs = mysql.query("SELECT date_time FROM man10_inventory_database WHERE uuid = '" + uuid + "'ORDER BY id DESC");
        List<String> list = new ArrayList<>();
        try {
            while (rs.next()){
                list.add(rs.getString("date_time"));
            }
            rs.close();
            mysql.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}

