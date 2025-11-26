package org.study.platform.client;

import org.springframework.context.ConfigurableApplicationContext;
import org.study.platform.entity.Room;
import org.study.platform.entity.User;
import org.study.platform.service.RoomService;
import org.study.platform.service.UserService;
import org.study.platform.service.PostService;
import org.study.platform.service.CommentService;
import org.study.platform.service.PostReadStatusService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class MainFrame extends JFrame {

    private User currentUser;
    private SocketClient socketClient;
    private RoomService roomService;
    private UserService userService;
    private ConfigurableApplicationContext context;

    private JTable roomTable;
    private DefaultTableModel tableModel;
    private JButton createRoomButton;
    private JButton enterRoomButton;
    private JButton deleteRoomButton;
    private JButton refreshButton;
    private JLabel userInfoLabel;
    private JTextArea onlineUsersArea;

    public MainFrame(RoomService roomService, UserService userService, ConfigurableApplicationContext context) {
        this.roomService = roomService;
        this.userService = userService;
        this.context = context;
    }

    public void initialize(User user, SocketClient socketClient) {
        this.currentUser = user;
        this.socketClient = socketClient;
        initComponents();
        loadRooms();
        setupSocketListener();
    }

    private void initComponents() {
        setTitle("스터디 플랫폼 - 메인");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 상단 패널
        JPanel topPanel = new JPanel(new BorderLayout());
        userInfoLabel = new JLabel("사용자: " + currentUser.getNickname() + " (ID: " + currentUser.getUserId() + ")");
        userInfoLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topPanel.add(userInfoLabel, BorderLayout.WEST);
        add(topPanel, BorderLayout.NORTH);

        // 중앙 패널
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("스터디 방 목록"));

        // 테이블 생성
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

        // macOS 닫기 버튼 지원
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                int confirm = JOptionPane.showConfirmDialog(
                        MainFrame.this,
                        "정말로 종료하시겠습니까?",
                        "종료 확인",
                        JOptionPane.YES_NO_OPTION
                );

                if (confirm == JOptionPane.YES_OPTION) {
                    if (socketClient != null) {
                        socketClient.disconnect();
                    }
                    dispose();  // 창만 닫기
                    System.exit(0);  // 프로그램 종료
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
            List<Room> rooms = roomService.findAllRooms();
            for (Room room : rooms) {
                // 방장 닉네임 가져오기
                String creatorName = userService.findById(room.getCreatorId())
                        .map(User::getNickname).orElse("알 수 없음");

                Object[] row = {
                        room.getRoomId(),
                        room.getRoomName(),
                        creatorName,
                        room.getCreatedAt()
                };
                tableModel.addRow(row);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "방 목록 로드 실패: " + ex.getMessage(),
                    "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 방 만들기
    private void handleCreateRoom() {
        String roomName = JOptionPane.showInputDialog(this, "방 이름을 입력하세요:",
                "방 만들기", JOptionPane.PLAIN_MESSAGE);

        if (roomName != null && !roomName.trim().isEmpty()) {
            try {
                roomService.createRoom(roomName.trim(), currentUser.getUserId());
                loadRooms();
                JOptionPane.showMessageDialog(this, "방이 생성되었습니다.",
                        "성공", JOptionPane.INFORMATION_MESSAGE);
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

        try {
            Room room = roomService.findById(roomId).orElse(null);
            if (room == null) {
                JOptionPane.showMessageDialog(this, "방 정보를 찾을 수 없습니다.",
                        "오류", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // RoomFrame 열기
            openRoomFrame(room);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "방 입장 실패: " + ex.getMessage(),
                    "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    // RoomFrame 열기
    private void openRoomFrame(Room room) {
        SwingUtilities.invokeLater(() -> {
            // Spring Context에서 Service들 가져오기
            PostService postService = context.getBean(PostService.class);
            CommentService commentService = context.getBean(CommentService.class);
            PostReadStatusService postReadStatusService = context.getBean(PostReadStatusService.class);

            RoomFrame roomFrame = new RoomFrame(postService, commentService, postReadStatusService, userService);
            roomFrame.initialize(currentUser, room, socketClient);
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

        // 실제 Room 객체에서 creatorId 가져오기
        try {
            Room room = roomService.findById(roomId).orElse(null);
            if (room == null) {
                JOptionPane.showMessageDialog(this, "방 정보를 찾을 수 없습니다.",
                        "오류", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 방장 확인
            if (!room.getCreatorId().equals(currentUser.getUserId())) {
                JOptionPane.showMessageDialog(this, "방장만 삭제할 수 있습니다.",
                        "권한 없음", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this, "정말로 이 방을 삭제하시겠습니까?",
                    "삭제 확인", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                roomService.deleteRoom(roomId, currentUser.getUserId());
                loadRooms();
                JOptionPane.showMessageDialog(this, "방이 삭제되었습니다.",
                        "성공", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "방 삭제 실패: " + ex.getMessage(),
                    "오류", JOptionPane.ERROR_MESSAGE);
        }
    }
}