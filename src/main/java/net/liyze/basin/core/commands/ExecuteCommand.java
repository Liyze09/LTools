package net.liyze.basin.core.commands;

import net.liyze.basin.core.Command;
import net.liyze.basin.core.Conversation;
import net.liyze.basin.core.Main;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Put command into a CachedThreadPool
 * /execute [command] [args..]
 *
 * @author Liyze09
 */
public class ExecuteCommand implements Command {


    @Override
    public void run(@NotNull List<String> args) {
        Main.servicePool.submit(new Thread(() -> new Conversation().sync().parse(args)));
    }

    @Override
    public @NotNull String Name() {
        return "execute";
    }
}


