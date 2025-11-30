package org.study.platform.client;

import org.study.platform.entity.*;
import org.study.platform.service.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;

public class RoomFrame extends JFrame {

    private User currentUser;
    private Room currentRoom;
    private SocketClient socketClient;

    private PostService postService;
    private CommentService commentService;
    private PostReadStatusService postReadStatusService;
    private UserService userService;

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

    // 읽음 상태 (축소된 패널)
    private JTextArea readStatusArea;

    // 과제 관련
    private JPanel assignmentPanel;
    private JLabel assignmentLabel;
    private JButton assignmentActionButton;

    // 현재 선택된 게시글
    private Long selectedPostId;
    private Long selectedPostAuthorId;
    private boolean selectedPostHasAssignment;

    // 실시간 리스너
    private SocketClient.MessageListener messageListener;

    public RoomFrame(PostService postService, CommentService commentService,
                     PostReadStatusService postReadStatusService, UserService userService) {
        this.postService = postService;
        this.commentService = commentService;
        this.postReadStatusService = postReadStatusService;
        this.userService = userService;
    }

    public void initialize(User user, Room room, SocketClient socketClient) {
        this.currentUser = user;
        this.currentRoom = room;
        this.socketClient = socketClient;
        initComponents();
        loadNotices();
        loadGeneralPosts();
        setupRealtimeListener();

        // 방 입장 알림
        socketClient.joinRoom(room.getRoomId());
    }

    private void initComponents() {
        setTitle("스터디 플랫폼 - " + currentRoom.getRoomName());
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(5, 5));

        // 상단 패널
        JPanel topPanel = new JPanel(new BorderLayout());
        roomInfoLabel = new JLabel("  방: " + currentRoom.getRoomName() + " | 사용자: " + currentUser.getNickname());
        roomInfoLabel.setFont(new Font("Dialog", Font.BOLD, 14));
        roomInfoLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        backButton = new JButton("← 메인으로");
        JPanel topRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topRightPanel.add(backButton);

        topPanel.add(roomInfoLabel, BorderLayout.WEST);
        topPanel.add(topRightPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // 좌측 패널 (공지 + 게시글 목록)
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setPreferredSize(new Dimension(380, 0));

        // 공지사항 영역 (상단)
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
        noticeTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        noticeTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        noticeTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        JScrollPane noticeScroll = new JScrollPane(noticeTable);
        noticePanel.add(noticeScroll, BorderLayout.CENTER);

        leftPanel.add(noticePanel, BorderLayout.NORTH);

        // 일반 게시글 영역
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
        postTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        postTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        postTable.getColumnModel().getColumn(2).setPreferredWidth(70);
        postTable.getColumnModel().getColumn(3).setPreferredWidth(40);
        JScrollPane postScroll = new JScrollPane(postTable);
        postListPanel.add(postScroll, BorderLayout.CENTER);

        // 게시글 버튼
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

        // 중앙 패널 (게시글 상세)
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

        // 과제 패널 (게시글 내용 아래)
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

        // 댓글 영역
        JPanel commentPanel = new JPanel(new BorderLayout(5, 5));
        commentPanel.setBorder(BorderFactory.createTitledBorder("💬 댓글"));
        commentPanel.setPreferredSize(new Dimension(0, 200));

        commentArea = new JTextArea();
        commentArea.setEditable(false);
        commentArea.setLineWrap(true);
        commentArea.setFont(new Font("Dialog", Font.PLAIN, 12));
        JScrollPane commentScroll = new JScrollPane(commentArea);
        commentPanel.add(commentScroll, BorderLayout.CENTER);

        // 댓글 입력
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

        // 방 참여자 (상단)
        JPanel roomUsersPanel = new JPanel(new BorderLayout());
        roomUsersPanel.setBorder(BorderFactory.createTitledBorder("👥 참여자"));
        roomUsersPanel.setPreferredSize(new Dimension(180, 150));
        roomUsersArea = new JTextArea();
        roomUsersArea.setEditable(false);
        roomUsersArea.setFont(new Font("Dialog", Font.PLAIN, 12));
        JScrollPane usersScroll = new JScrollPane(roomUsersArea);
        roomUsersPanel.add(usersScroll, BorderLayout.CENTER);
        rightPanel.add(roomUsersPanel, BorderLayout.NORTH);

        // 읽음 상태 (하단, 축소)
        JPanel readPanel = new JPanel(new BorderLayout());
        readPanel.setBorder(BorderFactory.createTitledBorder("👁 읽은 사람"));
        readStatusArea = new JTextArea();
        readStatusArea.setEditable(false);
        readStatusArea.setFont(new Font("Dialog", Font.PLAIN, 11));
        JScrollPane readScroll = new JScrollPane(readStatusArea);
        readPanel.add(readScroll, BorderLayout.CENTER);
        rightPanel.add(readPanel, BorderLayout.CENTER);

        add(rightPanel, BorderLayout.EAST);

        // === 이벤트 리스너 ===

        // 뒤로 가기
        backButton.addActionListener(e -> {
            socketClient.leaveRoom(currentRoom.getRoomId());
            dispose();
        });

        // 공지 선택
        noticeTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && noticeTable.getSelectedRow() >= 0) {
                postTable.clearSelection();
                Long postId = (Long) noticeTableModel.getValueAt(noticeTable.getSelectedRow(), 0);
                loadPostDetail(postId);
            }
        });

        // 게시글 선택
        postTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && postTable.getSelectedRow() >= 0) {
                noticeTable.clearSelection();
                Long postId = (Long) postTableModel.getValueAt(postTable.getSelectedRow(), 0);
                loadPostDetail(postId);
            }
        });

        // 글쓰기
        writePostButton.addActionListener(e -> handleWritePost());

        // 삭제
        deletePostButton.addActionListener(e -> handleDeletePost());

        // 새로고침
        refreshButton.addActionListener(e -> {
            loadNotices();
            loadGeneralPosts();
        });

        // 댓글 Enter 키로 작성
        commentField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleWriteComment();
                }
            }
        });

        // 댓글 버튼으로 작성
        sendCommentButton.addActionListener(e -> handleWriteComment());

        // 과제 버튼
        assignmentActionButton.addActionListener(e -> handleAssignmentAction());

        // 창 닫기 처리
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                socketClient.leaveRoom(currentRoom.getRoomId());
                if (messageListener != null) {
                    socketClient.removeMessageListener(messageListener);
                }
                dispose();
            }
        });
    }

    // 실시간 리스너 설정
    private void setupRealtimeListener() {
        messageListener = message -> {
            SwingUtilities.invokeLater(() -> {
                try {
                    if (message.startsWith("NEW_POST:" + currentRoom.getRoomId())) {
                        // 새 게시글 알림
                        loadNotices();
                        loadGeneralPosts();
                    } else if (message.startsWith("POST_DELETED:" + currentRoom.getRoomId())) {
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
                    } else if (message.startsWith("ROOM_JOIN:" + currentRoom.getRoomId())) {
                        String[] parts = message.split(":");
                        if (parts.length >= 4) {
                            String joinerName = parts[3];
                            roomUsersArea.append(joinerName + " 님이 입장했습니다.\n");
                        }
                    } else if (message.startsWith("ROOM_LEAVE:" + currentRoom.getRoomId())) {
                        String[] parts = message.split(":");
                        if (parts.length >= 4) {
                            String leaverName = parts[3];
                            roomUsersArea.append(leaverName + " 님이 퇴장했습니다.\n");
                        }
                    } else if (message.startsWith("ROOM_USERLIST:" + currentRoom.getRoomId())) {
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
        // ROOM_USERLIST:roomId:userId:nickname,userId:nickname,...
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

    // 공지사항 로드
    private void loadNotices() {
        noticeTableModel.setRowCount(0);
        try {
            List<Post> notices = postService.findNoticesByRoomId(currentRoom.getRoomId());
            for (Post post : notices) {
                String authorName = userService.findById(post.getAuthorId())
                        .map(User::getNickname).orElse("알 수 없음");
                Object[] row = {
                        post.getPostId(),
                        post.getTitle(),
                        authorName
                };
                noticeTableModel.addRow(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 일반 게시글 로드
    private void loadGeneralPosts() {
        postTableModel.setRowCount(0);
        try {
            List<Post> posts = postService.findGeneralPostsByRoomId(currentRoom.getRoomId());
            for (Post post : posts) {
                String authorName = userService.findById(post.getAuthorId())
                        .map(User::getNickname).orElse("알 수 없음");
                Object[] row = {
                        post.getPostId(),
                        post.getTitle(),
                        authorName,
                        post.getHasAssignment() ? "📋" : ""
                };
                postTableModel.addRow(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 게시글 상세 로드
    private void loadPostDetail(Long postId) {
        this.selectedPostId = postId;
        try {
            Post post = postService.findById(postId).orElse(null);
            if (post != null) {
                this.selectedPostAuthorId = post.getAuthorId();
                this.selectedPostHasAssignment = post.getHasAssignment();

                String authorName = userService.findById(post.getAuthorId())
                        .map(User::getNickname).orElse("알 수 없음");

                StringBuilder sb = new StringBuilder();
                sb.append("제목: ").append(post.getTitle()).append("\n");
                sb.append("작성자: ").append(authorName).append("\n");
                sb.append("작성일: ").append(post.getCreatedAt()).append("\n");
                sb.append("유형: ").append(post.getPostType() == Post.PostType.NOTICE ? "공지사항" : "일반").append("\n");
                sb.append("\n").append(post.getContent());

                postContentArea.setText(sb.toString());
                postContentArea.setCaretPosition(0);

                loadComments(postId);
                loadReadStatus(postId);
                markAsRead(postId);

                // 과제 패널 설정
                if (post.getHasAssignment()) {
                    loadAssignmentInfo(postId);
                } else {
                    assignmentPanel.setVisible(false);
                }
            }
        } catch (Exception e) {
            postContentArea.setText("게시글을 불러올 수 없습니다: " + e.getMessage());
        }
    }

    // 과제 정보 로드
    private void loadAssignmentInfo(Long postId) {
        try {
            String response = socketClient.getAssignment(postId);
            if (response != null && response.contains("SUCCESS")) {
                String[] parts = response.split("\\|");
                if (parts.length >= 6) {
                    String assignTitle = socketClient.decodeText(parts[2]);
                    String dueDate = parts[4].isEmpty() ? "없음" : parts[4];

                    assignmentLabel.setText("<html><b>" + assignTitle + "</b><br>마감: " + dueDate + "</html>");

                    // 작성자면 관리, 아니면 제출
                    if (selectedPostAuthorId.equals(currentUser.getUserId())) {
                        assignmentActionButton.setText("제출물 관리");
                    } else {
                        // 이미 제출했는지 확인
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

    // 댓글 로드
    private void loadComments(Long postId) {
        commentArea.setText("");
        try {
            List<Comment> comments = commentService.findByPostId(postId);
            if (comments.isEmpty()) {
                commentArea.setText("댓글이 없습니다.");
            } else {
                for (Comment comment : comments) {
                    String authorName = userService.findById(comment.getAuthorId())
                            .map(User::getNickname).orElse("알 수 없음");
                    commentArea.append("[" + authorName + "] " + comment.getCreatedAt() + "\n");
                    commentArea.append(comment.getContent() + "\n\n");
                }
            }
        } catch (Exception e) {
            commentArea.setText("댓글 로드 실패: " + e.getMessage());
        }
    }

    // 읽음 상태 로드
    private void loadReadStatus(Long postId) {
        readStatusArea.setText("");
        try {
            List<PostReadStatus> statuses = postReadStatusService.getReadStatusByPost(postId);
            readStatusArea.append("총 " + statuses.size() + "명\n\n");
            for (PostReadStatus status : statuses) {
                String userName = userService.findById(status.getUserId())
                        .map(User::getNickname).orElse("알 수 없음");
                readStatusArea.append("• " + userName + "\n");
            }
        } catch (Exception e) {
            readStatusArea.setText("로드 실패");
        }
    }

    // 읽음 처리
    private void markAsRead(Long postId) {
        try {
            if (!postReadStatusService.hasRead(postId, currentUser.getUserId())) {
                postReadStatusService.markAsRead(postId, currentUser.getUserId());
                socketClient.sendPostRead(postId);
                loadReadStatus(postId);
            }
        } catch (Exception e) {
            // 이미 읽은 경우 무시
        }
    }

    // 글쓰기
    private void handleWritePost() {
        boolean isCreator = currentRoom.getCreatorId().equals(currentUser.getUserId());
        PostWriteDialog dialog = new PostWriteDialog(this, isCreator);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            try {
                Post.PostType postType = dialog.getPostType().equals("NOTICE") ?
                        Post.PostType.NOTICE : Post.PostType.GENERAL;

                Post post = postService.createPost(
                        currentRoom.getRoomId(),
                        currentUser.getUserId(),
                        dialog.getTitle(),
                        dialog.getContent(),
                        postType
                );

                // 과제 생성
                if (dialog.hasAssignment()) {
                    socketClient.createAssignment(
                            post.getPostId(),
                            dialog.getAssignmentTitle(),
                            dialog.getAssignmentDescription(),
                            dialog.getDueDate()
                    );
                }

                loadNotices();
                loadGeneralPosts();
                JOptionPane.showMessageDialog(this, "게시글이 작성되었습니다.", "성공", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "작성 실패: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 삭제
    private void handleDeletePost() {
        if (selectedPostId == null) {
            JOptionPane.showMessageDialog(this, "삭제할 게시글을 선택하세요.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "정말로 삭제하시겠습니까?", "삭제 확인", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                postService.deletePost(selectedPostId, currentUser.getUserId());
                loadNotices();
                loadGeneralPosts();
                postContentArea.setText("");
                commentArea.setText("");
                readStatusArea.setText("");
                assignmentPanel.setVisible(false);
                selectedPostId = null;
                JOptionPane.showMessageDialog(this, "삭제되었습니다.", "성공", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "삭제 실패: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 댓글 작성
    private void handleWriteComment() {
        if (selectedPostId == null) {
            JOptionPane.showMessageDialog(this, "게시글을 먼저 선택하세요.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String content = commentField.getText().trim();
        if (content.isEmpty()) {
            return;
        }

        try {
            commentService.createComment(selectedPostId, currentUser.getUserId(), content);
            commentField.setText("");
            loadComments(selectedPostId);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "댓글 작성 실패: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 과제 액션
    private void handleAssignmentAction() {
        if (selectedPostId == null) return;

        try {
            String response = socketClient.getAssignment(selectedPostId);
            if (response == null || !response.contains("SUCCESS")) return;

            String[] parts = response.split("\\|");
            Long assignmentId = Long.parseLong(parts[1]);
            String assignTitle = socketClient.decodeText(parts[2]);
            String assignDesc = socketClient.decodeText(parts[3]);

            if (selectedPostAuthorId.equals(currentUser.getUserId())) {
                // 작성자: 제출물 관리
                AssignmentManageDialog dialog = new AssignmentManageDialog(this, socketClient, assignmentId, assignTitle);
                dialog.setVisible(true);
            } else {
                // 일반 사용자: 제출
                String subResponse = socketClient.getMySubmission(assignmentId);
                if (subResponse != null && subResponse.contains("SUCCESS")) {
                    // 이미 제출함 - 확인
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
                    // 제출하지 않음 - 제출 다이얼로그
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
                            JOptionPane.showMessageDialog(this, "과제가 제출되었습니다.", "성공", JOptionPane.INFORMATION_MESSAGE);
                            loadAssignmentInfo(selectedPostId);
                        } else {
                            JOptionPane.showMessageDialog(this, "제출 실패", "오류", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "오류: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }
}
