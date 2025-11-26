package org.study.platform.client;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class SocketClient {

    private static final int SERVER_PORT = 9090;

    private String serverHost;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Long userId;
    private String nickname;
    private volatile boolean running = false;

    private List<MessageListener> listeners = new ArrayList<>();

    public interface MessageListener {
        void onMessageReceived(String message);
    }

    public SocketClient(String serverHost) {
        this.serverHost = serverHost;
    }

    public boolean connect(Long userId, String nickname) {
        try {
            socket = new Socket(serverHost, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            this.userId = userId;
            this.nickname = nickname;

            out.println("AUTH:" + userId + ":" + nickname);

            String response = in.readLine();
            if (response != null && response.equals("CONNECTED:success")) {
                running = true;

                new Thread(this::receiveMessages).start();

                System.out.println("서버 연결 성공: " + nickname + " (" + serverHost + ")");
                return true;
            }

        } catch (IOException e) {
            System.err.println("서버 연결 실패 (" + serverHost + "): " + e.getMessage());
        }
        return false;
    }

    private void receiveMessages() {
        try {
            String message;
            while (running && (message = in.readLine()) != null) {
                for (MessageListener listener : listeners) {
                    listener.onMessageReceived(message);
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("메시지 수신 중 오류: " + e.getMessage());
            }
        }
    }

    public void sendMessage(String message) {
        if (out != null && running) {
            out.println(message);
        }
    }

    public void sendChatMessage(String message) {
        sendMessage("CHAT:" + message);
    }

    public void sendPostRead(Long postId) {
        sendMessage("POST_READ:" + postId);
    }

    public void addMessageListener(MessageListener listener) {
        listeners.add(listener);
    }

    public void removeMessageListener(MessageListener listener) {
        listeners.remove(listener);
    }

    public void disconnect() {
        running = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            System.out.println("서버 연결 종료");
        } catch (IOException e) {
            System.err.println("연결 종료 중 오류: " + e.getMessage());
        }
    }

    public Long getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }

    public boolean isConnected() {
        return running && socket != null && !socket.isClosed();
    }
}