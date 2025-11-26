package org.study.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.study.platform.client.ClientLoginFrame;
import org.study.platform.client.LoginFrame;
import org.study.platform.service.UserService;
import org.study.platform.service.RoomService;
import org.study.platform.socket.SocketServer;

import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;

@SpringBootApplication
public class StudyplatformApplication {

    public static void main(String[] args) {
        // 실행 모드 선택
        String mode = showModeSelectionDialog();

        if (mode == null) {
            System.exit(0);
            return;
        }

        if (mode.equals("SERVER")) {
            startServerMode(args);
        } else {
            startClientMode(args);
        }
    }

    // 모드 선택 다이얼로그
    private static String showModeSelectionDialog() {
        String[] options = {"서버 모드", "클라이언트 모드"};
        int choice = JOptionPane.showOptionDialog(
                null,
                "실행 모드를 선택하세요\n\n서버 모드: DB + Socket Server + GUI\n클라이언트 모드: GUI만 (소켓 통신)",
                "스터디 플랫폼",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == 0) return "SERVER";
        if (choice == 1) return "CLIENT";
        return null;
    }

    // 서버 모드 시작
    private static void startServerMode(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(StudyplatformApplication.class, args);

        // ApplicationContext 전달하여 SocketServer 생성
        SocketServer socketServer = new SocketServer(context);
        socketServer.start();

        SwingUtilities.invokeLater(() -> {
            UserService userService = context.getBean(UserService.class);
            RoomService roomService = context.getBean(RoomService.class);

            LoginFrame loginFrame = new LoginFrame(userService, roomService, context);
            loginFrame.setTitle("스터디 플랫폼 - 서버");
            loginFrame.setVisible(true);
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("서버 종료 중...");
            socketServer.stop();
        }));
    }

    private static void startClientMode(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // 서버 IP 입력
            String serverIp = JOptionPane.showInputDialog(
                    null,
                    "서버 IP를 입력하세요:",
                    "클라이언트 모드",
                    JOptionPane.QUESTION_MESSAGE
            );

            if (serverIp == null || serverIp.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "서버 IP를 입력해야 합니다.");
                System.exit(0);
                return;
            }

            // ClientLoginFrame 생성
            ClientLoginFrame loginFrame = new ClientLoginFrame(serverIp.trim());
            loginFrame.setVisible(true);
        });
    }
}