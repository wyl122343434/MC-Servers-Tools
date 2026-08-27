package com.mcmanager.core;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicInteger;

public class RconClient {
    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private AtomicInteger requestId = new AtomicInteger(1);
    private boolean authenticated = false;
    private String host;
    private int port;
    private String password;

    public RconClient(String host, int port, String password) {
        this.host = host;
        this.port = port;
        this.password = password;
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        socket.setSoTimeout(5000);
        in = socket.getInputStream();
        out = socket.getOutputStream();
        authenticated = login();
        if (!authenticated) {
            throw new IOException("RCON 认证失败：密码错误");
        }
    }

    private boolean login() throws IOException {
        int id = requestId.getAndIncrement();
        sendPacket(id, 3, password);
        Packet resp = readPacket();
        return resp != null && resp.requestId == id;
    }

    public String sendCommand(String command) throws IOException {
        if (!authenticated) throw new IOException("未连接或未认证");
        int id = requestId.getAndIncrement();
        sendPacket(id, 2, command);
        StringBuilder result = new StringBuilder();
        Packet resp;
        do {
            resp = readPacket();
            if (resp != null) {
                result.append(resp.payload);
            }
        } while (resp != null && resp.requestId == id && resp.type == 0 && in.available() > 0);
        return result.toString().trim();
    }

    private void sendPacket(int requestId, int type, String payload) throws IOException {
        byte[] payloadBytes = payload.getBytes("UTF-8");
        int packetSize = 4 + 4 + payloadBytes.length + 2; // id + type + payload + null terminator + padding
        ByteBuffer buf = ByteBuffer.allocate(packetSize + 4); // +4 for length field
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(packetSize);
        buf.putInt(requestId);
        buf.putInt(type);
        buf.put(payloadBytes);
        buf.put((byte) 0); // null terminator
        buf.put((byte) 0); // padding
        out.write(buf.array());
        out.flush();
    }

    private Packet readPacket() throws IOException {
        byte[] lenBytes = readExactly(4);
        if (lenBytes == null) return null;
        int length = ByteBuffer.wrap(lenBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
        if (length < 8 || length > 4096) return null;
        byte[] data = readExactly(length);
        if (data == null) return null;
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int requestId = buf.getInt();
        int type = buf.getInt();
        byte[] payloadBytes = new byte[length - 8 - 2]; // minus id, type, null+padding
        buf.get(payloadBytes);
        return new Packet(requestId, type, new String(payloadBytes, "UTF-8"));
    }

    private byte[] readExactly(int n) throws IOException {
        byte[] data = new byte[n];
        int offset = 0;
        while (offset < n) {
            int read = in.read(data, offset, n - offset);
            if (read == -1) return null;
            offset += read;
        }
        return data;
    }

    public void disconnect() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {}
        authenticated = false;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed() && authenticated;
    }

    private static class Packet {
        int requestId;
        int type;
        String payload;
        Packet(int id, int type, String payload) {
            this.requestId = id;
            this.type = type;
            this.payload = payload;
        }
    }
}
