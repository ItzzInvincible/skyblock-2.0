package com.skyblock.skyblock.utilities.command;

import com.skyblock.skyblock.Skyblock;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public interface TrueAlias<T extends Command> {

    default void register() {
        T command;

        try {
            //noinspection unchecked
            command = (T) this.getClass().newInstance();
        } catch (Exception ex) {
            Skyblock.getPlugin().sendMessage("&cCould not register command alias: " + this.getClass().getName() + " because it does not implement com.skyblock.skyblock.utilities.command.Command.");

            return;
        }

        Skyblock.getPlugin().getCommand(command.name().toLowerCase()).setExecutor(
                (sender, command1, label, args) -> {
                    if (sender instanceof Player) ((Player) sender).performCommand("sb " + command.name().toLowerCase() + " " + String.join(" ", args));
                    else Bukkit.dispatchCommand(sender, "sb " + command.name().toLowerCase() + " " + String.join(" ", args));

                    return false;
                }
        );
    }

}
