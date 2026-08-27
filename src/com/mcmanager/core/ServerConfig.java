package com.mcmanager.core;

import java.io.Serializable;

public class ServerConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String name;
    private String type; // local, ssh
    private String host;
    private int port;
    private String username;
    private String password;
    private String keyFile;
    private String serverDir;
    private String javaPath;
    private String jarFile;
    private int serverPort;
    private int maxMemory;
    private int minMemory;
    private String extraArgs;
    private String coreType; // vanilla, paper, forge, fabric, neoforge
    private String mcVersion;
    private String iconPath; // server icon image
    private boolean autoStart;
    
    public ServerConfig() {
        this.id = java.util.UUID.randomUUID().toString();
        this.type = "local";
        this.host = "localhost";
        this.port = 22;
        this.serverPort = 25565;
        this.maxMemory = 4096;
        this.minMemory = 1024;
        this.coreType = "vanilla";
        this.javaPath = "java";
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getKeyFile() { return keyFile; }
    public void setKeyFile(String keyFile) { this.keyFile = keyFile; }
    public String getServerDir() { return serverDir; }
    public void setServerDir(String serverDir) { this.serverDir = serverDir; }
    public String getJavaPath() { return javaPath; }
    public void setJavaPath(String javaPath) { this.javaPath = javaPath; }
    public String getJarFile() { return jarFile; }
    public void setJarFile(String jarFile) { this.jarFile = jarFile; }
    public int getServerPort() { return serverPort; }
    public void setServerPort(int serverPort) { this.serverPort = serverPort; }
    public int getMaxMemory() { return maxMemory; }
    public void setMaxMemory(int maxMemory) { this.maxMemory = maxMemory; }
    public int getMinMemory() { return minMemory; }
    public void setMinMemory(int minMemory) { this.minMemory = minMemory; }
    public String getExtraArgs() { return extraArgs; }
    public void setExtraArgs(String extraArgs) { this.extraArgs = extraArgs; }
    public String getCoreType() { return coreType; }
    public void setCoreType(String coreType) { this.coreType = coreType; }
    public String getMcVersion() { return mcVersion; }
    public void setMcVersion(String mcVersion) { this.mcVersion = mcVersion; }
    public String getIconPath() { return iconPath; }
    public void setIconPath(String iconPath) { this.iconPath = iconPath; }
    public boolean isAutoStart() { return autoStart; }
    public void setAutoStart(boolean autoStart) { this.autoStart = autoStart; }
    
    @Override
    public String toString() {
        return name + " (" + type + ")";
    }
}
