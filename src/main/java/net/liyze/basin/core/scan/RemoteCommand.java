package net.liyze.basin.core.scan;

import com.itranswarp.summer.annotation.Component;
import net.liyze.basin.core.Command;
import net.liyze.basin.core.remote.Client;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.liyze.basin.core.Basin.*;

@Component
public class RemoteCommand implements Command {
    @Override
    public void run(@NotNull List<String> args) {
        String host = args.remove(0);
        try {
            Client.send(String.join(" ", args), host, envMap.get("\"" + host + "_token\"").toString());
        } catch (Exception e) {
            LOGGER.error(e.toString());
        }
    }

    @Override
    public @NotNull String Name() {
        return "remote";
    }
}
