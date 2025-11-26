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
    private String lastResponse = null;
    private final Object responseLock = new Object();

    private List<MessageListener> listeners = new ArrayList<>();

    public interface MessageListener {
        void onMessageReceived(String message);
    }

    public SocketClient(String serverHost) {
        this.serverHost = serverHost;
    }

    private String encode(String text) {
        if (text == null) return "";
        return text.replace("|", "&#124;")   // 파이프 변환
                .replace(":", "&#58;")    // 콜론 변환
                .replace("\n", "&#10;");  // 줄바꿈 변환
    }

    private String decode(String text) {
        if (text == null) return "";
        return text.replace("&#124;", "|")
                .replace("&#58;", ":")
                .replace("&#10;", "\n");
    }

    public boolean connect(Long userId, String nickname) {
        try {
            socket = new Socket(serverHost, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            this.userId = userId;
            this.nickname = nickname;

            // AUTH 메시지 전송
            out.println("AUTH:" + userId + ":" + nickname);
            out.flush();  // 추가

            // 서버 응답 대기 (타임아웃 추가)
            socket.setSoTimeout(3000);  // 3초 타임아웃
            String response = in.readLine();
            socket.setSoTimeout(0);  // 타임아웃 해제

            if (response != null && response.equals("CONNECTED:success")) {
                running = true;

                // 메시지 수신 스레드 시작
                Thread receiverThread = new Thread(this::receiveMessages);
                receiverThread.setDaemon(true);
                receiverThread.start();

                System.out.println("서버 연결 성공: " + nickname + " (" + serverHost + ")");
                return true;
            } else {
                System.err.println("서버 응답 실패: " + response);
            }

        } catch (IOException e) {
            System.err.println("서버 연결 실패 (" + serverHost + "): " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // receiveMessages 메서드
    private void receiveMessages() {
        try {
            String message;
            while (running && (message = in.readLine()) != null) {
                final String finalMessage = message;

                // 응답 메시지인 경우 (|로 구분된 메시지)
                if (message.contains("|") && message.contains("_RESPONSE")) {
                    synchronized (responseLock) {
                        lastResponse = message;
                        responseLock.notify();
                    }
                }

                // 리스너들에게 메시지 전달
                for (MessageListener listener : listeners) {
                    listener.onMessageReceived(finalMessage);
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
        sendMessage("CHAT:" + encode(message));
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

    // 동기 요청/응답 메서드 (기존 receiveMessages 메서드 아래에 추가)
    public String sendRequestAndWaitResponse(String request, String expectedResponsePrefix) {
        synchronized (responseLock) {
            lastResponse = null;
            sendMessage(request);

            try {
                // 응답 대기 (최대 5초)
                responseLock.wait(5000);

                if (lastResponse != null && lastResponse.startsWith(expectedResponsePrefix)) {
                    return lastResponse;
                }

                return null;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
    }

    // 편의 메서드들 추가
    public String login(String username, String password) {
        return sendRequestAndWaitResponse("LOGIN|" + username + "|" + password, "LOGIN_RESPONSE");
    }

    public String register(String username, String password, String nickname) {
        return sendRequestAndWaitResponse("REGISTER|" + username + "|" + password + "|" + nickname, "REGISTER_RESPONSE");
    }

    public String getRooms() {
        return sendRequestAndWaitResponse("GET_ROOMS", "GET_ROOMS_RESPONSE");
    }

    public String createRoom(String roomName) {
        return sendRequestAndWaitResponse("CREATE_ROOM|" + roomName, "CREATE_ROOM_RESPONSE");
    }

    public String deleteRoom(Long roomId) {
        return sendRequestAndWaitResponse("DELETE_ROOM|" + roomId, "DELETE_ROOM_RESPONSE");
    }

    public String getPosts(Long roomId) {
        return sendRequestAndWaitResponse("GET_POSTS|" + roomId, "GET_POSTS_RESPONSE");
    }

    public String createPost(Long roomId, String title, String content, String postType) {
        return sendRequestAndWaitResponse("CREATE_POST|" + roomId + "|" + encode(title) + "|" + encode(content) + "|" + postType, "CREATE_POST_RESPONSE");
    }

    public String deletePost(Long postId) {
        return sendRequestAndWaitResponse("DELETE_POST|" + postId, "DELETE_POST_RESPONSE");
    }

    public String getComments(Long postId) {
        return sendRequestAndWaitResponse("GET_COMMENTS|" + postId, "GET_COMMENTS_RESPONSE");
    }

    public String createComment(Long postId, String content) {
        return sendRequestAndWaitResponse("CREATE_COMMENT|" + postId + "|" + encode(content), "CREATE_COMMENT_RESPONSE");
    }

    public String getUser(Long userId) {
        return sendRequestAndWaitResponse("GET_USER|" + userId, "GET_USER_RESPONSE");
    }

    public String markRead(Long postId) {
        return sendRequestAndWaitResponse("MARK_READ|" + postId, "MARK_READ_RESPONSE");
    }

    public String getReadStatus(Long postId) {
        return sendRequestAndWaitResponse("GET_READ_STATUS|" + postId, "GET_READ_STATUS_RESPONSE");
    }
}