package org.study.platform.socket;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {

    private final Map<Long, ClientHandler> clients;
    // 방별 접속자 관리
    private final Map<Long, Set<Long>> roomUsers;

    public ConnectionManager() {
        this.clients = new ConcurrentHashMap<>();
        this.roomUsers = new ConcurrentHashMap<>();
    }

    public void addClient(Long userId, String nickname, ClientHandler handler) {
        clients.put(userId, handler);
        System.out.println("사용자 접속: " + nickname + " (ID: " + userId + ")");
    }

    public void removeClient(Long userId) {
        ClientHandler handler = clients.remove(userId);
        if (handler != null) {
            System.out.println("사용자 퇴장: " + handler.getNickname() + " (ID: " + userId + ")");
        }
        // 모든 방에서 해당 사용자 제거
        for (Set<Long> users : roomUsers.values()) {
            users.remove(userId);
        }
    }

    // 방 입장
    public void joinRoom(Long roomId, Long userId) {
        roomUsers.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(userId);
        ClientHandler handler = clients.get(userId);
        if (handler != null) {
            // 해당 방의 다른 사용자들에게 입장 알림
            broadcastToRoom(roomId, "ROOM_JOIN:" + roomId + ":" + userId + ":" + handler.getNickname());
        }
    }

    // 방 퇴장
    public void leaveRoom(Long roomId, Long userId) {
        Set<Long> users = roomUsers.get(roomId);
        if (users != null) {
            users.remove(userId);
            ClientHandler handler = clients.get(userId);
            if (handler != null) {
                broadcastToRoom(roomId, "ROOM_LEAVE:" + roomId + ":" + userId + ":" + handler.getNickname());
            }
        }
    }

    // 모든 클라이언트에게 브로드캐스트
    public void broadcast(String message) {
        for (ClientHandler handler : clients.values()) {
            handler.sendMessage(message);
        }
    }

    // 특정 방의 사용자들에게만 브로드캐스트
    public void broadcastToRoom(Long roomId, String message) {
        Set<Long> users = roomUsers.get(roomId);
        if (users != null) {
            for (Long userId : users) {
                ClientHandler handler = clients.get(userId);
                if (handler != null) {
                    handler.sendMessage(message);
                }
            }
        }
    }

    // 특정 사용자에게 메시지 전송
    public void sendToUser(Long userId, String message) {
        ClientHandler handler = clients.get(userId);
        if (handler != null) {
            handler.sendMessage(message);
        }
    }

    // 접속자 목록 브로드캐스트
    public void broadcastUserList() {
        StringBuilder userList = new StringBuilder("USERLIST:");
        for (ClientHandler handler : clients.values()) {
            userList.append(handler.getUserId())
                    .append(":")
                    .append(handler.getNickname())
                    .append(",");
        }
        if (userList.length() > 9) {
            userList.setLength(userList.length() - 1);
        }
        broadcast(userList.toString());
    }

    // 방별 접속자 목록 브로드캐스트
    public void broadcastRoomUserList(Long roomId) {
        Set<Long> users = roomUsers.get(roomId);
        StringBuilder userList = new StringBuilder("ROOM_USERLIST:" + roomId + ":");
        if (users != null) {
            for (Long userId : users) {
                ClientHandler handler = clients.get(userId);
                if (handler != null) {
                    userList.append(userId).append(":").append(handler.getNickname()).append(",");
                }
            }
            if (userList.charAt(userList.length() - 1) == ',') {
                userList.setLength(userList.length() - 1);
            }
        }
        broadcastToRoom(roomId, userList.toString());
    }

    // === 실시간 동기화 메서드들 ===

    // 새 방 생성 알림
    public void notifyNewRoom(Long roomId, String roomName, Long creatorId, String creatorName) {
        broadcast("NEW_ROOM:" + roomId + ":" + roomName + ":" + creatorId + ":" + creatorName);
    }

    // 방 삭제 알림
    public void notifyRoomDeleted(Long roomId) {
        broadcast("ROOM_DELETED:" + roomId);
    }

    // 새 게시글 알림 (해당 방 사용자들에게)
    public void notifyNewPost(Long roomId, Long postId, String title, String authorName, String postType) {
        broadcastToRoom(roomId, "NEW_POST:" + roomId + ":" + postId + ":" + encode(title) + ":" + authorName + ":" + postType);
        // 모든 사용자에게도 알림 (방 목록에서 새 게시글 표시용)
        broadcast("POST_UPDATE:" + roomId);
    }

    // 게시글 삭제 알림
    public void notifyPostDeleted(Long roomId, Long postId) {
        broadcastToRoom(roomId, "POST_DELETED:" + roomId + ":" + postId);
    }

    // 새 댓글 알림
    public void notifyNewComment(Long roomId, Long postId, Long commentId, String authorName, String content) {
        broadcastToRoom(roomId, "NEW_COMMENT:" + postId + ":" + commentId + ":" + authorName + ":" + encode(content));
    }

    // 게시글 읽음 알림
    public void notifyPostRead(Long roomId, Long postId, Long userId, String nickname) {
        broadcastToRoom(roomId, "POST_READ:" + postId + ":" + userId + ":" + nickname);
    }

    // 과제 제출 알림 (작성자에게)
    public void notifyAssignmentSubmitted(Long postAuthorId, Long assignmentId, Long submitterId, String submitterName) {
        sendToUser(postAuthorId, "ASSIGNMENT_SUBMITTED:" + assignmentId + ":" + submitterId + ":" + submitterName);
    }

    public int getOnlineCount() {
        return clients.size();
    }

    public boolean isUserOnline(Long userId) {
        return clients.containsKey(userId);
    }

    public void disconnectAll() {
        System.out.println("전체 클라이언트 연결 해제 시작...");
        for (ClientHandler handler : clients.values()) {
            try {
                handler.sendMessage("SERVER_SHUTDOWN");
                handler.disconnect();
            } catch (Exception e) {
                // 무시
            }
        }
        clients.clear();
        roomUsers.clear();
        System.out.println("모든 클라이언트 연결이 종료되었습니다.");
    }

    private String encode(String text) {
        if (text == null) return "";
        return text.replace("|", "&#124;")
                .replace(":", "&#58;")
                .replace("\n", "&#10;");
    }
}
