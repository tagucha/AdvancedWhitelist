package tgcdev.aw;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PluginMain extends JavaPlugin implements Listener {
    private CustomConfig config;

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this,this);
        config = new CustomConfig(this,"whitelist.yml");
        config.saveDefaultConfig();
    }

    private List<UUID> getEternal() {
        List<UUID> uuids = new ArrayList<>();
        for (String arg:config.getConfig().getStringList("eternal")) uuids.add(UUID.fromString(arg));
        return uuids;
    }

    private List<UUID> getInstant() {
        List<UUID> uuids = new ArrayList<>();
        for (String arg:config.getConfig().getStringList("instant")) uuids.add(UUID.fromString(arg));
        return uuids;
    }

    private List<UUID> getList() {
        config.reloadConfig();
        List<UUID> uuids = new ArrayList<>();
        for (String arg:config.getConfig().getStringList("eternal")) uuids.add(UUID.fromString(arg));
        for (String arg:config.getConfig().getStringList("instant")) uuids.add(UUID.fromString(arg));
        return uuids;
    }

    private boolean removeEternal(UUID uuid) {
        List<UUID> uuids = getEternal();
        if (!uuids.contains(uuid)) return false;
        uuids.remove(uuid);
        List<String> args = new ArrayList<>();
        for (UUID uid:uuids) args.add(uid.toString());
        config.getConfig().set("eternal",args);
        config.saveConfig();
        return true;
    }

    private boolean removeInstant(UUID uuid) {
        List<UUID> uuids = getInstant();
        if (!uuids.contains(uuid)) return false;
        uuids.remove(uuid);
        List<String> args = new ArrayList<>();
        for (UUID uid:uuids) args.add(uid.toString());
        config.getConfig().set("instant",args);
        config.saveConfig();
        return true;
    }

    private boolean removeBoth(UUID uuid) {
        return removeEternal(uuid) || removeInstant(uuid);
    }

    private boolean containsEternal(UUID uuid) {
        return getEternal().contains(uuid);
    }

    private boolean containsInstant(UUID uuid) {
        return getInstant().contains(uuid);
    }

    private boolean containsBoth(UUID uuid) {
        return getEternal().contains(uuid) || getInstant().contains(uuid);
    }

    private boolean addEternal(UUID uuid) {
        if (containsInstant(uuid)) removeInstant(uuid);
        List<UUID> uuids = getEternal();
        if (uuids.contains(uuid)) return false;
        uuids.add(uuid);
        List<String> args = new ArrayList<>();
        for (UUID uid:uuids) args.add(uid.toString());
        config.getConfig().set("eternal",args);
        config.saveConfig();
        return true;
    }

    private boolean addInstant(UUID uuid) {
        if (containsEternal(uuid)) removeEternal(uuid);
        List<UUID> uuids = getInstant();
        if (uuids.contains(uuid)) return false;
        uuids.add(uuid);
        List<String> args = new ArrayList<>();
        for (UUID uid:uuids) args.add(uid.toString());
        config.getConfig().set("instant",args);
        config.saveConfig();
        return true;
    }

    private void resetInstant() {
        config.getConfig().set("instant",new ArrayList<String>());
        config.saveConfig();
    }

    private String convertToName(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return (player.isOnline() ? ChatColor.WHITE : ChatColor.GRAY) + player.getName() + ChatColor.RESET;
    }

    private boolean isEnable() {
        return config.getConfig().getBoolean("status",false);
    }

    private void setStatus(boolean isEnable) {
        config.getConfig().set("status",isEnable);
        config.saveConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("whitelist.command")) {
            sender.sendMessage("権限がありません。");
            return true;
        }
        switch (args.length) {
            case 0:
                sender.sendMessage("現在ホワイトリストは" + (isEnable() ? "有効":"無効") + "になっています。");
                sender.sendMessage(String.format("許可されているアカウント(計:%d)",getList().size()));
                List<UUID> eternal = getEternal(),instant = getInstant();
                sender.sendMessage(String.format("永続的に許可されているアカウント(%d)",eternal.size()));
                if (eternal.size() != 0) {
                    String arg = convertToName(eternal.get(0));
                    for (int i = 1;i < eternal.size();i++) arg += "," + convertToName(eternal.get(i));
                    sender.sendMessage(arg);
                }
                sender.sendMessage(String.format("一時的に許可されているアカウント(%d)",instant.size()));
                if (instant.size() != 0) {
                    String arg = convertToName(instant.get(0));
                    for (int i = 1;i < instant.size();i++) arg += "," + convertToName(instant.get(i));
                    sender.sendMessage(arg);
                }
                return true;
            case 1:
                if (args[0].equalsIgnoreCase("help")) {
                    sender.sendMessage("/whitelist : 接続が許可されているアカウントを表示します。");
                    sender.sendMessage("- on : ホワイトリストを有効にします。また、オンラインのプレイヤーは一時的なホワイトリストへ追加されます。");
                    sender.sendMessage("- off : ホワイトリストを無効にします。また、一時的なホワイトリストに追加されていたプレイヤーは削除されます。");
                    sender.sendMessage("- add [instant|eternal] <player> : ホワイトリストにプレイヤーを追加します。instant:一時的,eternal:永続");
                    sender.sendMessage("- remove <player> : ホワイトリストからプレイヤーを削除します。");
                    sender.sendMessage("- reload : whitelist.ymlを再読み込みします。");
                    sender.sendMessage("- clear : 一時的なホワイトリストに追加されているプレイヤーを全て削除します。");
                    sender.sendMessage("- help : このメッセージを表示します。");
                    return true;
                } else if (args[0].equalsIgnoreCase("on")) {
                    if (isEnable()) {
                        sender.sendMessage("既にホワイトリストは有効です。");
                        return true;
                    }
                    setStatus(true);
                    for (Player player:Bukkit.getOnlinePlayers()) {
                        UUID uuid = player.getUniqueId();
                        if (containsEternal(uuid)) continue;
                        addInstant(uuid);
                    }
                    sender.sendMessage("ホワイトリストを有効にしました。");
                    return true;
                } else if (args[0].equalsIgnoreCase("off")) {
                    if (!isEnable()) {
                        sender.sendMessage("既にホワイトリストは無効です。");
                        return true;
                    }
                    resetInstant();
                    setStatus(false);
                    sender.sendMessage("ホワイトリストを無効にしました。");
                    return true;
                } else if (args[0].equalsIgnoreCase("reload")) {
                    config.reloadConfig();
                    sender.sendMessage("whitelist.ymlを再読み込みしました。");
                    return true;
                } else if (args[0].equalsIgnoreCase("clear")) {
                    resetInstant();
                    sender.sendMessage("一時的なホワイトリストに追加されていたプレイヤーを削除しました。");
                    return true;
                }
                break;
            case 2:
                if (args[0].equalsIgnoreCase("remove")) {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(args[1]);
                    if (removeBoth(player.getUniqueId())) {
                        sender.sendMessage(player.getName() + "をホワイトリストから削除しました。");
                    } else {
                        sender.sendMessage(player.getName() + "はホワイトリストに追加されていません。");
                    }
                    return true;
                }
                break;
            case 3:
                if (args[0].equalsIgnoreCase("add")) {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(args[2]);
                    if (args[1].equalsIgnoreCase("eternal")) {
                        if (addEternal(player.getUniqueId())){
                            sender.sendMessage(player.getName() + "を永続するホワイトリストに追加しました。");
                        } else {
                            sender.sendMessage(player.getName() + "は既にホワイトリストに追加されています。");
                        }
                        return true;
                    } else if (args[1].equalsIgnoreCase("instant")) {
                        if (addInstant(player.getUniqueId())){
                            sender.sendMessage(player.getName() + "を一時的なホワイトリストに追加しました。");
                        } else {
                            sender.sendMessage(player.getName() + "は既に一時的なホワイトリストに追加されています。");
                        }
                        return true;
                    } else {
                        sender.sendMessage("二番目の引数にはeternalまたはinstantを指定してください。");
                    }
                }
                break;
        }
        sender.sendMessage("コマンドの型が不正です。使い方は「/whitelist help」を参照してください。");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> commands = new ArrayList<>();
        if (!sender.hasPermission("whitelist.command")) return commands;
        switch (args.length) {
            case 1:
                if ("on".contains(args[0])) commands.add("on");
                if ("off".contains(args[0])) commands.add("off");
                if ("help".contains(args[0])) commands.add("help");
                if ("add".contains(args[0])) commands.add("add");
                if ("remove".contains(args[0])) commands.add("remove");
                if ("reload".contains(args[0])) commands.add("reload");
                if ("clear".contains(args[0])) commands.add("clear");
                break;
            case 2:
                if (args[0].equalsIgnoreCase("add")) {
                    if ("eternal".contains(args[1])) commands.add("eternal");
                    if ("instant".contains(args[1])) commands.add("instant");
                } else if (args[0].equalsIgnoreCase("remove")) {
                    for (UUID uuid:getList()) {
                        String name = Bukkit.getOfflinePlayer(uuid).getName();
                        if (name == null) continue;
                        if (name.contains(args[1])) commands.add(name);
                    }
                }
                break;
            case 3:
                if (args[0].equalsIgnoreCase("add") && (args[1].equalsIgnoreCase("eternal") || args[1].equalsIgnoreCase("instant")))
                    for (Player player:Bukkit.getOnlinePlayers())
                        if (player.getName().contains(args[1])) commands.add(player.getName());
                break;
        }
        return commands;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLogin(PlayerLoginEvent event) {
        if (!isEnable()) return;
        if (event.getPlayer().hasPermission("whitelist.join")) return;
        UUID uuid = event.getPlayer().getUniqueId();
        if (containsBoth(uuid)) return;
        PlayerLoginEvent.Result result = PlayerLoginEvent.Result.KICK_WHITELIST;
        String message = "ホワイトリストが有効になっています";
        event.disallow(result,message);
    }
}
