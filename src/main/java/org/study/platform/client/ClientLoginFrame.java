package org.study.platform.client;

import javax.swing.*;
import java.awt.*;

public class ClientLoginFrame extends JFrame {

    private String serverIp;
    private SocketClient socketClient;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField nicknameField;
    private JButton loginButton;
    private JButton registerButton;

    public ClientLoginFrame(String serverIp) {
        this.serverIp = serverIp;
        this.socketClient = new SocketClient(serverIp);

        initComponents();
    }

    private void initComponents() {
        setTitle("스터디 플랫폼 - 클라이언트 (" + serverIp + ")");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 서버 정보 라벨
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel serverLabel = new JLabel("서버: " + serverIp, JLabel.CENTER);
        serverLabel.setFont(new Font("Dialog", Font.BOLD, 14));
        mainPanel.add(serverLabel, gbc);

        gbc.gridwidth = 1;

        // 사용자명 라벨
        gbc.gridx = 0;
        gbc.gridy = 1;
        mainPanel.add(new JLabel("사용자명:"), gbc);

        // 사용자명 입력
        usernameField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 1;
        mainPanel.add(usernameField, gbc);

        // 비밀번호 라벨
        gbc.gridx = 0;
        gbc.gridy = 2;
        mainPanel.add(new JLabel("비밀번호:"), gbc);

        // 비밀번호 입력
        passwordField = new JPasswordField(20);
        gbc.gridx = 1;
        gbc.gridy = 2;
        mainPanel.add(passwordField, gbc);

        // 닉네임 라벨
        gbc.gridx = 0;
        gbc.gridy = 3;
        mainPanel.add(new JLabel("닉네임:"), gbc);

        // 닉네임 입력
        nicknameField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 3;
        mainPanel.add(nicknameField, gbc);

        // 버튼 패널
        JPanel buttonPanel = new JPanel();
        loginButton = new JButton("로그인");
        registerButton = new JButton("회원가입");

        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        mainPanel.add(buttonPanel, gbc);

        add(mainPanel);

        // 이벤트 리스너
        loginButton.addActionListener(e -> handleLogin());
        registerButton.addActionListener(e -> handleRegister());
        passwordField.addActionListener(e -> handleLogin());

        // macOS 닫기 버튼
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (socketClient != null) {
                    socketClient.disconnect();
                }
                System.exit(0);
            }
        });
    }

    // 로그인 처리
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "사용자명과 비밀번호를 입력하세요.",
                    "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!socketClient.isConnected()) {
            if (!socketClient.startConnection()) {
                JOptionPane.showMessageDialog(this, "서버에 연결할 수 없습니다.\nIP를 확인하세요.",
                        "연결 오류", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        try {
            // 소켓으로 로그인 요청
            String response = socketClient.login(username, password);

            if (response == null) {
                JOptionPane.showMessageDialog(this, "서버 응답 없음. 서버 연결을 확인하세요.",
                        "연결 오류", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String[] parts = response.split("\\|");

            if (parts.length >= 2 && parts[1].equals("SUCCESS")) {
                // 로그인 성공
                Long userId = Long.parseLong(parts[2]);
                String nickname = parts[3];

                // AUTH 메시지로 소켓 연결 활성화
                if (socketClient.authenticate(userId, nickname)) {
                    // 메인 화면으로 전환
                    openMainFrame(userId, nickname);
                } else {
                    JOptionPane.showMessageDialog(this, "서버 연결 실패",
                            "연결 오류", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                // 로그인 실패
                String errorMsg = parts.length >= 3 ? parts[2] : "로그인 실패";
                JOptionPane.showMessageDialog(this, errorMsg,
                        "로그인 실패", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "오류: " + ex.getMessage(),
                    "오류", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    // 회원가입 처리
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String nickname = nicknameField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || nickname.isEmpty()) {
            JOptionPane.showMessageDialog(this, "모든 정보를 입력하세요.",
                    "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!socketClient.isConnected()) {
            if (!socketClient.startConnection()) {
                JOptionPane.showMessageDialog(this, "서버에 연결할 수 없습니다.\nIP를 확인하세요.",
                        "연결 오류", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        try {
            // 소켓으로 회원가입 요청
            String response = socketClient.register(username, password, nickname);

            if (response == null) {
                JOptionPane.showMessageDialog(this, "서버 응답 없음. 서버 연결을 확인하세요.",
                        "연결 오류", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String[] parts = response.split("\\|");

            if (parts.length >= 2 && parts[1].equals("SUCCESS")) {
                // 회원가입 성공
                JOptionPane.showMessageDialog(this, "회원가입이 완료되었습니다. 로그인하세요.",
                        "회원가입 성공", JOptionPane.INFORMATION_MESSAGE);

                nicknameField.setText("");
                passwordField.setText("");
                usernameField.requestFocus();
            } else {
                // 회원가입 실패
                String errorMsg = parts.length >= 3 ? parts[2] : "회원가입 실패";
                JOptionPane.showMessageDialog(this, errorMsg,
                        "회원가입 실패", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "오류: " + ex.getMessage(),
                    "오류", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    // 메인 화면 열기
    private void openMainFrame(Long userId, String nickname) {
        this.dispose();

        SwingUtilities.invokeLater(() -> {
            ClientMainFrame mainFrame = new ClientMainFrame(socketClient, userId, nickname);
            mainFrame.setVisible(true);
        });
    }
}