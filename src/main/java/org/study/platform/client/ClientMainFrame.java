package org.study.platform.client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ClientMainFrame extends JFrame {

    private SocketClient socketClient;
    private Long currentUserId;
    private String currentUserNickname;

    private JTable roomTable;
    private DefaultTableModel tableModel;
    private JButton createRoomButton;
    private JButton enterRoomButton;
    private JButton deleteRoomButton;
    private JButton refreshButton;
    private JLabel userInfoLabel;
    private JTextArea onlineUsersArea;

    public ClientMainFrame(SocketClient socketClient, Long userId, String nickname) {
        this.socketClient = socketClient;
        this.currentUserId = userId;
        this.currentUserNickname = nickname;
        initComponents();
        loadRooms();
        setupSocketListener();
    }

    private void initComponents() {
        setTitle("스터디 플랫폼 - 클라이언트");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 상단 패널
        JPanel topPanel = new JPanel(new BorderLayout());
        userInfoLabel = new JLabel("사용자: " + currentUserNickname + " (ID: " + currentUserId + ")");
        userInfoLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topPanel.add(userInfoLabel, BorderLayout.WEST);
        add(topPanel, BorderLayout.NORTH);

        // 중앙 패널
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("스터디 방 목록"));

        // 테이블
        String[] columnNames = {"방 ID", "방 이름", "방장", "생성일"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        roomTable = new JTable(tableModel);
        roomTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane tableScrollPane = new JScrollPane(roomTable);
        centerPanel.add(tableScrollPane, BorderLayout.CENTER);

        // 버튼 패널
        JPanel buttonPanel = new JPanel();
        createRoomButton = new JButton("방 만들기");
        enterRoomButton = new JButton("입장");
        deleteRoomButton = new JButton("삭제");
        refreshButton = new JButton("새로고침");

        buttonPanel.add(createRoomButton);
        buttonPanel.add(enterRoomButton);
        buttonPanel.add(deleteRoomButton);
        buttonPanel.add(refreshButton);
        centerPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        // 우측 패널
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("현재 접속자"));
        rightPanel.setPreferredSize(new Dimension(200, 0));

        onlineUsersArea = new JTextArea();
        onlineUsersArea.setEditable(false);
        JScrollPane usersScrollPane = new JScrollPane(onlineUsersArea);
        rightPanel.add(usersScrollPane, BorderLayout.CENTER);

        add(rightPanel, BorderLayout.EAST);

        // 이벤트 리스너
        createRoomButton.addActionListener(e -> handleCreateRoom());
        enterRoomButton.addActionListener(e -> handleEnterRoom());
        deleteRoomButton.addActionListener(e -> handleDeleteRoom());
        refreshButton.addActionListener(e -> loadRooms());

        // 더블클릭으로 입장
        roomTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    handleEnterRoom();
                }
            }
        });

        // 종료 처리
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                int confirm = JOptionPane.showConfirmDialog(
                        ClientMainFrame.this,
                        "정말로 종료하시겠습니까?",
                        "종료 확인",
                        JOptionPane.YES_NO_OPTION
                );

                if (confirm == JOptionPane.YES_OPTION) {
                    new Thread(() -> {
                        if (socketClient != null) {
                            socketClient.disconnect();
                        }
                        System.exit(0);
                    }).start();

                    dispose();
                }
            }
        });
    }

    // 소켓 리스너 설정
    private void setupSocketListener() {
        socketClient.addMessageListener(message -> {
            SwingUtilities.invokeLater(() -> {
                if (message.startsWith("USERLIST:")) {
                    updateOnlineUsers(message.substring(9));
                }
            });
        });
    }

    // 접속자 목록 업데이트
    private void updateOnlineUsers(String userListData) {
        onlineUsersArea.setText("");
        if (!userListData.isEmpty()) {
            String[] users = userListData.split(",");
            for (String user : users) {
                String[] userData = user.split(":");
                if (userData.length == 2) {
                    onlineUsersArea.append(userData[1] + "\n");
                }
            }
        }
    }

    // 방 목록 로드
    private void loadRooms() {
        tableModel.setRowCount(0);

        try {
            String response = socketClient.getRooms();

            if (response == null) {
                JOptionPane.showMessageDialog(this, "서버 응답 없음",
                        "오류", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String[] parts = response.split("\\|");

            if (parts.length >= 2 && parts[1].equals("SUCCESS")) {
                // 방 목록 파싱
                for (int i = 2; i < parts.length; i++) {
                    String[] roomData = parts[i].split(":");
                    if (roomData.length >= 5) {
                        Object[] row = {
                                Long.parseLong(roomData[0]),  // roomId
                                roomData[1],                   // roomName
                                roomData[3],                   // creatorName
                                roomData[4]                    // createdAt
                        };
                        tableModel.addRow(row);
                    }
                }
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "방 목록 로드 실패: " + ex.getMessage(),
                    "오류", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    // 방 만들기
    private void handleCreateRoom() {
        String roomName = JOptionPane.showInputDialog(this, "방 이름을 입력하세요:",
                "방 만들기", JOptionPane.PLAIN_MESSAGE);

        if (roomName != null && !roomName.trim().isEmpty()) {
            try {
                String response = socketClient.createRoom(roomName.trim());

                if (response == null) {
                    JOptionPane.showMessageDialog(this, "서버 응답 없음",
                            "오류", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String[] parts = response.split("\\|");

                if (parts.length >= 2 && parts[1].equals("SUCCESS")) {
                    JOptionPane.showMessageDialog(this, "방이 생성되었습니다.",
                            "성공", JOptionPane.INFORMATION_MESSAGE);
                    loadRooms();
                } else {
                    String errorMsg = parts.length >= 3 ? parts[2] : "방 생성 실패";
                    JOptionPane.showMessageDialog(this, errorMsg,
                            "오류", JOptionPane.ERROR_MESSAGE);
                }

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "방 생성 실패: " + ex.getMessage(),
                        "오류", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 방 입장
    private void handleEnterRoom() {
        int selectedRow = roomTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "입장할 방을 선택하세요.",
                    "선택 필요", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long roomId = (Long) tableModel.getValueAt(selectedRow, 0);
        String roomName = (String) tableModel.getValueAt(selectedRow, 1);

        // RoomFrame 열기
        SwingUtilities.invokeLater(() -> {
            ClientRoomFrame roomFrame = new ClientRoomFrame(socketClient, currentUserId, currentUserNickname, roomId, roomName);
            roomFrame.setVisible(true);
        });
    }

    // 방 삭제
    private void handleDeleteRoom() {
        int selectedRow = roomTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "삭제할 방을 선택하세요.",
                    "선택 필요", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long roomId = (Long) tableModel.getValueAt(selectedRow, 0);

        int confirm = JOptionPane.showConfirmDialog(this, "정말로 이 방을 삭제하시겠습니까?",
                "삭제 확인", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                String response = socketClient.deleteRoom(roomId);

                if (response == null) {
                    JOptionPane.showMessageDialog(this, "서버 응답 없음",
                            "오류", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String[] parts = response.split("\\|");

                if (parts.length >= 2 && parts[1].equals("SUCCESS")) {
                    JOptionPane.showMessageDialog(this, "방이 삭제되었습니다.",
                            "성공", JOptionPane.INFORMATION_MESSAGE);
                    loadRooms();
                } else {
                    String errorMsg = parts.length >= 3 ? parts[2] : "방 삭제 실패";
                    JOptionPane.showMessageDialog(this, errorMsg,
                            "오류", JOptionPane.ERROR_MESSAGE);
                }

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "방 삭제 실패: " + ex.getMessage(),
                        "오류", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}