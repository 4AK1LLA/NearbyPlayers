package com.rustret.nearbyplayers;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.permission.PermissionAttachmentInfo;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;

import java.util.*;
import java.util.Map.*;
import java.util.regex.Pattern;

public class NearbyPlayers extends PluginBase {
    private final Pattern regex = Pattern.compile("near\\.radius\\.\\d+");
    private String TEXT_NEAR_NO, TEXT_NEAR_FOUND, TEXT_NEAR_PLAYER;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Config cfg = getConfig();
        TEXT_NEAR_NO = cfg.getString("near-no");
        TEXT_NEAR_FOUND = cfg.getString("near-found");
        TEXT_NEAR_PLAYER = cfg.getString("near-player");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        int radius = getRadius(player);
        near(radius, player);

        return true;
    }

    private int getRadius(Player player) {
        Collection<PermissionAttachmentInfo> permissions = player.getEffectivePermissions().values();

        Optional<Integer> radius = permissions
                .stream()
                .filter(info -> info.getValue() && regex.matcher(info.getPermission()).matches())
                .map(info -> Integer.parseInt(info.getPermission().replaceAll("\\D", "")))
                .max(Integer::compareTo);

        // TODO: Add default radius feature
        if (!radius.isPresent()) {
            throw new NoSuchElementException("Player used /near command, but permission with radius not found");
        }

        return radius.get();
    }

    private void near(int radius, Player sender) {
        Map<String, Integer> nearPlayers = new HashMap<>();

        Collection<Player> allPlayers = Server.getInstance().getOnlinePlayers().values();
        for (Player player : allPlayers) {
            if (player.getLevel().getId() != sender.getLevel().getId() || player.getId() == sender.getId()) {
                continue;
            }
            int distance = (int) sender.distance(player);
            if (distance > radius) {
                continue;
            }
            nearPlayers.put(player.getName(), distance);
        }

        sender.sendMessage(buildMessage(nearPlayers, radius));
    }

    private String buildMessage(Map<String, Integer> players, int radius) {
        if (players.size() == 0) {
            return String.format(TEXT_NEAR_NO, radius);
        }

        List<Entry<String, Integer>> entryList = new ArrayList<>(players.entrySet());
        entryList.sort(Entry.comparingByValue());

        String start = String.format(TEXT_NEAR_FOUND, radius);
        StringBuilder message = new StringBuilder(start);
        for (Entry<String, Integer> entry : entryList) {
            if (message.length() > start.length()) {
                message.append(", ");
            }
            message.append(String.format(TEXT_NEAR_PLAYER, entry.getKey(), entry.getValue()));
        }

        return message.toString();
    }
}