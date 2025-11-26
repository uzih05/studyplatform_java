package org.study.platform.client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ClientRoomFrame extends JFrame {

    private SocketClient socketClient;
    private Long currentUserId;
    private String currentUserNickname;
    private Long roomId;
    private String roomName;

    private JTable postTable;
    private DefaultTableModel tableModel;
    private JTextArea postContentArea;
    private JTextArea commentArea;
    private JButton writePostButton;
    private JButton writeCommentButton;
    private JButton deletePostButton;
    private JButton refreshButton;
    private JButton backButton;
    private JComboBox<String> postTypeComboBox;
    private JLabel roomInfoLabel;
    private JTextArea readStatusArea;

    public ClientRoomFrame(SocketClient socketClient, Long userId, String nickname, Long roomId, String roomName) {
        this.socketClient = socketClient;
        this.currentUserId = userId;
        this.currentUserNickname = nickname;
        this.roomId = roomId;
        this.roomName = roomName;
        initComponents();
        loadPosts();
        setupSocketListener();
    }

    private void initComponents() {
        setTitle("스터디 플랫폼 - " + roomName);
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 상단 패널
        JPanel topPanel = new JPanel(new BorderLayout());
        roomInfoLabel = new JLabel("방: " + roomName + " | 사용자: " + currentUserNickname);
        roomInfoLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        backButton = new JButton("← 메인으로");
        JPanel topRightPanel = new JPanel();
        topRightPanel.add(backButton);

        topPanel.add(roomInfoLabel, BorderLayout.WEST);
        topPanel.add(topRightPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // 좌측 패널 (게시글 목록)
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("게시글 목록"));
        leftPanel.setPreferredSize(new Dimension(400, 0));

        // 테이블
        String[] columnNames = {"ID", "제목", "작성자", "타입", "작성일"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        postTable = new JTable(tableModel);
        postTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane tableScrollPane = new JScrollPane(postTable);
        leftPanel.add(tableScrollPane, BorderLayout.CENTER);

        // 게시글 목록 버튼
        JPanel leftButtonPanel = new JPanel();
        writePostButton = new JButton("글쓰기");
        deletePostButton = new JButton("삭제");
        refreshButton = new JButton("새로고침");

        postTypeComboBox = new JComboBox<>(new String[]{"일반", "공지"});

        leftButtonPanel.add(postTypeComboBox);
        leftButtonPanel.add(writePostButton);
        leftButtonPanel.add(deletePostButton);
        leftButtonPanel.add(refreshButton);
        leftPanel.add(leftButtonPanel, BorderLayout.SOUTH);

        add(leftPanel, BorderLayout.WEST);

        // 중앙 패널 (게시글 내용 + 댓글)
        JPanel centerPanel = new JPanel(new BorderLayout());

        // 게시글 내용
        JPanel postPanel = new JPanel(new BorderLayout());
        postPanel.setBorder(BorderFactory.createTitledBorder("게시글 내용"));
        postContentArea = new JTextArea();
        postContentArea.setEditable(false);
        postContentArea.setLineWrap(true);
        JScrollPane postScrollPane = new JScrollPane(postContentArea);
        postPanel.add(postScrollPane, BorderLayout.CENTER);

        centerPanel.add(postPanel, BorderLayout.CENTER);

        // 댓글 영역
        JPanel commentPanel = new JPanel(new BorderLayout());
        commentPanel.setBorder(BorderFactory.createTitledBorder("댓글"));
        commentPanel.setPreferredSize(new Dimension(0, 250));

        commentArea = new JTextArea();
        commentArea.setEditable(false);
        commentArea.setLineWrap(true);
        JScrollPane commentScrollPane = new JScrollPane(commentArea);
        commentPanel.add(commentScrollPane, BorderLayout.CENTER);

        // 댓글 작성 패널
        JPanel commentWritePanel = new JPanel(new BorderLayout());
        JTextField commentTextField = new JTextField();
        writeCommentButton = new JButton("댓글 작성");
        commentWritePanel.add(commentTextField, BorderLayout.CENTER);
        commentWritePanel.add(writeCommentButton, BorderLayout.EAST);
        commentPanel.add(commentWritePanel, BorderLayout.SOUTH);

        centerPanel.add(commentPanel, BorderLayout.SOUTH);
        add(centerPanel, BorderLayout.CENTER);

        // 우측 패널 (읽음 상태)
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("읽음 상태"));
        rightPanel.setPreferredSize(new Dimension(200, 0));

        readStatusArea = new JTextArea();
        readStatusArea.setEditable(false);
        JScrollPane readScrollPane = new JScrollPane(readStatusArea);
        rightPanel.add(readScrollPane, BorderLayout.CENTER);

        add(rightPanel, BorderLayout.EAST);

        // 이벤트 리스너
        backButton.addActionListener(e -> dispose());
        writePostButton.addActionListener(e -> handleWritePost());
        deletePostButton.addActionListener(e -> handleDeletePost());
        refreshButton.addActionListener(e -> loadPosts());
        writeCommentButton.addActionListener(e -> handleWriteComment(commentTextField));

        // 게시글 선택 시 내용 표시
        postTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = postTable.getSelectedRow();
                if (selectedRow != -1) {
                    Long postId = (Long) tableModel.getValueAt(selectedRow, 0);
                    loadPostContent(postId);
                    loadComments(postId);
                    loadReadStatus(postId);
                    markPostAsRead(postId);
                }
            }
        });

        // macOS 닫기 버튼
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                dispose();
            }
        });
    }

    // 소켓 리스너 설정
    private void setupSocketListener() {
        socketClient.addMessageListener(message -> {
            SwingUtilities.invokeLater(() -> {
                if (message.startsWith("POST_READ:")) {
                    int selectedRow = postTable.getSelectedRow();
                    if (selectedRow != -1) {
                        Long postId = (Long) tableModel.getValueAt(selectedRow, 0);
                        loadReadStatus(postId);
                    }
                }
            });
        });
    }

    // 게시글 목록 로드
    private void loadPosts() {
        tableModel.setRowCount(0);

        try {
            String response = socketClient.getPosts(roomId);

            if (response == null) return;

            String[] parts = response.split("\\|");

            if (parts.length >= 2 && parts[1].equals("SUCCESS")) {
                for (int i = 2; i < parts.length; i++) {
                    String[] postData = parts[i].split(":");
                    if (postData.length >= 5) {
                        Object[] row = {
                                Long.parseLong(postData[0]),  // postId
                                decode(postData[1]),
                                postData[2],
                                postData[3].equals("NOTICE") ? "공지" : "일반",
                                postData[4]                   // createdAt
                        };
                        tableModel.addRow(row);
                    }
                }
            }

        } catch (Exception ex) {
            System.err.println("게시글 로드 실패: " + ex.getMessage());
        }
    }

    // 게시글 내용 로드 (간단히 처리)
    private void loadPostContent(Long postId) {
        postContentArea.setText("게시글 상세 내용은 추가 프로토콜이 필요합니다.\nPost ID: " + postId);
    }

    // 댓글 로드
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
                        String[] commentData = parts[i].split(":");
                        if (commentData.length >= 4) {
                            commentArea.append("[" + commentData[1] + "] " + commentData[3] + "\n");
                            commentArea.append(decode(commentData[2]) + "\n\n");
                        }
                    }
                }
            }

        } catch (Exception ex) {
            commentArea.setText("댓글 로드 중 오류 발생");
        }
    }

    // 읽음 상태 로드
    private void loadReadStatus(Long postId) {
        readStatusArea.setText("");

        try {
            String response = socketClient.getReadStatus(postId);

            if (response == null) return;

            String[] parts = response.split("\\|");

            if (parts.length >= 2 && parts[1].equals("SUCCESS")) {
                int count = parts.length - 2;
                readStatusArea.append("읽은 사용자 (" + count + "명)\n\n");

                if (count == 0) {
                    readStatusArea.append("아직 읽은 사용자가 없습니다.");
                } else {
                    for (int i = 2; i < parts.length; i++) {
                        String[] userData = parts[i].split(":");
                        if (userData.length >= 2) {
                            readStatusArea.append(userData[1] + "\n");
                        }
                    }
                }
            }

        } catch (Exception ex) {
            readStatusArea.setText("읽음 상태 로드 중 오류 발생");
        }
    }

    // 게시글 읽음 처리
    private void markPostAsRead(Long postId) {
        try {
            socketClient.markRead(postId);
            socketClient.sendPostRead(postId);
        } catch (Exception ex) {
            System.err.println("읽음 처리 실패: " + ex.getMessage());
        }
    }

    // 게시글 작성
    private void handleWritePost() {
        String title = JOptionPane.showInputDialog(this, "제목을 입력하세요:",
                "게시글 작성", JOptionPane.PLAIN_MESSAGE);

        if (title == null || title.trim().isEmpty()) return;

        String content = JOptionPane.showInputDialog(this, "내용을 입력하세요:",
                "게시글 작성", JOptionPane.PLAIN_MESSAGE);

        if (content == null || content.trim().isEmpty()) return;

        try {
            String postType = postTypeComboBox.getSelectedIndex() == 0 ? "GENERAL" : "NOTICE";
            String response = socketClient.createPost(roomId, title.trim(), content.trim(), postType);

            if (response == null) {
                JOptionPane.showMessageDialog(this, "서버 응답 없음", "오류", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String[] parts = response.split("\\|");

            if (parts.length >= 2 && parts[1].equals("SUCCESS")) {
                JOptionPane.showMessageDialog(this, "게시글이 작성되었습니다.",
                        "성공", JOptionPane.INFORMATION_MESSAGE);
                loadPosts();
            } else {
                String errorMsg = parts.length >= 3 ? parts[2] : "게시글 작성 실패";
                JOptionPane.showMessageDialog(this, errorMsg, "오류", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "게시글 작성 실패: " + ex.getMessage(),
                    "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 게시글 삭제
    private void handleDeletePost() {
        int selectedRow = postTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "삭제할 게시글을 선택하세요.",
                    "선택 필요", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long postId = (Long) tableModel.getValueAt(selectedRow, 0);

        int confirm = JOptionPane.showConfirmDialog(this, "정말로 이 게시글을 삭제하시겠습니까?",
                "삭제 확인", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                String response = socketClient.deletePost(postId);

                if (response == null) {
                    JOptionPane.showMessageDialog(this, "서버 응답 없음", "오류", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String[] parts = response.split("\\|");

                if (parts.length >= 2 && parts[1].equals("SUCCESS")) {
                    postContentArea.setText("");
                    commentArea.setText("");
                    readStatusArea.setText("");
                    loadPosts();
                    JOptionPane.showMessageDialog(this, "게시글이 삭제되었습니다.",
                            "성공", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    String errorMsg = parts.length >= 3 ? parts[2] : "게시글 삭제 실패";
                    JOptionPane.showMessageDialog(this, errorMsg, "오류", JOptionPane.ERROR_MESSAGE);
                }

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "게시글 삭제 실패: " + ex.getMessage(),
                        "오류", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 댓글 작성
    private void handleWriteComment(JTextField commentTextField) {
        int selectedRow = postTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "게시글을 먼저 선택하세요.",
                    "선택 필요", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String content = commentTextField.getText().trim();
        if (content.isEmpty()) {
            JOptionPane.showMessageDialog(this, "댓글 내용을 입력하세요.",
                    "입력 필요", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long postId = (Long) tableModel.getValueAt(selectedRow, 0);

        try {
            String response = socketClient.createComment(postId, content);

            if (response == null) {
                JOptionPane.showMessageDialog(this, "서버 응답 없음", "오류", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String[] parts = response.split("\\|");

            if (parts.length >= 2 && parts[1].equals("SUCCESS")) {
                commentTextField.setText("");
                loadComments(postId);
            } else {
                String errorMsg = parts.length >= 3 ? parts[2] : "댓글 작성 실패";
                JOptionPane.showMessageDialog(this, errorMsg, "오류", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "댓글 작성 실패: " + ex.getMessage(),
                    "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String decode(String text) {
        if (text == null) return "";
        return text.replace("&#124;", "|")
                .replace("&#58;", ":")
                .replace("&#10;", "\n");
    }

}