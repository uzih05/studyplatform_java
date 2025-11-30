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
        return text.replace("|", "&#124;")
                .replace(":", "&#58;")
                .replace("\n", "&#10;");
    }

    private String decode(String text) {
        if (text == null) return "";
        return text.replace("&#124;", "|")
                .replace("&#58;", ":")
                .replace("&#10;", "\n");
    }

    public boolean startConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                return true;
            }
            socket = new Socket(serverHost, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            running = true;

            Thread receiverThread = new Thread(this::receiveMessages);
            receiverThread.setDaemon(true);
            receiverThread.start();

            System.out.println("서버 연결 성공: " + serverHost);
            return true;
        } catch (IOException e) {
            System.err.println("서버 연결 실패: " + e.getMessage());
            return false;
        }
    }

    public boolean authenticate(Long userId, String nickname) {
        this.userId = userId;
        this.nickname = nickname;

        if (!isConnected()) {
            if (!startConnection()) {
                return false;
            }
        }

        sendMessage("AUTH:" + userId + ":" + nickname);
        return true;
    }

    private void receiveMessages() {
        try {
            String message;
            while (running && (message = in.readLine()) != null) {
                final String finalMessage = message;

                // 응답 메시지인 경우
                if (message.contains("|") && message.contains("_RESPONSE")) {
                    synchronized (responseLock) {
                        lastResponse = message;
                        responseLock.notify();
                    }
                }

                // 리스너들에게 메시지 전달 (실시간 업데이트용)
                for (MessageListener listener : new ArrayList<>(listeners)) {
                    try {
                        listener.onMessageReceived(finalMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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

    public String sendRequestAndWaitResponse(String request, String expectedResponsePrefix) {
        synchronized (responseLock) {
            lastResponse = null;
            sendMessage(request);

            try {
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

    // 기본 요청 메서드들
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

    public String joinRoom(Long roomId) {
        return sendRequestAndWaitResponse("JOIN_ROOM|" + roomId, "JOIN_ROOM_RESPONSE");
    }

    public String leaveRoom(Long roomId) {
        return sendRequestAndWaitResponse("LEAVE_ROOM|" + roomId, "LEAVE_ROOM_RESPONSE");
    }

    public String getPosts(Long roomId) {
        return sendRequestAndWaitResponse("GET_POSTS|" + roomId, "GET_POSTS_RESPONSE");
    }

    public String getNotices(Long roomId) {
        return sendRequestAndWaitResponse("GET_NOTICES|" + roomId, "GET_NOTICES_RESPONSE");
    }

    public String getGeneralPosts(Long roomId) {
        return sendRequestAndWaitResponse("GET_GENERAL_POSTS|" + roomId, "GET_GENERAL_POSTS_RESPONSE");
    }

    public String createPost(Long roomId, String title, String content, String postType) {
        return sendRequestAndWaitResponse("CREATE_POST|" + roomId + "|" + encode(title) + "|" + encode(content) + "|" + postType, "CREATE_POST_RESPONSE");
    }

    public String deletePost(Long postId) {
        return sendRequestAndWaitResponse("DELETE_POST|" + postId, "DELETE_POST_RESPONSE");
    }

    public String getPostDetail(Long postId) {
        return sendRequestAndWaitResponse("GET_POST_DETAIL|" + postId, "GET_POST_DETAIL_RESPONSE");
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

    // 과제 관련 메서드
    public String createAssignment(Long postId, String title, String description, String dueDate) {
        String dueDateParam = dueDate != null ? dueDate : "";
        return sendRequestAndWaitResponse("CREATE_ASSIGNMENT|" + postId + "|" + encode(title) + "|" + encode(description) + "|" + dueDateParam, "CREATE_ASSIGNMENT_RESPONSE");
    }

    public String getAssignment(Long postId) {
        return sendRequestAndWaitResponse("GET_ASSIGNMENT|" + postId, "GET_ASSIGNMENT_RESPONSE");
    }

    public String submitAssignment(Long assignmentId, String content, String fileName, String filePath, Long fileSize) {
        String fileNameParam = fileName != null ? encode(fileName) : "";
        String filePathParam = filePath != null ? filePath : "";
        String fileSizeParam = fileSize != null ? fileSize.toString() : "";
        return sendRequestAndWaitResponse("SUBMIT_ASSIGNMENT|" + assignmentId + "|" + encode(content) + "|" + fileNameParam + "|" + filePathParam + "|" + fileSizeParam, "SUBMIT_ASSIGNMENT_RESPONSE");
    }

    public String getSubmissions(Long assignmentId) {
        return sendRequestAndWaitResponse("GET_SUBMISSIONS|" + assignmentId, "GET_SUBMISSIONS_RESPONSE");
    }

    public String getMySubmission(Long assignmentId) {
        return sendRequestAndWaitResponse("GET_MY_SUBMISSION|" + assignmentId, "GET_MY_SUBMISSION_RESPONSE");
    }

    public String gradeSubmission(Long submissionId, Integer score, String feedback) {
        String scoreParam = score != null ? score.toString() : "";
        return sendRequestAndWaitResponse("GRADE_SUBMISSION|" + submissionId + "|" + scoreParam + "|" + encode(feedback), "GRADE_SUBMISSION_RESPONSE");
    }

    public String decodeText(String text) {
        return decode(text);
    }
}
