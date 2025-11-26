package org.study.platform.socket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {

    // userId -> ClientHandler 매핑
    private final Map<Long, ClientHandler> clients;

    public ConnectionManager() {
        this.clients = new ConcurrentHashMap<>();
    }

    // 클라이언트 추가
    public void addClient(Long userId, String nickname, ClientHandler handler) {
        clients.put(userId, handler);
        System.out.println("사용자 접속: " + nickname + " (ID: " + userId + ")");
    }

    // 클라이언트 제거
    public void removeClient(Long userId) {
        ClientHandler handler = clients.remove(userId);
        if (handler != null) {
            System.out.println("사용자 퇴장: " + handler.getNickname() + " (ID: " + userId + ")");
        }
    }

    // 모든 클라이언트에게 메시지 브로드캐스트
    public void broadcast(String message) {
        for (ClientHandler handler : clients.values()) {
            handler.sendMessage(message);
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

        // 마지막 쉼표 제거
        if (userList.length() > 9) {
            userList.setLength(userList.length() - 1);
        }

        broadcast(userList.toString());
    }

    // 현재 접속자 수
    public int getOnlineCount() {
        return clients.size();
    }

    // 특정 사용자가 접속 중인지 확인
    public boolean isUserOnline(Long userId) {
        return clients.containsKey(userId);
    }

    // 모든 클라이언트 연결 종료 (서버 종료 시 호출)
    public void disconnectAll() {
        // values()를 바로 순회하지 않고 복사본을 만들어 순회하거나,
        // 개별 disconnect 로직을 타지 않고 강제로 소켓만 닫습니다.

        System.out.println("전체 클라이언트 연결 해제 시작...");

        // ConcurrentModificationException 방지를 위해 값 복사본으로 수행
        for (ClientHandler handler : clients.values()) {
            try {
                handler.sendMessage("SERVER_SHUTDOWN"); // 클라에게 종료 알림 (선택사항)

                handler.disconnect();

            } catch (Exception e) {
                // 종료 중 오류는 무시
            }
        }
        clients.clear();
        System.out.println("모든 클라이언트 연결이 종료되었습니다.");
    }
}