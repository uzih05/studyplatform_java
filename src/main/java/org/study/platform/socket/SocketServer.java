package org.study.platform.socket;

import org.springframework.context.ApplicationContext;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketServer {

    private ServerSocket serverSocket;
    private static final int PORT = 9090;
    private ConnectionManager connectionManager;
    private ApplicationContext context;
    private volatile boolean running = false;
    private final ExecutorService clientExecutor = Executors.newCachedThreadPool();

    public SocketServer(ApplicationContext context) {
        this.connectionManager = new ConnectionManager();
        this.context = context;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;
            System.out.println("소켓 서버가 포트 " + PORT + "에서 시작되었습니다.");

            Thread serverThread = new Thread(this::acceptClients);
            serverThread.setDaemon(true);
            serverThread.start();

        } catch (IOException e) {
            System.err.println("서버 시작 실패: " + e.getMessage());
        }
    }

    private void acceptClients() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("새로운 클라이언트 연결: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket, connectionManager, context);
                clientExecutor.submit(handler);

            } catch (IOException e) {
                if (running) {
                    System.err.println("클라이언트 연결 수락 실패: " + e.getMessage());
                }
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            connectionManager.disconnectAll();
            clientExecutor.shutdownNow();
            System.out.println("서버가 종료되었습니다.");
        } catch (IOException e) {
            System.err.println("서버 종료 중 오류: " + e.getMessage());
        }
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }
}
