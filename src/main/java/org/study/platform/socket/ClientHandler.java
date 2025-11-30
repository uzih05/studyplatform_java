package org.study.platform.socket;

import org.study.platform.entity.*;
import org.study.platform.service.*;
import org.springframework.context.ApplicationContext;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ClientHandler implements Runnable {

    private Socket clientSocket;
    private ConnectionManager connectionManager;
    private ApplicationContext context;
    private BufferedReader in;
    private PrintWriter out;
    private Long userId;
    private String nickname;
    private Long currentRoomId;
    private volatile boolean running = true;

    private UserService userService;
    private RoomService roomService;
    private PostService postService;
    private CommentService commentService;
    private PostReadStatusService postReadStatusService;
    private AssignmentService assignmentService;

    public ClientHandler(Socket socket, ConnectionManager connectionManager, ApplicationContext context) {
        this.clientSocket = socket;
        this.connectionManager = connectionManager;
        this.context = context;

        this.userService = context.getBean(UserService.class);
        this.roomService = context.getBean(RoomService.class);
        this.postService = context.getBean(PostService.class);
        this.commentService = context.getBean(CommentService.class);
        this.postReadStatusService = context.getBean(PostReadStatusService.class);
        this.assignmentService = context.getBean(AssignmentService.class);
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

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

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
                case "JOIN_ROOM":
                    handleJoinRoom(parts);
                    break;
                case "LEAVE_ROOM":
                    handleLeaveRoom(parts);
                    break;
                case "GET_POSTS":
                    handleGetPosts(parts);
                    break;
                case "GET_NOTICES":
                    handleGetNotices(parts);
                    break;
                case "GET_GENERAL_POSTS":
                    handleGetGeneralPosts(parts);
                    break;
                case "CREATE_POST":
                    handleCreatePost(parts);
                    break;
                case "DELETE_POST":
                    handleDeletePost(parts);
                    break;
                case "GET_POST_DETAIL":
                    handleGetPostDetail(parts);
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
                case "CREATE_ASSIGNMENT":
                    handleCreateAssignment(parts);
                    break;
                case "GET_ASSIGNMENT":
                    handleGetAssignment(parts);
                    break;
                case "SUBMIT_ASSIGNMENT":
                    handleSubmitAssignment(parts);
                    break;
                case "GET_SUBMISSIONS":
                    handleGetSubmissions(parts);
                    break;
                case "GET_MY_SUBMISSION":
                    handleGetMySubmission(parts);
                    break;
                case "GRADE_SUBMISSION":
                    handleGradeSubmission(parts);
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

    private void handleAuth(String[] parts) {
        if (parts.length >= 3) {
            this.userId = Long.parseLong(parts[1]);
            this.nickname = parts[2];
            connectionManager.addClient(userId, nickname, this);
            sendMessage("CONNECTED:success");
            connectionManager.broadcastUserList();
        } else {
            sendMessage("CONNECTED:fail");
        }
    }

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

    private void handleCreateRoom(String[] parts) {
        if (parts.length < 2 || userId == null) {
            sendMessage("CREATE_ROOM_RESPONSE|ERROR|입력 오류");
            return;
        }
        String roomName = parts[1];
        try {
            Room room = roomService.createRoom(roomName, userId);
            sendMessage("CREATE_ROOM_RESPONSE|SUCCESS|" + room.getRoomId());
            // 모든 클라이언트에게 새 방 알림
            connectionManager.notifyNewRoom(room.getRoomId(), roomName, userId, nickname);
        } catch (Exception e) {
            sendMessage("CREATE_ROOM_RESPONSE|ERROR|" + e.getMessage());
        }
    }

    private void handleDeleteRoom(String[] parts) {
        if (parts.length < 2 || userId == null) {
            sendMessage("DELETE_ROOM_RESPONSE|ERROR|입력 오류");
            return;
        }
        Long roomId = Long.parseLong(parts[1]);
        try {
            roomService.deleteRoom(roomId, userId);
            sendMessage("DELETE_ROOM_RESPONSE|SUCCESS");
            connectionManager.notifyRoomDeleted(roomId);
        } catch (Exception e) {
            sendMessage("DELETE_ROOM_RESPONSE|ERROR|" + e.getMessage());
        }
    }

    private void handleJoinRoom(String[] parts) {
        if (parts.length < 2 || userId == null) {
            sendMessage("JOIN_ROOM_RESPONSE|ERROR|입력 오류");
            return;
        }
        Long roomId = Long.parseLong(parts[1]);
        this.currentRoomId = roomId;
        connectionManager.joinRoom(roomId, userId);
        sendMessage("JOIN_ROOM_RESPONSE|SUCCESS");
        connectionManager.broadcastRoomUserList(roomId);
    }

    private void handleLeaveRoom(String[] parts) {
        if (parts.length < 2 || userId == null) {
            sendMessage("LEAVE_ROOM_RESPONSE|ERROR|입력 오류");
            return;
        }
        Long roomId = Long.parseLong(parts[1]);
        connectionManager.leaveRoom(roomId, userId);
        if (currentRoomId != null && currentRoomId.equals(roomId)) {
            currentRoomId = null;
        }
        sendMessage("LEAVE_ROOM_RESPONSE|SUCCESS");
        connectionManager.broadcastRoomUserList(roomId);
    }

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
                        .append(post.getHasAssignment()).append(":")
                        .append(post.getCreatedAt());
            }
            sendMessage(sb.toString());
        } catch (Exception e) {
            sendMessage("GET_POSTS_RESPONSE|ERROR|" + e.getMessage());
        }
    }

    private void handleGetNotices(String[] parts) {
        if (parts.length < 2) {
            sendMessage("GET_NOTICES_RESPONSE|ERROR|입력 오류");
            return;
        }
        Long roomId = Long.parseLong(parts[1]);
        try {
            List<Post> posts = postService.findNoticesByRoomId(roomId);
            StringBuilder sb = new StringBuilder("GET_NOTICES_RESPONSE|SUCCESS");
            for (Post post : posts) {
                User author = userService.findById(post.getAuthorId()).orElse(null);
                String authorName = author != null ? author.getNickname() : "알 수 없음";
                sb.append("|")
                        .append(post.getPostId()).append(":")
                        .append(encode(post.getTitle())).append(":")
                        .append(authorName).append(":")
                        .append(post.getHasAssignment()).append(":")
                        .append(post.getCreatedAt());
            }
            sendMessage(sb.toString());
        } catch (Exception e) {
            sendMessage("GET_NOTICES_RESPONSE|ERROR|" + e.getMessage());
        }
    }

    private void handleGetGeneralPosts(String[] parts) {
        if (parts.length < 2) {
            sendMessage("GET_GENERAL_POSTS_RESPONSE|ERROR|입력 오류");
            return;
        }
        Long roomId = Long.parseLong(parts[1]);
        try {
            List<Post> posts = postService.findGeneralPostsByRoomId(roomId);
            StringBuilder sb = new StringBuilder("GET_GENERAL_POSTS_RESPONSE|SUCCESS");
            for (Post post : posts) {
                User author = userService.findById(post.getAuthorId()).orElse(null);
                String authorName = author != null ? author.getNickname() : "알 수 없음";
                sb.append("|")
                        .append(post.getPostId()).append(":")
                        .append(encode(post.getTitle())).append(":")
                        .append(authorName).append(":")
                        .append(post.getHasAssignment()).append(":")
                        .append(post.getCreatedAt());
            }
            sendMessage(sb.toString());
        } catch (Exception e) {
            sendMessage("GET_GENERAL_POSTS_RESPONSE|ERROR|" + e.getMessage());
        }
    }

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
            // 실시간 알림
            connectionManager.notifyNewPost(roomId, post.getPostId(), title, nickname, postType.name());
        } catch (Exception e) {
            sendMessage("CREATE_POST_RESPONSE|ERROR|" + e.getMessage());
        }
    }

    private void handleDeletePost(String[] parts) {
        if (parts.length < 2 || userId == null) {
            sendMessage("DELETE_POST_RESPONSE|ERROR|입력 오류");
            return;
        }
        Long postId = Long.parseLong(parts[1]);
        try {
            Post post = postService.findById(postId).orElse(null);
            Long roomId = post != null ? post.getRoomId() : null;
            postService.deletePost(postId, userId);
            sendMessage("DELETE_POST_RESPONSE|SUCCESS");
            if (roomId != null) {
                connectionManager.notifyPostDeleted(roomId, postId);
            }
        } catch (Exception e) {
            sendMessage("DELETE_POST_RESPONSE|ERROR|" + e.getMessage());
        }
    }

    private void handleGetPostDetail(String[] parts) {
        if (parts.length < 2) {
            sendMessage("GET_POST_DETAIL_RESPONSE|ERROR|입력 오류");
            return;
        }
        Long postId = Long.parseLong(parts[1]);
        try {
            Post post = postService.findById(postId).orElse(null);
            if (post != null) {
                User author = userService.findById(post.getAuthorId()).orElse(null);
                String authorName = author != null ? author.getNickname() : "알 수 없음";
                sendMessage("GET_POST_DETAIL_RESPONSE|SUCCESS|" +
                        post.getPostId() + "|" +
                        encode(post.getTitle()) + "|" +
                        encode(post.getContent()) + "|" +
                        post.getAuthorId() + "|" +
                        authorName + "|" +
                        post.getPostType() + "|" +
                        post.getHasAssignment() + "|" +
                        post.getCreatedAt());
            } else {
                sendMessage("GET_POST_DETAIL_RESPONSE|ERROR|게시글을 찾을 수 없습니다");
            }
        } catch (Exception e) {
            sendMessage("GET_POST_DETAIL_RESPONSE|ERROR|" + e.getMessage());
        }
    }

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
            // 실시간 댓글 알림
            Post post = postService.findById(postId).orElse(null);
            if (post != null) {
                connectionManager.notifyNewComment(post.getRoomId(), postId, comment.getCommentId(), nickname, content);
            }
        } catch (Exception e) {
            sendMessage("CREATE_COMMENT_RESPONSE|ERROR|" + e.getMessage());
        }
    }

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

    private void handleMarkRead(String[] parts) {
        if (parts.length < 2 || userId == null) {
            sendMessage("MARK_READ_RESPONSE|ERROR|입력 오류");
            return;
        }
        Long postId = Long.parseLong(parts[1]);
        try {
            if (!postReadStatusService.hasRead(postId, userId)) {
                postReadStatusService.markAsRead(postId, userId);
                // 실시간 읽음 알림
                Post post = postService.findById(postId).orElse(null);
                if (post != null) {
                    connectionManager.notifyPostRead(post.getRoomId(), postId, userId, nickname);
                }
            }
            sendMessage("MARK_READ_RESPONSE|SUCCESS");
        } catch (Exception e) {
            sendMessage("MARK_READ_RESPONSE|ERROR|" + e.getMessage());
        }
    }

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

    // === 과제 관련 핸들러 ===

    private void handleCreateAssignment(String[] parts) {
        if (parts.length < 4 || userId == null) {
            sendMessage("CREATE_ASSIGNMENT_RESPONSE|ERROR|입력 오류");
            return;
        }
        Long postId = Long.parseLong(parts[1]);
        String title = decode(parts[2]);
        String description = decode(parts[3]);
        LocalDateTime dueDate = null;
        if (parts.length >= 5 && !parts[4].isEmpty()) {
            dueDate = LocalDateTime.parse(parts[4], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        try {
            // 게시글 작성자만 과제 생성 가능
            Post post = postService.findById(postId).orElse(null);
            if (post == null) {
                sendMessage("CREATE_ASSIGNMENT_RESPONSE|ERROR|게시글을 찾을 수 없습니다");
                return;
            }
            if (!post.getAuthorId().equals(userId)) {
                sendMessage("CREATE_ASSIGNMENT_RESPONSE|ERROR|게시글 작성자만 과제를 생성할 수 있습니다");
                return;
            }
            Assignment assignment = assignmentService.createAssignment(postId, title, description, dueDate);
            sendMessage("CREATE_ASSIGNMENT_RESPONSE|SUCCESS|" + assignment.getAssignmentId());
        } catch (Exception e) {
            sendMessage("CREATE_ASSIGNMENT_RESPONSE|ERROR|" + e.getMessage());
        }
    }

    private void handleGetAssignment(String[] parts) {
        if (parts.length < 2) {
            sendMessage("GET_ASSIGNMENT_RESPONSE|ERROR|입력 오류");
            return;
        }
        Long postId = Long.parseLong(parts[1]);
        try {
            Assignment assignment = assignmentService.findByPostId(postId).orElse(null);
            if (assignment != null) {
                String dueDate = assignment.getDueDate() != null ?
                        assignment.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "";
                sendMessage("GET_ASSIGNMENT_RESPONSE|SUCCESS|" +
                        assignment.getAssignmentId() + "|" +
                        encode(assignment.getTitle()) + "|" +
                        encode(assignment.getDescription()) + "|" +
                        dueDate + "|" +
                        assignment.getCreatedAt());
            } else {
                sendMessage("GET_ASSIGNMENT_RESPONSE|ERROR|과제가 없습니다");
            }
        } catch (Exception e) {
            sendMessage("GET_ASSIGNMENT_RESPONSE|ERROR|" + e.getMessage());
        }
    }

    private void handleSubmitAssignment(String[] parts) {
        if (parts.length < 3 || userId == null) {
            sendMessage("SUBMIT_ASSIGNMENT_RESPONSE|ERROR|입력 오류");
            return;
        }
        Long assignmentId = Long.parseLong(parts[1]);
        String content = decode(parts[2]);
        String fileName = parts.length >= 4 ? decode(parts[3]) : null;
        String filePath = parts.length >= 5 ? parts[4] : null;
        Long fileSize = parts.length >= 6 && !parts[5].isEmpty() ? Long.parseLong(parts[5]) : null;
        try {
            AssignmentSubmission submission = assignmentService.submitAssignment(
                    assignmentId, userId, content, fileName, filePath, fileSize);
            sendMessage("SUBMIT_ASSIGNMENT_RESPONSE|SUCCESS|" + submission.getSubmissionId());
            // 작성자에게 알림
            Assignment assignment = assignmentService.findById(assignmentId).orElse(null);
            if (assignment != null) {
                Post post = postService.findById(assignment.getPostId()).orElse(null);
                if (post != null) {
                    connectionManager.notifyAssignmentSubmitted(post.getAuthorId(), assignmentId, userId, nickname);
                }
            }
        } catch (Exception e) {
            sendMessage("SUBMIT_ASSIGNMENT_RESPONSE|ERROR|" + e.getMessage());
        }
    }

    private void handleGetSubmissions(String[] parts) {
        if (parts.length < 2) {
            sendMessage("GET_SUBMISSIONS_RESPONSE|ERROR|입력 오류");
            return;
        }
        Long assignmentId = Long.parseLong(parts[1]);
        try {
            List<AssignmentSubmission> submissions = assignmentService.getSubmissionsByAssignment(assignmentId);
            StringBuilder sb = new StringBuilder("GET_SUBMISSIONS_RESPONSE|SUCCESS");
            for (AssignmentSubmission sub : submissions) {
                User submitter = userService.findById(sub.getUserId()).orElse(null);
                String submitterName = submitter != null ? submitter.getNickname() : "알 수 없음";
                sb.append("|")
                        .append(sub.getSubmissionId()).append(":")
                        .append(sub.getUserId()).append(":")
                        .append(submitterName).append(":")
                        .append(encode(sub.getContent() != null ? sub.getContent() : "")).append(":")
                        .append(sub.getFileName() != null ? encode(sub.getFileName()) : "").append(":")
                        .append(sub.getStatus()).append(":")
                        .append(sub.getScore() != null ? sub.getScore() : "").append(":")
                        .append(sub.getSubmittedAt());
            }
            sendMessage(sb.toString());
        } catch (Exception e) {
            sendMessage("GET_SUBMISSIONS_RESPONSE|ERROR|" + e.getMessage());
        }
    }

    private void handleGetMySubmission(String[] parts) {
        if (parts.length < 2 || userId == null) {
            sendMessage("GET_MY_SUBMISSION_RESPONSE|ERROR|입력 오류");
            return;
        }
        Long assignmentId = Long.parseLong(parts[1]);
        try {
            AssignmentSubmission sub = assignmentService.getSubmission(assignmentId, userId).orElse(null);
            if (sub != null) {
                sendMessage("GET_MY_SUBMISSION_RESPONSE|SUCCESS|" +
                        sub.getSubmissionId() + "|" +
                        encode(sub.getContent() != null ? sub.getContent() : "") + "|" +
                        (sub.getFileName() != null ? encode(sub.getFileName()) : "") + "|" +
                        sub.getStatus() + "|" +
                        (sub.getScore() != null ? sub.getScore() : "") + "|" +
                        (sub.getFeedback() != null ? encode(sub.getFeedback()) : "") + "|" +
                        sub.getSubmittedAt());
            } else {
                sendMessage("GET_MY_SUBMISSION_RESPONSE|ERROR|제출물이 없습니다");
            }
        } catch (Exception e) {
            sendMessage("GET_MY_SUBMISSION_RESPONSE|ERROR|" + e.getMessage());
        }
    }

    private void handleGradeSubmission(String[] parts) {
        if (parts.length < 4 || userId == null) {
            sendMessage("GRADE_SUBMISSION_RESPONSE|ERROR|입력 오류");
            return;
        }
        Long submissionId = Long.parseLong(parts[1]);
        Integer score = parts[2].isEmpty() ? null : Integer.parseInt(parts[2]);
        String feedback = decode(parts[3]);
        try {
            AssignmentSubmission submission = assignmentService.gradeSubmission(submissionId, score, feedback);
            sendMessage("GRADE_SUBMISSION_RESPONSE|SUCCESS");
        } catch (Exception e) {
            sendMessage("GRADE_SUBMISSION_RESPONSE|ERROR|" + e.getMessage());
        }
    }

    private void handleChat(String[] parts) {
        if (parts.length >= 2) {
            String chatMessage = parts[1];
            connectionManager.broadcast(nickname + ": " + chatMessage);
        }
    }

    private void handlePostRead(String[] parts) {
        if (parts.length >= 2) {
            String postId = parts[1];
            connectionManager.broadcast("POST_READ:" + userId + ":" + postId);
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public void disconnect() {
        if (!running) return;
        running = false;
        try {
            if (userId != null) {
                if (currentRoomId != null) {
                    connectionManager.leaveRoom(currentRoomId, userId);
                }
                connectionManager.removeClient(userId);
                try {
                    connectionManager.broadcastUserList();
                } catch (Exception e) {
                    // 무시
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
