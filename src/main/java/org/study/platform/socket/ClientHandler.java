package org.study.platform.socket;

import org.study.platform.entity.*;
import org.study.platform.service.*;
import org.springframework.context.ApplicationContext;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {

    private Socket clientSocket;
    private ConnectionManager connectionManager;
    private ApplicationContext context;
    private BufferedReader in;
    private PrintWriter out;
    private Long userId;
    private String nickname;
    private volatile boolean running = true;

    // Services
    private UserService userService;
    private RoomService roomService;
    private PostService postService;
    private CommentService commentService;
    private PostReadStatusService postReadStatusService;

    public ClientHandler(Socket socket, ConnectionManager connectionManager, ApplicationContext context) {
        this.clientSocket = socket;
        this.connectionManager = connectionManager;
        this.context = context;

        // Service 초기화
        this.userService = context.getBean(UserService.class);
        this.roomService = context.getBean(RoomService.class);
        this.postService = context.getBean(PostService.class);
        this.commentService = context.getBean(CommentService.class);
        this.postReadStatusService = context.getBean(PostReadStatusService.class);
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

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

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
        if (message.startsWith("AUTH:")) {
            String[] parts = message.split(":");
            handleAuth(parts);
            return;
        }

        String[] parts = message.split("\\|");
        String command = parts[0];

        try {
            switch (command) {
                case "AUTH":
                    handleAuth(parts);
                    break;
                case "LOGIN":
                    handleLogin(parts);
                    break;
                case "REGISTER":
                    handleRegister(parts);
                    break;
                case "GET_ROOMS":
                    handleGetRooms();
                    break;
                case "CREATE_ROOM":
                    handleCreateRoom(parts);
                    break;
                case "DELETE_ROOM":
                    handleDeleteRoom(parts);
                    break;
                case "GET_POSTS":
                    handleGetPosts(parts);
                    break;
                case "CREATE_POST":
                    handleCreatePost(parts);
                    break;
                case "DELETE_POST":
                    handleDeletePost(parts);
                    break;
                case "GET_COMMENTS":
                    handleGetComments(parts);
                    break;
                case "CREATE_COMMENT":
                    handleCreateComment(parts);
                    break;
                case "GET_USER":
                    handleGetUser(parts);
                    break;
                case "MARK_READ":
                    handleMarkRead(parts);
                    break;
                case "GET_READ_STATUS":
                    handleGetReadStatus(parts);
                    break;
                case "CHAT":
                    handleChat(parts);
                    break;
                case "POST_READ":
                    handlePostRead(parts);
                    break;
                default:
                    sendMessage("ERROR|Unknown command: " + command);
            }
        } catch (Exception e) {
            sendMessage("ERROR|" + e.getMessage());
            e.printStackTrace();
        }
    }

    // AUTH 처리
    private void handleAuth(String[] parts) {
        if (parts.length >= 3) {
            this.userId = Long.parseLong(parts[1]);
            this.nickname = parts[2];

            connectionManager.addClient(userId, nickname, this);
            sendMessage("CONNECTED:success");
            connectionManager.broadcastUserList();
        } else {
            sendMessage("CONNECTED:fail");  // 실패 응답 추가
        }
    }

    // LOGIN 처리
    private void handleLogin(String[] parts) {
        if (parts.length < 3) {
            sendMessage("LOGIN_RESPONSE|ERROR|입력 오류");
            return;
        }

        String username = parts[1];
        String password = parts[2];

        try {
            User user = userService.login(username, password);
            sendMessage("LOGIN_RESPONSE|SUCCESS|" + user.getUserId() + "|" + user.getNickname());
        } catch (Exception e) {
            sendMessage("LOGIN_RESPONSE|ERROR|" + e.getMessage());
        }
    }

    // REGISTER 처리
    private void handleRegister(String[] parts) {
        if (parts.length < 4) {
            sendMessage("REGISTER_RESPONSE|ERROR|입력 오류");
            return;
        }

        String username = parts[1];
        String password = parts[2];
        String nickname = parts[3];

        try {
            User user = userService.register(username, password, nickname);
            sendMessage("REGISTER_RESPONSE|SUCCESS|" + user.getUserId() + "|" + user.getNickname());
        } catch (Exception e) {
            sendMessage("REGISTER_RESPONSE|ERROR|" + e.getMessage());
        }
    }

    // GET_ROOMS 처리
    private void handleGetRooms() {
        try {
            List<Room> rooms = roomService.findAllRooms();
            StringBuilder sb = new StringBuilder("GET_ROOMS_RESPONSE|SUCCESS");

            for (Room room : rooms) {
                User creator = userService.findById(room.getCreatorId()).orElse(null);
                String creatorName = creator != null ? creator.getNickname() : "알 수 없음";

                sb.append("|")
                        .append(room.getRoomId()).append(":")
                        .append(room.getRoomName()).append(":")
                        .append(room.getCreatorId()).append(":")
                        .append(creatorName).append(":")
                        .append(room.getCreatedAt());
            }

            sendMessage(sb.toString());
        } catch (Exception e) {
            sendMessage("GET_ROOMS_RESPONSE|ERROR|" + e.getMessage());
        }
    }

    // CREATE_ROOM 처리
    private void handleCreateRoom(String[] parts) {
        if (parts.length < 2 || userId == null) {
            sendMessage("CREATE_ROOM_RESPONSE|ERROR|입력 오류");
            return;
        }

        String roomName = parts[1];

        try {
            Room room = roomService.createRoom(roomName, userId);
            sendMessage("CREATE_ROOM_RESPONSE|SUCCESS|" + room.getRoomId());
        } catch (Exception e) {
            sendMessage("CREATE_ROOM_RESPONSE|ERROR|" + e.getMessage());
        }
    }

    // DELETE_ROOM 처리
    private void handleDeleteRoom(String[] parts) {
        if (parts.length < 2 || userId == null) {
            sendMessage("DELETE_ROOM_RESPONSE|ERROR|입력 오류");
            return;
        }

        Long roomId = Long.parseLong(parts[1]);

        try {
            roomService.deleteRoom(roomId, userId);
            sendMessage("DELETE_ROOM_RESPONSE|SUCCESS");
        } catch (Exception e) {
            sendMessage("DELETE_ROOM_RESPONSE|ERROR|" + e.getMessage());
        }
    }

    // GET_POSTS 처리
    private void handleGetPosts(String[] parts) {
        if (parts.length < 2) {
            sendMessage("GET_POSTS_RESPONSE|ERROR|입력 오류");
            return;
        }

        Long roomId = Long.parseLong(parts[1]);

        try {
            List<Post> posts = postService.findByRoomId(roomId);
            StringBuilder sb = new StringBuilder("GET_POSTS_RESPONSE|SUCCESS");

            for (Post post : posts) {
                User author = userService.findById(post.getAuthorId()).orElse(null);
                String authorName = author != null ? author.getNickname() : "알 수 없음";

                sb.append("|")
                        .append(post.getPostId()).append(":")
                        .append(encode(post.getTitle())).append(":")

                        .append(authorName).append(":")
                        .append(post.getPostType()).append(":")
                        .append(post.getCreatedAt());
            }

            sendMessage(sb.toString());
        } catch (Exception e) {
            sendMessage("GET_POSTS_RESPONSE|ERROR|" + e.getMessage());
        }
    }

    // CREATE_POST 처리
    private void handleCreatePost(String[] parts) {
        if (parts.length < 5 || userId == null) {
            sendMessage("CREATE_POST_RESPONSE|ERROR|입력 오류");
            return;
        }

        Long roomId = Long.parseLong(parts[1]);
        String title = decode(parts[2]);
        String content = decode(parts[3]);
        Post.PostType postType = Post.PostType.valueOf(parts[4]);

        try {
            Post post = postService.createPost(roomId, userId, title, content, postType);
            sendMessage("CREATE_POST_RESPONSE|SUCCESS|" + post.getPostId());
        } catch (Exception e) {
            sendMessage("CREATE_POST_RESPONSE|ERROR|" + e.getMessage());
        }
    }

    // DELETE_POST 처리
    private void handleDeletePost(String[] parts) {
        if (parts.length < 2 || userId == null) {
            sendMessage("DELETE_POST_RESPONSE|ERROR|입력 오류");
            return;
        }

        Long postId = Long.parseLong(parts[1]);

        try {
            postService.deletePost(postId, userId);
            sendMessage("DELETE_POST_RESPONSE|SUCCESS");
        } catch (Exception e) {
            sendMessage("DELETE_POST_RESPONSE|ERROR|" + e.getMessage());
        }
    }

    // GET_COMMENTS 처리
    private void handleGetComments(String[] parts) {
        if (parts.length < 2) {
            sendMessage("GET_COMMENTS_RESPONSE|ERROR|입력 오류");
            return;
        }

        Long postId = Long.parseLong(parts[1]);

        try {
            List<Comment> comments = commentService.findByPostId(postId);
            StringBuilder sb = new StringBuilder("GET_COMMENTS_RESPONSE|SUCCESS");

            for (Comment comment : comments) {
                User author = userService.findById(comment.getAuthorId()).orElse(null);
                String authorName = author != null ? author.getNickname() : "알 수 없음";

                sb.append("|")
                        .append(comment.getCommentId()).append(":")
                        .append(authorName).append(":")

                        .append(encode(comment.getContent())).append(":")
                        .append(comment.getCreatedAt());
            }

            sendMessage(sb.toString());
        } catch (Exception e) {
            sendMessage("GET_COMMENTS_RESPONSE|ERROR|" + e.getMessage());
        }
    }

    // CREATE_COMMENT 처리
    private void handleCreateComment(String[] parts) {
        if (parts.length < 3 || userId == null) {
            sendMessage("CREATE_COMMENT_RESPONSE|ERROR|입력 오류");
            return;
        }

        Long postId = Long.parseLong(parts[1]);
        String content = decode(parts[2]);

        try {
            Comment comment = commentService.createComment(postId, userId, content);
            sendMessage("CREATE_COMMENT_RESPONSE|SUCCESS|" + comment.getCommentId());
        } catch (Exception e) {
            sendMessage("CREATE_COMMENT_RESPONSE|ERROR|" + e.getMessage());
        }
    }

    // GET_USER 처리
    private void handleGetUser(String[] parts) {
        if (parts.length < 2) {
            sendMessage("GET_USER_RESPONSE|ERROR|입력 오류");
            return;
        }

        Long targetUserId = Long.parseLong(parts[1]);

        try {
            User user = userService.findById(targetUserId).orElse(null);
            if (user != null) {
                sendMessage("GET_USER_RESPONSE|SUCCESS|" + user.getNickname());
            } else {
                sendMessage("GET_USER_RESPONSE|ERROR|사용자를 찾을 수 없습니다");
            }
        } catch (Exception e) {
            sendMessage("GET_USER_RESPONSE|ERROR|" + e.getMessage());
        }
    }

    // MARK_READ 처리
    private void handleMarkRead(String[] parts) {
        if (parts.length < 2 || userId == null) {
            sendMessage("MARK_READ_RESPONSE|ERROR|입력 오류");
            return;
        }

        Long postId = Long.parseLong(parts[1]);

        try {
            if (!postReadStatusService.hasRead(postId, userId)) {
                postReadStatusService.markAsRead(postId, userId);
            }
            sendMessage("MARK_READ_RESPONSE|SUCCESS");
        } catch (Exception e) {
            sendMessage("MARK_READ_RESPONSE|ERROR|" + e.getMessage());
        }
    }

    // GET_READ_STATUS 처리
    private void handleGetReadStatus(String[] parts) {
        if (parts.length < 2) {
            sendMessage("GET_READ_STATUS_RESPONSE|ERROR|입력 오류");
            return;
        }

        Long postId = Long.parseLong(parts[1]);

        try {
            List<PostReadStatus> statuses = postReadStatusService.getReadStatusByPost(postId);
            StringBuilder sb = new StringBuilder("GET_READ_STATUS_RESPONSE|SUCCESS");

            for (PostReadStatus status : statuses) {
                User user = userService.findById(status.getUserId()).orElse(null);
                String userName = user != null ? user.getNickname() : "알 수 없음";

                sb.append("|")
                        .append(status.getUserId()).append(":")
                        .append(userName);
            }

            sendMessage(sb.toString());
        } catch (Exception e) {
            sendMessage("GET_READ_STATUS_RESPONSE|ERROR|" + e.getMessage());
        }
    }

    // CHAT 처리
    private void handleChat(String[] parts) {
        if (parts.length >= 2) {
            String chatMessage = parts[1];
            connectionManager.broadcast(nickname + ": " + chatMessage);
        }
    }

    // POST_READ 처리 (기존)
    private void handlePostRead(String[] parts) {
        if (parts.length >= 2) {
            String postId = parts[1];
            connectionManager.broadcast("POST_READ:" + userId + ":" + postId);
        }
    }

    // 메시지 전송
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    // 연결 종료
    public void disconnect() {
        if (!running) return;
        running = false;
        try {
            if (userId != null) {
                connectionManager.removeClient(userId);
                // 서버 전체 종료 시에는 아래 broadcast가 병목이 됨.
                // 하지만 개별 클라이언트가 나갈 때는 필요함.
                // 일단 유지하되, 예외 발생 시 무시하도록 try-catch로 감쌉니다.
                try {
                    connectionManager.broadcastUserList();
                } catch (Exception e) {
                    // 브로드캐스트 실패는 무시 (서버 종료 상황 등)
                }
            }
        } catch (Exception e) {
            // 무시
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