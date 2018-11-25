import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.logging.*;
import com.google.gson.*;
import static java.util.logging.Level.*;

//UI와 서버 연결 및 채팅 메시지 전달

public class MultiChatController implements Runnable {

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
	 * 
	 * @param chatData
	 * @param v
	 */
	public MultiChatController(MultiChatData chatData, MultiChatUI v) {
		// 로거 객체 초기화
		logger = Logger.getLogger(this.getClass().getName());

		// MultiChatData, MultiChatUI 객체 초기화
		this.v = v;
		this.chatData = chatData;
	}

	/**
	 * 어플리케이션 메인 실행 메서드 컨트롤러 클래스 메인 로직; UI에서 발생한 이벤트를 위임받아 처리
	 */
	public void appMain() {
		// 데이터 객체(chatData)에서 (채팅 내용 출력창의) 데이터 변화를 처리할 UI 객체 추가
		chatData.addObj(v.msgOut);

		// 데이터 객체에 UI 객체를 추가하고, 버튼들의 이벤트 핸들러를 등록하는
		// addButtonActionListener() 메소드를 호출하면서 리스너 클래스를 익명의 내부 클래스로 만들어 전달
		v.addButtonActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object obj = e.getSource();

				// 종료버튼, 로그인버튼, 로그아웃버튼, 메시지전송버튼(엔터) 처리
				if (obj == v.exitButton) {
					outMsg.println(gson.toJson(new Message(v.id, "", "", "logout")));
					
					try {
						if (socket != null)
							socket.close();
						Thread.sleep(100);
					} catch (IOException e1) {
						logger.warning("[MultiChatController] appMain() Exception 발생!! 1");
						e1.printStackTrace();
					} catch (InterruptedException e1) {
						logger.warning("[MultiChatController] appMain() Exception 발생!! 2");
						e1.printStackTrace();
					}
					System.exit(0);
				} else if (obj == v.loginButton) {
					// 입력한 아이디를 가져와
					String id = v.idInput.getText();
					if(id == null || id.trim().equals("")) {
						logger.warning("[MultiChatController] appMain() Exception 발생!! 2");
						return;
					}
					v.id = id.trim();
					
					// outLabel에 출력하고
					v.outLabel.setText(" 대화명 : " + v.id);

					// 카드 레이아웃을 변경하여 로그인 상태(즉, 로그아웃이 보이게)로 전환
					v.cardLayout.show(v.tab, "logout");

					// 서버에 연결
					connectServer();
				} else if (obj == v.logoutButton) {
					// 로그아웃 메시지 전송
					// 출력 스트림을 이용하여 Message 객체를 생성한 후 JSON으로 변경하여 로그아웃 메시지를 전송
					outMsg.println(gson.toJson(new Message(v.id, "", "", "logout")));

					// 대화창 클리어
					v.msgOut.setText(null);

					// 로그인 패널로 전환 및 소켓/스트림 닫기 + status 업데이트
					v.cardLayout.show(v.tab, "login");
					
					status = false;
					
					if (outMsg != null)
						outMsg.close();
					
//					try {
//						if (inMsg != null)
//							inMsg.close();
//					} catch (IOException e1) {
//						logger.log(WARNING, "[MultiChatController]connectServer() Exception 발생!! (입력스트림 닫기)");
//						e1.printStackTrace();
//					}
//					
					try {
						if (socket != null)
							socket.close();
					} catch (IOException e1) {
						logger.warning("[MultiChatController] appMain() Exception 발생!!");
						e1.printStackTrace();
					}

				} else if (obj == v.msgInput) {
					// 입력된 메시지 전송 (위의 로그아웃 메시지 전송코드와 Message 생성자 참고)
					outMsg.println(gson.toJson(new Message(v.id, "", v.msgInput.getText(), "msg")));

					// 입력창 클리어
					v.msgInput.setText(null);
				}
			}
		});
	}

	/**
	 * 서버 접속을 위한 메서드 (멤버필드를 적극 활용하기 바람; 이 메서드 안에서 새롭게 선언이 필요한 변수는 없음) 채팅 서버 접속을 위한
	 * 메소드 서버와 연결하고 입출력 스트림을 만든 후 메시지 수신에 필요한 스레드 생성 서버와 연결==로그인 이므로 입출력 스트림을 생성한 후
	 * 바로 로그인 메시지 전달 필요
	 */
	public void connectServer() {
		try {
			// 소켓 생성 (ip, port는 임의로 설정하되 나중에 서버에서 듣게될 포트와 동일해야함)
			socket = new Socket(ip, 8888); 
			
			// INFO 레벨 로깅 (서버 연결에 성공했다는 메시지 화면에 출력)
			logger.info("[MultiChatController]connectServer() 서버 연결에 성공했다. ");
			
			// 입출력(inMsg, outMsg) 스트림 생성
			inMsg = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			outMsg = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

			// 서버에 로그인 메시지 전달
			outMsg.println(gson.toJson(new Message(v.id, "", "", "login")));

			// 메시지 수신을 위한 스레드(thread) 생성 및 스타트
			thread = new Thread(this, "Thread-ChatDataRefresher");
			thread.start();
		} catch (Exception e) {
			logger.log(WARNING, "[MultiChatController]connectServer() Exception 발생!!");
			e.printStackTrace();
		}
	}
	
	/**
	 * 메시지 수신을 독립적으로 처리하기 위한 스레드 실행 
	 * 서버 연결 후 메시지 수신을 UI동작과 상관없이 독립적으로 처리하는 스레드를 실행
	 */
	public void run() {
		// 수신 메시지 처리를 위한 변수
		logger.info("[MultiChatController] 메시지 스트림 시작!!");
		String msg;

		// status 업데이트
		status = true;

		while (status) {
			try {
				// 메시지 수신
				msg = inMsg.readLine();
				
				// 메시지 파싱
				m = gson.fromJson(msg, Message.class);
				
				// MultiChatData 객체를 통해 데이터 갱신
				chatData.refreshData(m.getId() + ">" + m.getMsg() + "\n");

				// 커서를 현재 대화 메시지에 보여줌
				v.msgOut.setCaretPosition(v.msgOut.getDocument().getLength());
			} catch (IOException e) {
				logger.log(WARNING, "[MultiChatController] 메시지 스트림 종료!!");
			}
		}
		
		logger.info("[MultiChatController]" + thread.getName() + " 메시지 수신 스레드 종료됨!!");
	}

	// 프로그램 시작을 위한 메인 메서드
	public static void main(String[] args) {
		// MultiChatController 객체생성 및 appMain() 실행
		MultiChatData chatData = new MultiChatData();
		MultiChatUI v = new MultiChatUI();
		MultiChatController chatController = new MultiChatController(chatData, v);
		chatController.appMain();
	}
}
