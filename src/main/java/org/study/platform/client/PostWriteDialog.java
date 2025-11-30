package org.study.platform.client;

import javax.swing.*;
import java.awt.*;

public class PostWriteDialog extends JDialog {

    private JTextField titleField;
    private JTextArea contentArea;
    private JComboBox<String> postTypeCombo;
    private JCheckBox assignmentCheckBox;
    private JTextField assignmentTitleField;
    private JTextArea assignmentDescArea;
    private JTextField dueDateField;
    private JPanel assignmentPanel;

    private boolean confirmed = false;
    private boolean isRoomCreator;

    public PostWriteDialog(JFrame parent, boolean isRoomCreator) {
        super(parent, "게시글 작성", true);
        this.isRoomCreator = isRoomCreator;
        initComponents();
    }

    private void initComponents() {
        setSize(600, 700);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 게시글 타입 선택
        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        typePanel.add(new JLabel("게시글 유형:"));
        if (isRoomCreator) {
            postTypeCombo = new JComboBox<>(new String[]{"일반 게시글", "공지사항"});
        } else {
            postTypeCombo = new JComboBox<>(new String[]{"일반 게시글"});
        }
        typePanel.add(postTypeCombo);
        typePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        mainPanel.add(typePanel);

        mainPanel.add(Box.createVerticalStrut(10));

        // 제목 입력
        JPanel titlePanel = new JPanel(new BorderLayout(5, 5));
        titlePanel.add(new JLabel("제목"), BorderLayout.NORTH);
        titleField = new JTextField();
        titleField.setFont(new Font("Dialog", Font.PLAIN, 14));
        titlePanel.add(titleField, BorderLayout.CENTER);
        titlePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        mainPanel.add(titlePanel);

        mainPanel.add(Box.createVerticalStrut(15));

        // 본문 입력
        JPanel contentPanel = new JPanel(new BorderLayout(5, 5));
        contentPanel.add(new JLabel("본문"), BorderLayout.NORTH);
        contentArea = new JTextArea(12, 40);
        contentArea.setFont(new Font("Dialog", Font.PLAIN, 14));
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        JScrollPane contentScroll = new JScrollPane(contentArea);
        contentPanel.add(contentScroll, BorderLayout.CENTER);
        mainPanel.add(contentPanel);

        mainPanel.add(Box.createVerticalStrut(15));

        // 과제 설정 체크박스
        assignmentCheckBox = new JCheckBox("이 게시글에 과제 추가");
        assignmentCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(assignmentCheckBox);

        mainPanel.add(Box.createVerticalStrut(10));

        // 과제 설정 패널
        assignmentPanel = new JPanel();
        assignmentPanel.setLayout(new BoxLayout(assignmentPanel, BoxLayout.Y_AXIS));
        assignmentPanel.setBorder(BorderFactory.createTitledBorder("과제 설정"));
        assignmentPanel.setVisible(false);

        JPanel assignTitlePanel = new JPanel(new BorderLayout(5, 5));
        assignTitlePanel.add(new JLabel("과제 제목"), BorderLayout.NORTH);
        assignmentTitleField = new JTextField();
        assignTitlePanel.add(assignmentTitleField, BorderLayout.CENTER);
        assignTitlePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        assignmentPanel.add(assignTitlePanel);

        assignmentPanel.add(Box.createVerticalStrut(10));

        JPanel assignDescPanel = new JPanel(new BorderLayout(5, 5));
        assignDescPanel.add(new JLabel("과제 설명"), BorderLayout.NORTH);
        assignmentDescArea = new JTextArea(4, 30);
        assignmentDescArea.setLineWrap(true);
        assignmentDescArea.setWrapStyleWord(true);
        JScrollPane assignDescScroll = new JScrollPane(assignmentDescArea);
        assignDescPanel.add(assignDescScroll, BorderLayout.CENTER);
        assignmentPanel.add(assignDescPanel);

        assignmentPanel.add(Box.createVerticalStrut(10));

        JPanel dueDatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dueDatePanel.add(new JLabel("마감일 (선택, 형식: 2025-01-01T23:59):"));
        dueDateField = new JTextField(16);
        dueDatePanel.add(dueDateField);
        dueDatePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        assignmentPanel.add(dueDatePanel);

        mainPanel.add(assignmentPanel);

        // 과제 체크박스 이벤트
        assignmentCheckBox.addActionListener(e -> {
            assignmentPanel.setVisible(assignmentCheckBox.isSelected());
            pack();
            setSize(600, assignmentCheckBox.isSelected() ? 850 : 700);
        });

        add(mainPanel, BorderLayout.CENTER);

        // 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("취소");
        JButton confirmButton = new JButton("작성");

        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        confirmButton.addActionListener(e -> {
            if (titleField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "제목을 입력하세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (contentArea.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "본문을 입력하세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (assignmentCheckBox.isSelected() && assignmentTitleField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "과제 제목을 입력하세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
                return;
            }
            confirmed = true;
            dispose();
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(confirmButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getTitle() {
        return titleField.getText().trim();
    }

    public String getContent() {
        return contentArea.getText().trim();
    }

    public String getPostType() {
        return postTypeCombo.getSelectedIndex() == 0 ? "GENERAL" : "NOTICE";
    }

    public boolean hasAssignment() {
        return assignmentCheckBox.isSelected();
    }

    public String getAssignmentTitle() {
        return assignmentTitleField.getText().trim();
    }

    public String getAssignmentDescription() {
        return assignmentDescArea.getText().trim();
    }

    public String getDueDate() {
        String date = dueDateField.getText().trim();
        return date.isEmpty() ? null : date;
    }
}
