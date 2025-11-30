package org.study.platform.client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

public class ClientRoomFrame extends JFrame {

    private SocketClient socketClient;
    private Long currentUserId;
    private String currentUserNickname;
    private Long roomId;
    private String roomName;

    // UI 컴포넌트
    private JTable noticeTable;
    private DefaultTableModel noticeTableModel;
    private JTable postTable;
    private DefaultTableModel postTableModel;
    private JTextArea postContentArea;
    private JTextArea commentArea;
    private JTextField commentField;
    private JButton writePostButton;
    private JButton deletePostButton;
    private JButton refreshButton;
    private JButton backButton;
    private JLabel roomInfoLabel;
    private JTextArea roomUsersArea;
    private JTextArea readStatusArea;

    // 과제 관련
    private JPanel assignmentPanel;
    private JLabel assignmentLabel;
    private JButton assignmentActionButton;

    // 현재 선택된 게시글
    private Long selectedPostId;
    private Long selectedPostAuthorId;
    private boolean selectedPostHasAssignment;

    private SocketClient.MessageListener messageListener;

    public ClientRoomFrame(SocketClient socketClient, Long userId, String nickname, Long roomId, String roomName) {
        this.socketClient = socketClient;
        this.currentUserId = userId;
        this.currentUserNickname = nickname;
        this.roomId = roomId;
        this.roomName = roomName;
        initComponents();
        loadNotices();
        loadGeneralPosts();
        setupRealtimeListener();
        socketClient.joinRoom(roomId);
    }

    private void initComponents() {
        setTitle("스터디 플랫폼 - " + roomName);
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(5, 5));

        // 상단 패널
        JPanel topPanel = new JPanel(new BorderLayout());
        roomInfoLabel = new JLabel("  방: " + roomName + " | 사용자: " + currentUserNickname);
        roomInfoLabel.setFont(new Font("Dialog", Font.BOLD, 14));
        roomInfoLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        backButton = new JButton("← 메인으로");
        JPanel topRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topRightPanel.add(backButton);

        topPanel.add(roomInfoLabel, BorderLayout.WEST);
        topPanel.add(topRightPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // 좌측 패널
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setPreferredSize(new Dimension(380, 0));

        // 공지사항
        JPanel noticePanel = new JPanel(new BorderLayout());
        noticePanel.setBorder(BorderFactory.createTitledBorder("📢 공지사항"));
        noticePanel.setPreferredSize(new Dimension(380, 150));

        String[] noticeColumns = {"ID", "제목", "작성자"};
        noticeTableModel = new DefaultTableModel(noticeColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        noticeTable = new JTable(noticeTableModel);
        noticeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane noticeScroll = new JScrollPane(noticeTable);
        noticePanel.add(noticeScroll, BorderLayout.CENTER);

        leftPanel.add(noticePanel, BorderLayout.NORTH);

        // 일반 게시글
        JPanel postListPanel = new JPanel(new BorderLayout());
        postListPanel.setBorder(BorderFactory.createTitledBorder("📝 게시글"));

        String[] postColumns = {"ID", "제목", "작성자", "과제"};
        postTableModel = new DefaultTableModel(postColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        postTable = new JTable(postTableModel);
        postTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane postScroll = new JScrollPane(postTable);
        postListPanel.add(postScroll, BorderLayout.CENTER);

        JPanel postButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        writePostButton = new JButton("글쓰기");
        deletePostButton = new JButton("삭제");
        refreshButton = new JButton("새로고침");
        postButtonPanel.add(writePostButton);
        postButtonPanel.add(deletePostButton);
        postButtonPanel.add(refreshButton);
        postListPanel.add(postButtonPanel, BorderLayout.SOUTH);

        leftPanel.add(postListPanel, BorderLayout.CENTER);

        add(leftPanel, BorderLayout.WEST);

        // 중앙 패널
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));

        // 게시글 내용
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createTitledBorder("게시글 내용"));
        postContentArea = new JTextArea();
        postContentArea.setEditable(false);
        postContentArea.setLineWrap(true);
        postContentArea.setWrapStyleWord(true);
        postContentArea.setFont(new Font("Dialog", Font.PLAIN, 13));
        JScrollPane contentScroll = new JScrollPane(postContentArea);
        contentPanel.add(contentScroll, BorderLayout.CENTER);

        // 과제 패널
        assignmentPanel = new JPanel(new BorderLayout(5, 5));
        assignmentPanel.setBorder(BorderFactory.createTitledBorder("📋 과제"));
        assignmentPanel.setPreferredSize(new Dimension(0, 80));
        assignmentPanel.setVisible(false);

        assignmentLabel = new JLabel("과제 정보");
        assignmentActionButton = new JButton("과제 제출");
        JPanel assignBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        assignBtnPanel.add(assignmentActionButton);

        assignmentPanel.add(assignmentLabel, BorderLayout.CENTER);
        assignmentPanel.add(assignBtnPanel, BorderLayout.EAST);
        contentPanel.add(assignmentPanel, BorderLayout.SOUTH);

        centerPanel.add(contentPanel, BorderLayout.CENTER);

        // 댓글
        JPanel commentPanel = new JPanel(new BorderLayout(5, 5));
        commentPanel.setBorder(BorderFactory.createTitledBorder("💬 댓글"));
        commentPanel.setPreferredSize(new Dimension(0, 200));

        commentArea = new JTextArea();
        commentArea.setEditable(false);
        commentArea.setLineWrap(true);
        commentArea.setFont(new Font("Dialog", Font.PLAIN, 12));
        JScrollPane commentScroll = new JScrollPane(commentArea);
        commentPanel.add(commentScroll, BorderLayout.CENTER);

        JPanel commentInputPanel = new JPanel(new BorderLayout(5, 5));
        commentField = new JTextField();
        commentField.setFont(new Font("Dialog", Font.PLAIN, 13));
        JButton sendCommentButton = new JButton("작성");
        commentInputPanel.add(commentField, BorderLayout.CENTER);
        commentInputPanel.add(sendCommentButton, BorderLayout.EAST);
        commentPanel.add(commentInputPanel, BorderLayout.SOUTH);

        centerPanel.add(commentPanel, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        // 우측 패널
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setPreferredSize(new Dimension(180, 0));

        JPanel roomUsersPanel = new JPanel(new BorderLayout());
        roomUsersPanel.setBorder(BorderFactory.createTitledBorder("👥 참여자"));
        roomUsersPanel.setPreferredSize(new Dimension(180, 150));
        roomUsersArea = new JTextArea();
        roomUsersArea.setEditable(false);
        roomUsersArea.setFont(new Font("Dialog", Font.PLAIN, 12));
        JScrollPane usersScroll = new JScrollPane(roomUsersArea);
        roomUsersPanel.add(usersScroll, BorderLayout.CENTER);
        rightPanel.add(roomUsersPanel, BorderLayout.NORTH);

        JPanel readPanel = new JPanel(new BorderLayout());
        readPanel.setBorder(BorderFactory.createTitledBorder("👁 읽은 사람"));
        readStatusArea = new JTextArea();
        readStatusArea.setEditable(false);
        readStatusArea.setFont(new Font("Dialog", Font.PLAIN, 11));
        JScrollPane readScroll = new JScrollPane(readStatusArea);
        readPanel.add(readScroll, BorderLayout.CENTER);
        rightPanel.add(readPanel, BorderLayout.CENTER);

        add(rightPanel, BorderLayout.EAST);

        // 이벤트 리스너
        backButton.addActionListener(e -> {
            socketClient.leaveRoom(roomId);
            dispose();
        });

        noticeTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && noticeTable.getSelectedRow() >= 0) {
                postTable.clearSelection();
                Long postId = (Long) noticeTableModel.getValueAt(noticeTable.getSelectedRow(), 0);
                loadPostDetail(postId);
            }
        });

        postTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && postTable.getSelectedRow() >= 0) {
                noticeTable.clearSelection();
                Long postId = (Long) postTableModel.getValueAt(postTable.getSelectedRow(), 0);
                loadPostDetail(postId);
            }
        });

        writePostButton.addActionListener(e -> handleWritePost());
        deletePostButton.addActionListener(e -> handleDeletePost());
        refreshButton.addActionListener(e -> {
            loadNotices();
            loadGeneralPosts();
        });

        // Enter 키로 댓글 작성
        commentField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleWriteComment();
                }
            }
        });

        sendCommentButton.addActionListener(e -> handleWriteComment());
        assignmentActionButton.addActionListener(e -> handleAssignmentAction());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                socketClient.leaveRoom(roomId);
                if (messageListener != null) {
                    socketClient.removeMessageListener(messageListener);
                }
                dispose();
            }
        });
    }

    private void setupRealtimeListener() {
        messageListener = message -> {
            SwingUtilities.invokeLater(() -> {
                try {
                    if (message.startsWith("NEW_POST:" + roomId)) {
                        loadNotices();
                        loadGeneralPosts();
                    } else if (message.startsWith("POST_DELETED:" + roomId)) {
                        loadNotices();
                        loadGeneralPosts();
                        postContentArea.setText("");
                        commentArea.setText("");
                        assignmentPanel.setVisible(false);
                    } else if (message.startsWith("NEW_COMMENT:") && selectedPostId != null) {
                        String[] parts = message.split(":");
                        if (parts.length >= 2 && parts[1].equals(selectedPostId.toString())) {
                            loadComments(selectedPostId);
                        }
                    } else if (message.startsWith("POST_READ:") && selectedPostId != null) {
                        String[] parts = message.split(":");
                        if (parts.length >= 2 && parts[1].equals(selectedPostId.toString())) {
                            loadReadStatus(selectedPostId);
                        }
                    } else if (message.startsWith("ROOM_JOIN:" + roomId)) {
                        String[] parts = message.split(":");
                        if (parts.length >= 4) {
                            roomUsersArea.append(parts[3] + " 님이 입장했습니다.\n");
                        }
                    } else if (message.startsWith("ROOM_LEAVE:" + roomId)) {
                        String[] parts = message.split(":");
                        if (parts.length >= 4) {
                            roomUsersArea.append(parts[3] + " 님이 퇴장했습니다.\n");
                        }
                    } else if (message.startsWith("ROOM_USERLIST:" + roomId)) {
                        updateRoomUsers(message);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        };
        socketClient.addMessageListener(messageListener);
    }

    private void updateRoomUsers(String message) {
        String[] parts = message.split(":", 3);
        if (parts.length >= 3) {
            roomUsersArea.setText("현재 참여자:\n");
            String[] users = parts[2].split(",");
            for (String user : users) {
                String[] userData = user.split(":");
                if (userData.length >= 2) {
                    roomUsersArea.append("• " + userData[1] + "\n");
                }
            }
        }
    }

    private void loadNotices() {
        noticeTableModel.setRowCount(0);
        try {
            String response = socketClient.getNotices(roomId);
            if (response == null) return;

            String[] parts = response.split("\\|");
            if (parts.length >= 2 && parts[1].equals("SUCCESS")) {
                for (int i = 2; i < parts.length; i++) {
                    String[] data = parts[i].split(":");
                    if (data.length >= 5) {
                        Object[] row = {
                                Long.parseLong(data[0]),
                                socketClient.decodeText(data[1]),
                                data[2]
                        };
                        noticeTableModel.addRow(row);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadGeneralPosts() {
        postTableModel.setRowCount(0);
        try {
            String response = socketClient.getGeneralPosts(roomId);
            if (response == null) return;

            String[] parts = response.split("\\|");
            if (parts.length >= 2 && parts[1].equals("SUCCESS")) {
                for (int i = 2; i < parts.length; i++) {
                    String[] data = parts[i].split(":");
                    if (data.length >= 5) {
                        Object[] row = {
                                Long.parseLong(data[0]),
                                socketClient.decodeText(data[1]),
                                data[2],
                                data[3].equals("true") ? "📋" : ""
                        };
                        postTableModel.addRow(row);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadPostDetail(Long postId) {
        this.selectedPostId = postId;
        try {
            String response = socketClient.getPostDetail(postId);
            if (response == null) return;

            String[] parts = response.split("\\|");
            if (parts.length >= 9 && parts[1].equals("SUCCESS")) {
                this.selectedPostAuthorId = Long.parseLong(parts[4]);
                this.selectedPostHasAssignment = parts[7].equals("true");

                StringBuilder sb = new StringBuilder();
                sb.append("제목: ").append(socketClient.decodeText(parts[2])).append("\n");
                sb.append("작성자: ").append(parts[5]).append("\n");
                sb.append("작성일: ").append(parts[8]).append("\n");
                sb.append("유형: ").append(parts[6].equals("NOTICE") ? "공지사항" : "일반").append("\n");
                sb.append("\n").append(socketClient.decodeText(parts[3]));

                postContentArea.setText(sb.toString());
                postContentArea.setCaretPosition(0);

                loadComments(postId);
                loadReadStatus(postId);
                markAsRead(postId);

                if (selectedPostHasAssignment) {
                    loadAssignmentInfo(postId);
                } else {
                    assignmentPanel.setVisible(false);
                }
            }
        } catch (Exception e) {
            postContentArea.setText("게시글을 불러올 수 없습니다.");
        }
    }

    private void loadAssignmentInfo(Long postId) {
        try {
            String response = socketClient.getAssignment(postId);
            if (response != null && response.contains("SUCCESS")) {
                String[] parts = response.split("\\|");
                if (parts.length >= 6) {
                    String assignTitle = socketClient.decodeText(parts[2]);
                    String dueDate = parts[4].isEmpty() ? "없음" : parts[4];

                    assignmentLabel.setText("<html><b>" + assignTitle + "</b><br>마감: " + dueDate + "</html>");

                    if (selectedPostAuthorId.equals(currentUserId)) {
                        assignmentActionButton.setText("제출물 관리");
                    } else {
                        Long assignmentId = Long.parseLong(parts[1]);
                        String subResponse = socketClient.getMySubmission(assignmentId);
                        if (subResponse != null && subResponse.contains("SUCCESS")) {
                            assignmentActionButton.setText("제출물 확인");
                        } else {
                            assignmentActionButton.setText("과제 제출");
                        }
                    }
                    assignmentPanel.setVisible(true);
                }
            } else {
                assignmentPanel.setVisible(false);
            }
        } catch (Exception e) {
            assignmentPanel.setVisible(false);
        }
    }

    private void loadComments(Long postId) {
        commentArea.setText("");
        try {
            String response = socketClient.getComments(postId);
            if (response == null) return;

            String[] parts = response.split("\\|");
            if (parts.length >= 2 && parts[1].equals("SUCCESS")) {
                if (parts.length == 2) {
                    commentArea.setText("댓글이 없습니다.");
                } else {
                    for (int i = 2; i < parts.length; i++) {
                        String[] data = parts[i].split(":");
                        if (data.length >= 4) {
                            commentArea.append("[" + data[1] + "] " + data[3] + "\n");
                            commentArea.append(socketClient.decodeText(data[2]) + "\n\n");
                        }
                    }
                }
            }
        } catch (Exception e) {
            commentArea.setText("댓글 로드 실패");
        }
    }

    private void loadReadStatus(Long postId) {
        readStatusArea.setText("");
        try {
            String response = socketClient.getReadStatus(postId);
            if (response == null) return;

            String[] parts = response.split("\\|");
            if (parts.length >= 2 && parts[1].equals("SUCCESS")) {
                int count = parts.length - 2;
                readStatusArea.append("총 " + count + "명\n\n");
                for (int i = 2; i < parts.length; i++) {
                    String[] data = parts[i].split(":");
                    if (data.length >= 2) {
                        readStatusArea.append("• " + data[1] + "\n");
                    }
                }
            }
        } catch (Exception e) {
            readStatusArea.setText("로드 실패");
        }
    }

    private void markAsRead(Long postId) {
        try {
            socketClient.markRead(postId);
            socketClient.sendPostRead(postId);
        } catch (Exception e) {
            // 무시
        }
    }

    private void handleWritePost() {
        // 간단한 다이얼로그 (클라이언트 모드에서는 방장 여부 확인이 어려우므로 일반으로만 처리)
        JPanel panel = new JPanel(new BorderLayout(5, 10));

        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        typePanel.add(new JLabel("유형:"));
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"일반", "공지"});
        typePanel.add(typeCombo);

        JTextField titleField = new JTextField(30);
        JTextArea contentArea = new JTextArea(10, 30);
        contentArea.setLineWrap(true);

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.add(new JLabel("제목:"), BorderLayout.NORTH);
        titlePanel.add(titleField, BorderLayout.CENTER);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(new JLabel("내용:"), BorderLayout.NORTH);
        contentPanel.add(new JScrollPane(contentArea), BorderLayout.CENTER);

        panel.add(typePanel, BorderLayout.NORTH);
        panel.add(titlePanel, BorderLayout.CENTER);
        panel.add(contentPanel, BorderLayout.SOUTH);

        int result = JOptionPane.showConfirmDialog(this, panel, "게시글 작성",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String title = titleField.getText().trim();
            String content = contentArea.getText().trim();
            String type = typeCombo.getSelectedIndex() == 0 ? "GENERAL" : "NOTICE";

            if (title.isEmpty() || content.isEmpty()) {
                JOptionPane.showMessageDialog(this, "제목과 내용을 입력하세요.");
                return;
            }

            try {
                String response = socketClient.createPost(roomId, title, content, type);
                if (response != null && response.contains("SUCCESS")) {
                    loadNotices();
                    loadGeneralPosts();
                    JOptionPane.showMessageDialog(this, "게시글이 작성되었습니다.");
                } else {
                    JOptionPane.showMessageDialog(this, "작성 실패");
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "오류: " + e.getMessage());
            }
        }
    }

    private void handleDeletePost() {
        if (selectedPostId == null) {
            JOptionPane.showMessageDialog(this, "삭제할 게시글을 선택하세요.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "정말로 삭제하시겠습니까?",
                "삭제 확인", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                String response = socketClient.deletePost(selectedPostId);
                if (response != null && response.contains("SUCCESS")) {
                    loadNotices();
                    loadGeneralPosts();
                    postContentArea.setText("");
                    commentArea.setText("");
                    readStatusArea.setText("");
                    assignmentPanel.setVisible(false);
                    selectedPostId = null;
                    JOptionPane.showMessageDialog(this, "삭제되었습니다.");
                } else {
                    JOptionPane.showMessageDialog(this, "삭제 실패");
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "오류: " + e.getMessage());
            }
        }
    }

    private void handleWriteComment() {
        if (selectedPostId == null) {
            JOptionPane.showMessageDialog(this, "게시글을 먼저 선택하세요.");
            return;
        }

        String content = commentField.getText().trim();
        if (content.isEmpty()) return;

        try {
            String response = socketClient.createComment(selectedPostId, content);
            if (response != null && response.contains("SUCCESS")) {
                commentField.setText("");
                loadComments(selectedPostId);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "댓글 작성 실패: " + e.getMessage());
        }
    }

    private void handleAssignmentAction() {
        if (selectedPostId == null) return;

        try {
            String response = socketClient.getAssignment(selectedPostId);
            if (response == null || !response.contains("SUCCESS")) return;

            String[] parts = response.split("\\|");
            Long assignmentId = Long.parseLong(parts[1]);
            String assignTitle = socketClient.decodeText(parts[2]);
            String assignDesc = socketClient.decodeText(parts[3]);

            if (selectedPostAuthorId.equals(currentUserId)) {
                AssignmentManageDialog dialog = new AssignmentManageDialog(this, socketClient, assignmentId, assignTitle);
                dialog.setVisible(true);
            } else {
                String subResponse = socketClient.getMySubmission(assignmentId);
                if (subResponse != null && subResponse.contains("SUCCESS")) {
                    String[] subParts = subResponse.split("\\|");
                    String myContent = socketClient.decodeText(subParts[2]);
                    String status = subParts[4];
                    String score = subParts[5].isEmpty() ? "미채점" : subParts[5] + "점";
                    String feedback = subParts[6].isEmpty() ? "없음" : socketClient.decodeText(subParts[6]);

                    JOptionPane.showMessageDialog(this,
                            "제출 내용: " + myContent + "\n" +
                                    "상태: " + status + "\n" +
                                    "점수: " + score + "\n" +
                                    "피드백: " + feedback,
                            "내 제출물", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    AssignmentSubmitDialog dialog = new AssignmentSubmitDialog(this, assignTitle, assignDesc);
                    dialog.setVisible(true);

                    if (dialog.isConfirmed()) {
                        String content = dialog.getContent();
                        File file = dialog.getSelectedFile();

                        String fileName = file != null ? file.getName() : null;
                        String filePath = file != null ? file.getAbsolutePath() : null;
                        Long fileSize = file != null ? file.length() : null;

                        String submitResponse = socketClient.submitAssignment(assignmentId, content, fileName, filePath, fileSize);
                        if (submitResponse != null && submitResponse.contains("SUCCESS")) {
                            JOptionPane.showMessageDialog(this, "과제가 제출되었습니다.");
                            loadAssignmentInfo(selectedPostId);
                        } else {
                            JOptionPane.showMessageDialog(this, "제출 실패");
                        }
                    }
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "오류: " + e.getMessage());
        }
    }
}
