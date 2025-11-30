package org.study.platform.client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class AssignmentManageDialog extends JDialog {

    private SocketClient socketClient;
    private Long assignmentId;

    private JTable submissionTable;
    private DefaultTableModel tableModel;
    private JTextArea contentArea;
    private JTextField scoreField;
    private JTextArea feedbackArea;
    private JButton gradeButton;

    public AssignmentManageDialog(JFrame parent, SocketClient socketClient, Long assignmentId, String assignmentTitle) {
        super(parent, "과제 관리 - " + assignmentTitle, true);
        this.socketClient = socketClient;
        this.assignmentId = assignmentId;
        initComponents();
        loadSubmissions();
    }

    private void initComponents() {
        setSize(800, 600);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout(10, 10));

        // 왼쪽: 제출물 목록
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("제출 목록"));
        leftPanel.setPreferredSize(new Dimension(350, 0));

        String[] columns = {"ID", "제출자", "상태", "점수", "제출일"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        submissionTable = new JTable(tableModel);
        submissionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane tableScroll = new JScrollPane(submissionTable);
        leftPanel.add(tableScroll, BorderLayout.CENTER);

        JButton refreshButton = new JButton("새로고침");
        refreshButton.addActionListener(e -> loadSubmissions());
        leftPanel.add(refreshButton, BorderLayout.SOUTH);

        add(leftPanel, BorderLayout.WEST);

        // 오른쪽: 상세 정보 및 채점
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 제출 내용
        JPanel contentPanel = new JPanel(new BorderLayout(5, 5));
        contentPanel.setBorder(BorderFactory.createTitledBorder("제출 내용"));
        contentArea = new JTextArea(8, 30);
        contentArea.setEditable(false);
        contentArea.setLineWrap(true);
        JScrollPane contentScroll = new JScrollPane(contentArea);
        contentPanel.add(contentScroll, BorderLayout.CENTER);
        rightPanel.add(contentPanel);

        rightPanel.add(Box.createVerticalStrut(15));

        // 채점 패널
        JPanel gradePanel = new JPanel();
        gradePanel.setLayout(new BoxLayout(gradePanel, BoxLayout.Y_AXIS));
        gradePanel.setBorder(BorderFactory.createTitledBorder("채점"));

        JPanel scorePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        scorePanel.add(new JLabel("점수:"));
        scoreField = new JTextField(5);
        scorePanel.add(scoreField);
        scorePanel.add(new JLabel("점"));
        scorePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        gradePanel.add(scorePanel);

        JPanel feedbackPanel = new JPanel(new BorderLayout(5, 5));
        feedbackPanel.add(new JLabel("피드백"), BorderLayout.NORTH);
        feedbackArea = new JTextArea(4, 30);
        feedbackArea.setLineWrap(true);
        JScrollPane feedbackScroll = new JScrollPane(feedbackArea);
        feedbackPanel.add(feedbackScroll, BorderLayout.CENTER);
        gradePanel.add(feedbackPanel);

        gradeButton = new JButton("채점 완료");
        gradeButton.setEnabled(false);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(gradeButton);
        gradePanel.add(buttonPanel);

        rightPanel.add(gradePanel);

        add(rightPanel, BorderLayout.CENTER);

        // 테이블 선택 이벤트
        submissionTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = submissionTable.getSelectedRow();
                if (row >= 0) {
                    loadSubmissionDetail(row);
                    gradeButton.setEnabled(true);
                }
            }
        });

        // 채점 버튼 이벤트
        gradeButton.addActionListener(e -> gradeSelectedSubmission());

        // 닫기 버튼
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = new JButton("닫기");
        closeButton.addActionListener(e -> dispose());
        bottomPanel.add(closeButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void loadSubmissions() {
        tableModel.setRowCount(0);
        try {
            String response = socketClient.getSubmissions(assignmentId);
            if (response == null) return;

            String[] parts = response.split("\\|");
            if (parts.length >= 2 && parts[1].equals("SUCCESS")) {
                for (int i = 2; i < parts.length; i++) {
                    String[] data = parts[i].split(":");
                    if (data.length >= 8) {
                        Object[] row = {
                                Long.parseLong(data[0]),  // submissionId
                                data[2],                   // submitterName
                                data[5],                   // status
                                data[6].isEmpty() ? "-" : data[6],  // score
                                data[7]                    // submittedAt
                        };
                        tableModel.addRow(row);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadSubmissionDetail(int row) {
        try {
            String response = socketClient.getSubmissions(assignmentId);
            if (response == null) return;

            String[] parts = response.split("\\|");
            if (parts.length > row + 2) {
                String[] data = parts[row + 2].split(":");
                if (data.length >= 5) {
                    String content = socketClient.decodeText(data[3]);
                    String fileName = data.length >= 5 && !data[4].isEmpty() ?
                            socketClient.decodeText(data[4]) : "";

                    StringBuilder sb = new StringBuilder();
                    sb.append("제출 내용:\n").append(content);
                    if (!fileName.isEmpty()) {
                        sb.append("\n\n첨부파일: ").append(fileName);
                    }
                    contentArea.setText(sb.toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void gradeSelectedSubmission() {
        int row = submissionTable.getSelectedRow();
        if (row < 0) return;

        Long submissionId = (Long) tableModel.getValueAt(row, 0);

        Integer score = null;
        String scoreText = scoreField.getText().trim();
        if (!scoreText.isEmpty()) {
            try {
                score = Integer.parseInt(scoreText);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "점수는 숫자로 입력하세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        String feedback = feedbackArea.getText().trim();

        try {
            String response = socketClient.gradeSubmission(submissionId, score, feedback);
            if (response != null && response.contains("SUCCESS")) {
                JOptionPane.showMessageDialog(this, "채점이 완료되었습니다.", "성공", JOptionPane.INFORMATION_MESSAGE);
                loadSubmissions();
                scoreField.setText("");
                feedbackArea.setText("");
                contentArea.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "채점에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "오류: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }
}
