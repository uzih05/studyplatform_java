package org.study.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.study.platform.client.LoginFrame;
import org.study.platform.service.UserService;
import org.study.platform.service.RoomService;
import org.study.platform.socket.SocketServer;

import javax.swing.SwingUtilities;

@SpringBootApplication
public class StudyplatformApplication {

    public static void main(String[] args) {
        // Spring Boot 컨텍스트 시작
        ConfigurableApplicationContext context = SpringApplication.run(StudyplatformApplication.class, args);

        // 소켓 서버 시작
        SocketServer socketServer = new SocketServer();
        socketServer.start();

        // Swing GUI를 Event Dispatch Thread에서 실행
        SwingUtilities.invokeLater(() -> {
            // Spring에서 Service들 가져오기
            UserService userService = context.getBean(UserService.class);
            RoomService roomService = context.getBean(RoomService.class);

            // LoginFrame 생성
            LoginFrame loginFrame = new LoginFrame(userService, roomService, context);
            loginFrame.setVisible(true);
        });

        // 애플리케이션 종료 시 소켓 서버도 종료
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("애플리케이션 종료 중...");
            socketServer.stop();
        }));
    }

}