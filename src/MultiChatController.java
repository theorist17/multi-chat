import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.logging.*;
import com.google.gson.*;
import static java.util.logging.Level.*;
 
public class MultiChatController implements Runnable{
 
    // 뷰 클래스 참조 객체
    private final MultiChatUI v;
 
    // 데이터 클래스 참조 객체
    private final MultiChatData chatData;
 
    // 소켓 연결을 위한 변수 선언
    private String ip = "203.252.148.148";
    private Socket socket;
    private BufferedReader inMsg = null;
    private PrintWriter outMsg = null;
 
    // 메시지 파싱을 위한 객체 생성
    Gson gson = new Gson();
    Message m;
 
    // 상태 플래그
    boolean status;
 
    // 로거 객체
    Logger logger;
 
    // 메시지 수신 스레드
    Thread thread;
 
    /**
     * 모델과 뷰 객체를 파라미터로 하는 생성자
     * @param chatData
     * @param v
     */
    public MultiChatController(MultiChatData chatData, MultiChatUI v) {
        // 로거 객체 초기화
        logger = Logger.getLogger(this.getClass().getName());
 
        // 모델과 뷰 클래스 참조
        this.chatData = chatData;
        this.v = v;
    }
 
    /**
     * 어플리케이션 메인 실행 메서드
     */
    public void appMain() {
        // 데이터 객체에서 데이터 변화를 처리할 UI 객체 추가
        chatData.addObj(v.msgOut);
 
        v.addButtonActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object obj = e.getSource();
 
                // 종료버튼 처리
                if (obj == v.exitButton) {
                    System.exit(0);
                } else if (obj == v.loginButton) {
                    v.id = v.idInput.getText();
                    v.outLabel.setText(" 대화명 : " + v.id);
                    v.cardLayout.show(v.tab, "logout");
                    connectServer();
                } else if (obj == v.logoutButton) {
                    // 로그아웃 메시지 전송
                    outMsg.println(gson.toJson(new Message(v.id, "", "", "logout")));
                    // 대화창 클리어
                    v.msgOut.setText("");
                    // 로그인 패널로 전환
                    v.cardLayout.show(v.tab, "login");
                    outMsg.close();
                    try {
                        inMsg.close();
                        socket.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    status = false;
                } else if (obj == v.msgInput) {
                    // 메시지 전송
                    outMsg.println(gson.toJson(new Message(v.id, "", v.msgInput.getText(), "msg")));
                    // 입력창 클리어
                    v.msgInput.setText("");
                }
            }
        });
    }
 
    /**
     * 서버 접속을 위한 메서드
     */
    public void connectServer() {
        try {
            // 소켓 생성
            socket = new Socket(ip,8888);
            logger.log(INFO,"[Client]Server 연결 성공!!");
 
            // 입출력 스트림 생성
            inMsg = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outMsg = new PrintWriter(socket.getOutputStream(),true);
 
            // 서버에 로그인 메시지 전달
            m = new Message(v.id,"","","login");
            outMsg.println(gson.toJson(m));
 
            // 메시지 수신을 위한 스레드 생성
            thread = new Thread(this);
            thread.start();
        }
        catch(Exception e) {
            logger.log(WARNING, "[MultiChatUI]connectServer() Exception 발생!!");
            e.printStackTrace();
        }
    }
 
    /**
     * 메시지 수신을 독립적으로 처리하기 위한 스레드 실행
     */
    public void run() {
        // 수신 메시지 처리를 위한 변수
        String msg;
 
        status=true;
 
        while(status) {
            try{
                // 메시지 수신 및 파싱
                msg = inMsg.readLine();
                m = gson.fromJson(msg, Message.class);
 
                // MultiChatData 객체를 통해 데이터 갱신
                chatData.refreshData(m.getId() + ">" + m.getMsg() + "\n");
 
                // 커서를 현재 대화 메시지에 보여줌
                v.msgOut.setCaretPosition(v.msgOut.getDocument().getLength());
            }
            catch(IOException e) {
                logger.log(WARNING,"[MultiChatUI]메시지 스트림 종료!!");
            }
        }
        logger.info("[MultiChatUI]" + thread.getName()+ " 메시지 수신 스레드 종료됨!!");
    }
 
 
    // 프로그램 시작을 위한 메인 메서드
    public static void main(String[] args) {
        MultiChatController app = new MultiChatController(new MultiChatData(),new MultiChatUI());
        app.appMain();
    }
}