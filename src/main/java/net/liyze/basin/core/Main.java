package net.liyze.basin.core;

import com.teesoft.jackson.dataformat.toml.TOMLMapper;
import net.liyze.basin.api.BasinBoot;
import net.liyze.basin.api.Command;
import net.liyze.basin.core.commands.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.jar.JarFile;

import static net.liyze.basin.core.Basin.basin;
import static net.liyze.basin.web.Server.dynamicFunctions;

public final class Main {
    public static final Logger LOGGER = LoggerFactory.getLogger("Basin");
    public static final HashMap<String, Command> commands = new HashMap<>();
    public static TOMLMapper env = new TOMLMapper();
    public static final File userHome = new File("data" + File.separator + "home");
    public static ExecutorService servicePool = Executors.newCachedThreadPool();
    static final File jars = new File("data" + File.separator + "jars");
    public final static File config = new File("data" + File.separator + "cfg.json");
    public static ExecutorService taskPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
    public static HashMap<String, Object> envMap;
    public final static List<Class<?>> BootClasses = new ArrayList<>();
    private static String command;

    public static void main(String[] args) {
        LOGGER.info("Basin started.");
        Thread init = new Thread(() -> {
            try {
                init();
                LOGGER.info("Init method are finished.");
                loadJars();
                LOGGER.info("Loader's method are finished.");
                BootClasses.forEach((i) -> new Thread(() -> {
                    try {
                        BasinBoot in = (BasinBoot) i.getDeclaredConstructor().newInstance();
                        in.afterStart();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).start());
                LOGGER.info("Startup method are finished.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        taskPool.submit(init);
        new Thread(() -> {
            regCommands();
            Scanner scanner = new Scanner(System.in);
            while (true) {
                command = scanner.nextLine();
                taskPool.submit(new Task());
            }
        }).start();
        System.out.println("Basin " + Basin.getVersion());
        System.out.println(basin);
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "unchecked"})
    public static void init() throws IOException {
        userHome.mkdirs();
        jars.mkdirs();
        File envFile = new File("data" + File.separator + "env.toml");
        try {
            Config.initConfig();
        } catch (Exception e) {
            LOGGER.error("Error when load config file: ", e);
        }
        if (!envFile.exists()) {
            try {
                envFile.createNewFile();
            } catch (IOException e) {
                LOGGER.error("Error when create environment variable file: ", e);
            }
            try (Writer writer = new FileWriter(envFile)) {
                writer.append("# Basin Environment Variable");
            } catch (IOException e) {
                LOGGER.error("Error when create environment variable file: ", e);
            }
        }
        envMap = env.readValue(envFile, HashMap.class);
        taskPool = Executors.newFixedThreadPool(Config.cfg.taskPoolSize);
    }

    public static void loadJars() throws Exception {
        File[] children = jars.listFiles((file, s) -> s.matches(".*\\.jar"));
        String b, c;
        String[] bl, cl;
        if (children == null) {
            LOGGER.error("Jars file isn't exist!");
        } else {
            for (File jar : children) {
                try (JarFile jarFile = new JarFile(jar)) {
                    b = jarFile.getManifest().getMainAttributes().getValue("Boot-Class");
                    bl = StringUtils.split(b, ' ');
                    c = jarFile.getManifest().getMainAttributes().getValue("Export-Command");
                    cl = StringUtils.split(c, ' ');
                    for (String i : bl) {
                        if (!b.isBlank()) {
                            Class<?> cls = Class.forName(i);
                            Object boot = cls.getDeclaredConstructor().newInstance();
                            if (boot instanceof BasinBoot) {
                                new Thread(() -> ((BasinBoot) boot).onStart()).start();
                                BootClasses.add(cls);
                            } else {
                                LOGGER.warn("App {} is unsupported", jar.getName());
                            }
                        }
                    }
                    for (String i : cl) {
                        if (!c.isBlank()) {
                            Class<?> cls = Class.forName(i);
                            Object command = cls.getDeclaredConstructor().newInstance();
                            if (command instanceof Command) {
                                register((Command) command);
                            } else {
                                LOGGER.warn("Plugin {} is unsupported", jar.getName());
                            }
                        }
                    }
                }
            }
        }
    }

    public static void runCommand(@NotNull String cmd) {
        if (command.isBlank()) return;
        ArrayList<String> args = new ArrayList<>(List.of(StringUtils.split(cmd.toLowerCase().strip().replace("/", ""), ' ')));
        String cmdName = args.get(0);
        args.remove(cmdName);
        Command run = commands.get(cmdName.strip());
        LOGGER.info("Starting: " + cmd);
        if (!(run == null)) {
            try {
                run.run(args);
            } catch (IndexOutOfBoundsException e) {
                LOGGER.error("Bad arg input.");
            } catch (RuntimeException e) {
                LOGGER.error(String.valueOf(e));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else LOGGER.error("Unknown command: " + cmdName);
    }

    public static void regCommands() {
        register(new ForceStopCommand());
        register(new StopCommand());
        register(new EquationCommand());
        register(new ListCommand());
        register(new BenchCommand());
        register(new ExecuteCommand());
        register(new ScriptCommand());
        register(new ServerCommand());
        register(new RestartCommand());
    }

    public static void register(Command cmd) {
        commands.put(cmd.Name(), cmd);
    }

    @SuppressWarnings("unused")
    public static void register(String regex, Function<String, byte[]> function) {
        dynamicFunctions.put(regex, function);
    }

    static class Task implements Runnable {
        @Override
        public void run() {
            try {
                runCommand(command);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
