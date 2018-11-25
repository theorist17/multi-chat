//package javabook.ch12;

//클라이언트와 서버 간의 통신에 사용하는 JSON 규격의 메시지를 좀 더 쉽게 사용하려고 자바 객체로 변환하는 데 필요한 클래스
//참고: JSON(JavaScript Object Notation)은 원래 자바스크립트에서 객체를 표현하려고 만든 구문인데, 
//지금은 인터넷으로 시스템이나 프로그램 간에 데이터를 주고받는 메시지 규격으로 널리 사용됨
//이 프로젝트에서는 구글에서 만든 JSON파서인 Gson을 사용 (관련 라이브러리 추가 방법: ecampus 강의자료 참고)
public class Message {
	private String id;
	private String passwd;
	private String msg;
	private String type;
	
	public Message() {}
	
	public Message(String id, String passwd, String msg, String type) {
		this.id = id;
		this.passwd = passwd;
		this.msg = msg;
		this.type = type;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getPasswd() {
		return passwd;
	}
	public void setPassword(String passwd) {
		this.passwd = passwd;
	}
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
}
