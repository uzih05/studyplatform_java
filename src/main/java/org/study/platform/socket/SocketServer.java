package org.study.platform.socket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer {

    private ServerSocket serverSocket;
    private static final int PORT = 9090;
    private ConnectionManager connectionManager;
    private volatile boolean running = false;

    public SocketServer() {
        this.connectionManager = new ConnectionManager();
    }

    // 서버 시작
    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;
            System.out.println("소켓 서버가 포트 " + PORT + "에서 시작되었습니다.");

            // 클라이언트 연결 대기 스레드
            new Thread(this::acceptClients).start();

        } catch (IOException e) {
            System.err.println("서버 시작 실패: " + e.getMessage());
        }
    }

    // 클라이언트 연결 수락
    private void acceptClients() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("새로운 클라이언트 연결: " + clientSocket.getInetAddress());

                // 클라이언트 핸들러 생성 및 시작
                ClientHandler handler = new ClientHandler(clientSocket, connectionManager);
                new Thread(handler).start();

            } catch (IOException e) {
                if (running) {
                    System.err.println("클라이언트 연결 수락 실패: " + e.getMessage());
                }
            }
        }
    }

    // 서버 종료
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            connectionManager.disconnectAll();
            System.out.println("서버가 종료되었습니다.");
        } catch (IOException e) {
            System.err.println("서버 종료 중 오류: " + e.getMessage());
        }
    }
}