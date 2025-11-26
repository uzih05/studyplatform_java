package org.study.platform.client;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class SocketClient {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9090;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Long userId;
    private String nickname;
    private volatile boolean running = false;

    private List<MessageListener> listeners = new ArrayList<>();

    // 메시지 수신 리스너 인터페이스
    public interface MessageListener {
        void onMessageReceived(String message);
    }

    // 서버 연결
    public boolean connect(Long userId, String nickname) {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            this.userId = userId;
            this.nickname = nickname;

            // 인증 메시지 전송
            out.println("AUTH:" + userId + ":" + nickname);

            // 서버 응답 대기
            String response = in.readLine();
            if (response != null && response.equals("CONNECTED:success")) {
                running = true;

                // 메시지 수신 스레드 시작
                new Thread(this::receiveMessages).start();

                System.out.println("서버 연결 성공: " + nickname);
                return true;
            }

        } catch (IOException e) {
            System.err.println("서버 연결 실패: " + e.getMessage());
        }
        return false;
    }

    // 메시지 수신
    private void receiveMessages() {
        try {
            String message;
            while (running && (message = in.readLine()) != null) {
                // 리스너들에게 메시지 전달
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

    // 메시지 전송
    public void sendMessage(String message) {
        if (out != null && running) {
            out.println(message);
        }
    }

    // 채팅 메시지 전송
    public void sendChatMessage(String message) {
        sendMessage("CHAT:" + message);
    }

    // 게시글 읽음 알림
    public void sendPostRead(Long postId) {
        sendMessage("POST_READ:" + postId);
    }

    // 메시지 리스너 추가
    public void addMessageListener(MessageListener listener) {
        listeners.add(listener);
    }

    // 메시지 리스너 제거
    public void removeMessageListener(MessageListener listener) {
        listeners.remove(listener);
    }

    // 연결 종료
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

    // Getter
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