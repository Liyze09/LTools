package net.liyze.basin.core.commands;

import net.liyze.basin.core.Main;
import net.liyze.basin.interfaces.Command;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

import static net.liyze.basin.core.Main.LOGGER;

/**
 * Print all command loaded.
 *
 * @author Liyze09
 */
public class ListCommand implements Command {
    @Override
    public void run(@NotNull List<String> args) {
        LOGGER.info("Commands");
        for (String i : Main.commands.keySet()) {
            System.out.println(i);
        }
        LOGGER.info("Variables");
        for (Map.Entry<String, Object> i : Main.envMap.entrySet()) {
            System.out.print(i.getKey() + " = ");
            System.out.println(i.getValue());
        }
    }

    @Override
    public @NotNull String Name() {
        return "list";
    }
}
