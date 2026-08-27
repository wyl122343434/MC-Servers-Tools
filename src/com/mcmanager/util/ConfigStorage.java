package com.mcmanager.util;

import com.mcmanager.core.ServerConfig;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ConfigStorage {
    private static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + ".mcmanager";
    private static final String SERVERS_FILE = CONFIG_DIR + File.separator + "servers.properties";
    private static final String SETTINGS_FILE = CONFIG_DIR + File.separator + "settings.properties";
    
    public static void ensureConfigDir() {
        File dir = new File(CONFIG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    public static List<ServerConfig> loadServers() {
        ensureConfigDir();
        List<ServerConfig> servers = new ArrayList<>();
        File file = new File(SERVERS_FILE);
        if (!file.exists()) return servers;
        
        try {
            Properties props = new Properties();
            props.load(new FileInputStream(file));
            int count = Integer.parseInt(props.getProperty("server.count", "0"));
            for (int i = 0; i < count; i++) {
                String prefix = "server." + i + ".";
                ServerConfig cfg = new ServerConfig();
                cfg.setId(props.getProperty(prefix + "id", ""));
                cfg.setName(props.getProperty(prefix + "name", "Unnamed"));
                cfg.setType(props.getProperty(prefix + "type", "local"));
                cfg.setHost(props.getProperty(prefix + "host", "localhost"));
                cfg.setPort(Integer.parseInt(props.getProperty(prefix + "port", "22")));
                cfg.setUsername(props.getProperty(prefix + "username", ""));
                cfg.setPassword(props.getProperty(prefix + "password", ""));
                cfg.setKeyFile(props.getProperty(prefix + "keyFile", ""));
                cfg.setServerDir(props.getProperty(prefix + "serverDir", ""));
                cfg.setJavaPath(props.getProperty(prefix + "javaPath", "java"));
                cfg.setJarFile(props.getProperty(prefix + "jarFile", ""));
                cfg.setServerPort(Integer.parseInt(props.getProperty(prefix + "serverPort", "25565")));
                cfg.setMaxMemory(Integer.parseInt(props.getProperty(prefix + "maxMemory", "4096")));
                cfg.setMinMemory(Integer.parseInt(props.getProperty(prefix + "minMemory", "1024")));
                cfg.setExtraArgs(props.getProperty(prefix + "extraArgs", ""));
                cfg.setCoreType(props.getProperty(prefix + "coreType", "vanilla"));
                cfg.setMcVersion(props.getProperty(prefix + "mcVersion", ""));
                cfg.setAutoStart(Boolean.parseBoolean(props.getProperty(prefix + "autoStart", "false")));
                servers.add(cfg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return servers;
    }
    
    public static void saveServers(List<ServerConfig> servers) {
        ensureConfigDir();
        try {
            Properties props = new Properties();
            props.setProperty("server.count", String.valueOf(servers.size()));
            for (int i = 0; i < servers.size(); i++) {
                ServerConfig cfg = servers.get(i);
                String prefix = "server." + i + ".";
                props.setProperty(prefix + "id", cfg.getId());
                props.setProperty(prefix + "name", cfg.getName());
                props.setProperty(prefix + "type", cfg.getType());
                props.setProperty(prefix + "host", cfg.getHost());
                props.setProperty(prefix + "port", String.valueOf(cfg.getPort()));
                props.setProperty(prefix + "username", cfg.getUsername());
                props.setProperty(prefix + "password", cfg.getPassword());
                props.setProperty(prefix + "keyFile", cfg.getKeyFile());
                props.setProperty(prefix + "serverDir", cfg.getServerDir());
                props.setProperty(prefix + "javaPath", cfg.getJavaPath());
                props.setProperty(prefix + "jarFile", cfg.getJarFile());
                props.setProperty(prefix + "serverPort", String.valueOf(cfg.getServerPort()));
                props.setProperty(prefix + "maxMemory", String.valueOf(cfg.getMaxMemory()));
                props.setProperty(prefix + "minMemory", String.valueOf(cfg.getMinMemory()));
                props.setProperty(prefix + "extraArgs", cfg.getExtraArgs() != null ? cfg.getExtraArgs() : "");
                props.setProperty(prefix + "coreType", cfg.getCoreType());
                props.setProperty(prefix + "mcVersion", cfg.getMcVersion() != null ? cfg.getMcVersion() : "");
                props.setProperty(prefix + "autoStart", String.valueOf(cfg.isAutoStart()));
            }
            props.store(new FileOutputStream(SERVERS_FILE), "MC-Servers-Tools - Server Configurations");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static Properties loadSettings() {
        ensureConfigDir();
        Properties props = new Properties();
        File file = new File(SETTINGS_FILE);
        if (file.exists()) {
            try {
                props.load(new FileInputStream(file));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return props;
    }
    
    public static void saveSettings(Properties props) {
        ensureConfigDir();
        try {
            props.store(new FileOutputStream(SETTINGS_FILE), "MC-Servers-Tools - Settings");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static String getConfigDir() {
        return CONFIG_DIR;
    }
}
