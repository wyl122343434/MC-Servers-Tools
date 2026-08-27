package com.mcmanager.ssh;

import com.jcraft.jsch.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class SSHClient {
    private Session session;
    private String host;
    private int port;
    private String username;
    private String password;
    private String keyFile;
    
    public SSHClient(String host, int port, String username, String password, String keyFile) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.keyFile = keyFile;
    }
    
    public void connect() throws JSchException {
        JSch jsch = new JSch();
        if (keyFile != null && !keyFile.isEmpty()) {
            jsch.addIdentity(keyFile);
        }
        session = jsch.getSession(username, host, port);
        if (password != null && !password.isEmpty()) {
            session.setPassword(password);
        }
        session.setConfig("StrictHostKeyChecking", "no");
        session.setConfig("PreferredAuthentications", "publickey,password");
        session.setConfig("ConnectTimeout", "15000");
        session.connect(15000);
    }
    
    public void disconnect() {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }
    
    public boolean isConnected() {
        return session != null && session.isConnected();
    }
    
    public String execCommand(String command) throws JSchException, IOException {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        InputStream in = channel.getInputStream();
        channel.connect();
        
        StringBuilder output = new StringBuilder();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = in.read(buffer)) > 0) {
            output.append(new String(buffer, 0, read));
        }
        channel.disconnect();
        return output.toString();
    }
    
    public List<String> listFiles(String path) throws JSchException, IOException, SftpException {
        ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
        sftp.connect();
        List<String> files = new ArrayList<>();
        try {
            Vector<?> entries = sftp.ls(path);
            for (Object entry : entries) {
                ChannelSftp.LsEntry e = (ChannelSftp.LsEntry) entry;
                if (!e.getFilename().equals(".") && !e.getFilename().equals("..")) {
                    String name = e.getFilename();
                    boolean isDir = e.getAttrs().isDir();
                    long size = e.getAttrs().getSize();
                    String sizeStr = isDir ? "<DIR>" : formatSize(size);
                    String perms = e.getAttrs().getPermissionsString();
                    long mtime = e.getAttrs().getMTime() * 1000L;
                    String timeStr = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(mtime));
                    // Format: name|size|perms|time|isDir
                    files.add(name + "|" + sizeStr + "|" + perms + "|" + timeStr + "|" + (isDir ? "dir" : "file"));
                }
            }
        } finally {
            sftp.disconnect();
        }
        return files;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    public String readFile(String path) throws JSchException, IOException, SftpException {
        ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
        sftp.connect();
        InputStream is = sftp.get(path);
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        sftp.disconnect();
        return content.toString();
    }
    
    public void writeFile(String path, String content) throws JSchException, IOException, SftpException {
        ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
        sftp.connect();
        OutputStream os = sftp.put(path);
        os.write(content.getBytes("UTF-8"));
        os.flush();
        os.close();
        sftp.disconnect();
    }
    
    public void deleteFile(String path) throws JSchException, IOException, SftpException {
        ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
        sftp.connect();
        try {
            sftp.rm(path);
        } catch (SftpException e) {
            // might be a directory
            sftp.rmdir(path);
        }
        sftp.disconnect();
    }
    
    public void downloadFile(String remotePath, String localPath) throws JSchException, IOException, SftpException {
        ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
        sftp.connect();
        sftp.get(remotePath, localPath);
        sftp.disconnect();
    }
    
    public void uploadFile(String localPath, String remotePath) throws JSchException, IOException, SftpException {
        ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
        sftp.connect();
        sftp.put(localPath, remotePath);
        sftp.disconnect();
    }
    
    public Session getSession() {
        return session;
    }
}
