package org.study.platform.client;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class AssignmentSubmitDialog extends JDialog {

    private JTextArea contentArea;
    private JLabel fileLabel;
    private JButton selectFileButton;
    private JButton removeFileButton;
    private File selectedFile;

    private boolean confirmed = false;

    public AssignmentSubmitDialog(JFrame parent, String assignmentTitle, String assignmentDesc) {
        super(parent, "과제 제출", true);
        initComponents(assignmentTitle, assignmentDesc);
    }

    private void initComponents(String assignmentTitle, String assignmentDesc) {
        setSize(500, 500);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 과제 정보
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("과제 정보"));
        JTextArea infoArea = new JTextArea();
        infoArea.setText("제목: " + assignmentTitle + "\n\n" + assignmentDesc);
        infoArea.setEditable(false);
        infoArea.setBackground(new Color(245, 245, 245));
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        JScrollPane infoScroll = new JScrollPane(infoArea);
        infoScroll.setPreferredSize(new Dimension(450, 100));
        infoPanel.add(infoScroll, BorderLayout.CENTER);
        mainPanel.add(infoPanel);

        mainPanel.add(Box.createVerticalStrut(15));

        // 내용 입력
        JPanel contentPanel = new JPanel(new BorderLayout(5, 5));
        contentPanel.setBorder(BorderFactory.createTitledBorder("제출 내용 (선택)"));
        contentArea = new JTextArea(8, 40);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        JScrollPane contentScroll = new JScrollPane(contentArea);
        contentPanel.add(contentScroll, BorderLayout.CENTER);
        mainPanel.add(contentPanel);

        mainPanel.add(Box.createVerticalStrut(15));

        // 파일 첨부
        JPanel filePanel = new JPanel(new BorderLayout(5, 5));
        filePanel.setBorder(BorderFactory.createTitledBorder("파일 첨부 (선택)"));

        JPanel fileSelectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectFileButton = new JButton("파일 선택");
        removeFileButton = new JButton("제거");
        removeFileButton.setEnabled(false);
        fileLabel = new JLabel("선택된 파일 없음");

        fileSelectPanel.add(selectFileButton);
        fileSelectPanel.add(removeFileButton);
        fileSelectPanel.add(fileLabel);
        filePanel.add(fileSelectPanel, BorderLayout.CENTER);
        mainPanel.add(filePanel);

        // 파일 선택 이벤트
        selectFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("파일 선택");
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
                fileLabel.setText(selectedFile.getName() + " (" + formatFileSize(selectedFile.length()) + ")");
                removeFileButton.setEnabled(true);
            }
        });

        removeFileButton.addActionListener(e -> {
            selectedFile = null;
            fileLabel.setText("선택된 파일 없음");
            removeFileButton.setEnabled(false);
        });

        add(mainPanel, BorderLayout.CENTER);

        // 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("취소");
        JButton submitButton = new JButton("제출");

        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        submitButton.addActionListener(e -> {
            if (contentArea.getText().trim().isEmpty() && selectedFile == null) {
                JOptionPane.showMessageDialog(this, "내용 또는 파일 중 하나는 제출해야 합니다.",
                        "입력 오류", JOptionPane.WARNING_MESSAGE);
                return;
            }
            confirmed = true;
            dispose();
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(submitButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024.0));
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getContent() {
        return contentArea.getText().trim();
    }

    public File getSelectedFile() {
        return selectedFile;
    }
}
