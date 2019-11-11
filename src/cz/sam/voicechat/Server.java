package cz.sam.voicechat;

import com.sun.awt.SecurityWarning;

import javax.sound.sampled.Line;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Server implements Runnable {

    public static class ServerClient_Read implements Runnable {

        private ServerClient serverClient;
        private DataInputStream dataInputStream;

        public ServerClient_Read(ServerClient serverClient) throws IOException {
            this.serverClient = serverClient;
            this.dataInputStream = new DataInputStream(serverClient.getSocket().getInputStream());
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1600];

            while(true) {
                try {
                    if(this.dataInputStream.read(buffer, 0, buffer.length) > 0) {
                        this.serverClient.getServer().send(this.serverClient, buffer);
                    }
                }catch (Exception ex) {
                    this.stop();
                    break;
                }
            }
        }

        public void stop() {
            try {
                this.dataInputStream.close();
            } catch (Exception ex) { }
        }

        public void start() {
            new Thread(this).start();
        }
    }

    public static class ServerClient_Write implements Runnable {

        private ServerClient serverClient;
        private DataOutputStream dataOutputStream;
        private Queue<byte[]> queue = new LinkedList<>();

        public ServerClient_Write(ServerClient serverClient) throws IOException {
            this.serverClient = serverClient;
            this.dataOutputStream = new DataOutputStream(serverClient.getSocket().getOutputStream());
        }

        @Override
        public void run() {
            while(true) {
                try {
                    synchronized (this.queue) {
                        for(int i = 0; i < this.queue.size(); i++) {
                            synchronized (this.queue) {
                                byte[] data = this.queue.remove();

                                this.dataOutputStream.write(data, 0, data.length);
                            }
                        }
                    }
                }catch (Exception ex) {
                    this.stop();
                    break;
                }
            }
        }

        private synchronized void send(byte[] data) {
            synchronized (this.queue) {
                this.queue.add(data);
            }
        }

        public void stop() {
            try {
                this.dataOutputStream.close();
            } catch (Exception ex) { }
        }

        public void start() {
            new Thread(this).start();
        }
    }

    public static class ServerClient {

        private Server server;
        private Socket socket;

        private ServerClient_Read serverClient_read;
        private ServerClient_Write serverClient_write;

        public ServerClient(Server server, Socket socket) throws IOException {
            this.server = server;
            this.socket = socket;

            System.out.println("Client connected: " + socket.getInetAddress().getHostAddress());
        }

        public void start() {
            try {
                this.serverClient_write = new ServerClient_Write(this);
                this.serverClient_read = new ServerClient_Read(this);

                this.serverClient_write.start();
                this.serverClient_read.start();

            } catch (Exception ex) {
                this.stop();
            }
        }

        public synchronized void send(byte[] data) {
            this.serverClient_write.send(data);
        }

        public void stop() {
            try {
                this.socket.close();
            } catch (Exception ex) { }

            this.server.removeClient(this);
        }

        public Server getServer() {
            return server;
        }

        public Socket getSocket() {
            return socket;
        }
    }

    private List<ServerClient> serverClients = new ArrayList<>();

    public static void main(String[] args) {
        new Thread(new Server()).start();
    }

    @Override
    public void run() {
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(25566);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        while(true) {
            try {
                Socket socket = serverSocket.accept();
                ServerClient serverClient = new ServerClient(this, socket);
                serverClient.start();

                addClient(serverClient);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public synchronized List<ServerClient> getClients() {
        return this.serverClients;
    }

    public synchronized void addClient(ServerClient serverClient) {
        this.serverClients.add(serverClient);
    }

    public synchronized void send(ServerClient client, byte[] buffer) {
        for(ServerClient serverClient : this.serverClients) {
            if(serverClient != client) {
                serverClient.send(buffer);
            }
        }
    }

    public synchronized void removeClient(ServerClient serverClient) {
        this.serverClients.remove(serverClient);

        System.out.println("Client disconnected: " + serverClient.getSocket().getInetAddress().getHostAddress());
    }
}
