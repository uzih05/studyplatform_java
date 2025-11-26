package org.study.platform.socket;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket clientSocket;
    private ConnectionManager connectionManager;
    private BufferedReader in;
    private PrintWriter out;
    private Long userId;
    private String nickname;
    private volatile boolean running = true;

    public ClientHandler(Socket socket, ConnectionManager connectionManager) {
        this.clientSocket = socket;
        this.connectionManager = connectionManager;
    }

    @Override
    public void run() {
        try {
            // 입출력 스트림 초기화
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            // 클라이언트 인증 (userId, nickname 받기)
            String authMessage = in.readLine();
            if (authMessage != null && authMessage.startsWith("AUTH:")) {
                String[] authData = authMessage.substring(5).split(":");
                this.userId = Long.parseLong(authData[0]);
                this.nickname = authData[1];

                // 연결 관리자에 등록
                connectionManager.addClient(userId, nickname, this);

                // 연결 성공 메시지
                sendMessage("CONNECTED:success");

                // 현재 접속자 목록 브로드캐스트
                connectionManager.broadcastUserList();
            }

            // 클라이언트 메시지 수신
            String message;
            while (running && (message = in.readLine()) != null) {
                handleMessage(message);
            }

        } catch (IOException e) {
            System.err.println("클라이언트 처리 중 오류: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    // 메시지 처리
    private void handleMessage(String message) {
        if (message.startsWith("CHAT:")) {
            // 채팅 메시지 브로드캐스트
            String chatMessage = message.substring(5);
            connectionManager.broadcast(nickname + ": " + chatMessage);
        } else if (message.startsWith("POST_READ:")) {
            // 게시글 읽음 알림
            String postId = message.substring(10);
            connectionManager.broadcast("POST_READ:" + userId + ":" + postId);
        }
    }

    // 클라이언트에게 메시지 전송
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    // 연결 종료
    public void disconnect() {
        running = false;
        if (userId != null) {
            connectionManager.removeClient(userId);
            connectionManager.broadcastUserList();
        }

        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            System.err.println("연결 종료 중 오류: " + e.getMessage());
        }

        System.out.println("클라이언트 연결 종료: " + nickname);
    }

    public Long getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }
}