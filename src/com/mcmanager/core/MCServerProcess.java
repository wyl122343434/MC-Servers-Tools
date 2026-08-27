package com.mcmanager.core;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
public class MCServerProcess {
    private ServerConfig config;
    private Process process;
    private Thread outputThread;
    private Thread errorThread;
    private List<String> logBuffer = new ArrayList<>();
    private Consumer<String> logListener;
    private boolean running = false;

    public MCServerProcess(ServerConfig config) {
        this.config = config;
    }

    public void start() throws IOException {
        if (running) return;
        int startupMode = Integer.parseInt(com.mcmanager.util.ConfigStorage.loadSettings().getProperty("startup.mode", "0"));
        List<String> cmd = new ArrayList<>();
        File serverDir = new File(config.getServerDir());

        String jarName = config.getJarFile();
        boolean jarExists = jarName != null && !jarName.isEmpty()
            && jarName.toLowerCase().endsWith(".jar")
            && new File(serverDir, jarName).exists();
        // If user mistakenly put .bat as jar, auto-detect bat mode
        if (jarName != null && (jarName.toLowerCase().endsWith(".bat") || jarName.toLowerCase().endsWith(".cmd"))) {
            jarExists = false;
        }
        File runBat = new File(serverDir, "run.bat");
        File startBat = new File(serverDir, "start.bat");
        boolean batExists = runBat.exists() || startBat.exists();

        boolean useBat = (startupMode == 1 || startupMode == 2) || (!jarExists && batExists);

        if (useBat) {
            if (runBat.exists()) {
                cmd.add("cmd.exe");
                cmd.add("/c");
                cmd.add(runBat.getAbsolutePath());
            } else if (startBat.exists()) {
                cmd.add("cmd.exe");
                cmd.add("/c");
                cmd.add(startBat.getAbsolutePath());
            } else {
                throw new IOException("未找到 run.bat 或 start.bat，且未配置有效的 server.jar 文件。\n请在服务器设置中指定核心 jar 文件，或在服务器目录中放置 run.bat。");
            }
        } else {
            if (!jarExists) {
                throw new IOException("服务器核心 jar 文件不存在: " + (config.getJarFile() == null ? "(未设置)" : config.getJarFile())
                    + "\n请在「服务器设置」中指定核心 jar，或在服务器目录放置 run.bat 后重启程序。");
            }
            buildJavaCommand(cmd);
        }

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(serverDir);
        pb.redirectErrorStream(false);
        process = pb.start();
        running = true;
        outputThread = new Thread(() -> readStream(process.getInputStream(), false));
        errorThread = new Thread(() -> readStream(process.getErrorStream(), true));
        outputThread.setDaemon(true);
        errorThread.setDaemon(true);
        outputThread.start();
        errorThread.start();
    }

    private void buildJavaCommand(List<String> cmd) {
        cmd.add(config.getJavaPath());
        cmd.add("-Xmx" + config.getMaxMemory() + "M");
        cmd.add("-Xms" + config.getMinMemory() + "M");
        if (config.getExtraArgs() != null && !config.getExtraArgs().isEmpty()) {
            for (String arg : config.getExtraArgs().split(" ")) {
                if (!arg.trim().isEmpty()) cmd.add(arg.trim());
            }
        }
        cmd.add("-jar");
        cmd.add(config.getJarFile());
        cmd.add("nogui");
    }

    private void readStream(InputStream is, boolean isError) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                synchronized (logBuffer) {
                    logBuffer.add(line);
                    if (logBuffer.size() > 5000) {
                        logBuffer.remove(0);
                    }
                }
                if (logListener != null) {
                    logListener.accept(line);
                }
            }
        } catch (IOException e) {
            // stream closed
        }
    }

    public void sendCommand(String command) throws IOException {
        if (process != null && process.isAlive()) {
            OutputStream os = process.getOutputStream();
            os.write((command + "\n").getBytes("UTF-8"));
            os.flush();
        }
    }

    public void stop() {
        if (process != null && process.isAlive()) {
            try {
                sendCommand("stop");
                process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception e) {
                process.destroyForcibly();
            }
        }
        running = false;
    }

    public void kill() {
        if (process != null) {
            process.destroyForcibly();
        }
        running = false;
    }

    public boolean isRunning() {
        return process != null && process.isAlive();
    }

    public List<String> getLogs() {
        synchronized (logBuffer) {
            return new ArrayList<>(logBuffer);
        }
    }

    public void setLogListener(Consumer<String> listener) {
        this.logListener = listener;
    }

    public int getExitCode() {
        if (process != null && !process.isAlive()) {
            return process.exitValue();
        }
        return -1;
    }
}
