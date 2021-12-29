package net.zerotoil.dev.cyberlevels.objects;

import net.zerotoil.dev.cyberlevels.CyberLevels;
import net.zerotoil.dev.cyberlevels.utilities.LangUtils;
import net.zerotoil.dev.cyberlevels.utilities.LevelUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RewardObject {

    private final CyberLevels main;
    private final LangUtils lang;
    private final LevelUtils utils;

    private List<String> commands;
    private List<String> messages;
    private List<Long> levels;

    public RewardObject(CyberLevels main, String rewardName) {
        this.main = main;
        this.lang = main.langUtils();
        this.utils = main.levelUtils();

        commands = lang.convertList(main.getFiles().getFile("rewards"), "rewards." + rewardName + ".commands");
        messages = lang.convertList(main.getFiles().getFile("rewards"), "rewards." + rewardName + ".messages");
        levels = new ArrayList<>();
        for (String s : lang.convertList(main.getFiles().getFile("rewards"), "rewards." + rewardName + ".levels")) {
            levels.add(Long.parseLong(s));
            main.levelCache().levelData().get(Long.parseLong(s)).addReward(this);
        }
    }

    public void giveReward(Player player) {
        sendCommands(player);
        sendMessage(player);
    }

    private String parseFormat(String prefix, String line) {
        if (line.toLowerCase().startsWith(prefix))
            line = line.substring(prefix.length());
        while (line.charAt(0) == ' ') line = line.substring(1);
        return line;
    }

    private void sendCommands(Player player) {
        if (commands == null) return;
        for (String command : commands) {
            while (command.charAt(0) == ' ') command = command.substring(1);

            if (!command.startsWith("[") || command.toLowerCase().startsWith("[console]")) {
                command = parseFormat("[console]", command);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), utils.getPlaceholders(command, player, true));
                continue;
            }

            if (command.toLowerCase().startsWith("[player]")) {
                command = parseFormat("[player]", command);
                Bukkit.dispatchCommand(player, utils.getPlaceholders(command, player, true));
            }
        }
    }

    public void typeMessage(Player player, String line) {
        if (line.toLowerCase().startsWith("[actionbar]"))
            lang.actionBar(player, parseFormat("[actionbar]", line));
        else if (line.toLowerCase().startsWith("[title]")) {
            lang.title(player, parseFormat("[title]", line).split("<n>"), null);
        }
        else if (line.toLowerCase().startsWith("[json]") && line.contains("{\"text\":"))
            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(), "minecraft:tellraw " +
                    player.getName() + " " + parseFormat("[json]", line)
            );
        else lang.sendMixed(player, line);
    }

    private void sendMessage(Player player) {
        if (messages == null) return;
        for (String message : messages) {
            if (message == null || message.equals("")) continue;
            while (message.charAt(0) == ' ') message = message.substring(1);

            message = utils.getPlaceholders(message, player, true);

            if (message.toLowerCase().startsWith("[player]"))
                typeMessage(player, parseFormat("[player]", message));
            else {
                String result = message;
                Bukkit.getOnlinePlayers().forEach(p -> typeMessage(p, result));
            }
        }
    }

}
