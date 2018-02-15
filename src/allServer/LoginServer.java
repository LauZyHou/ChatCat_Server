package allServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

//[登陆请求管理线程],3838端口管理登录请求
public class LoginServer implements Runnable {
	ServerSocket ss = null;// 服务端Socket,用来建立Socket对象
	Socket sckt = null;// Socket连接对象

	@Override
	public void run() {
		// 在这个[登陆请求管理线程]中,不停地监听客户端发来的登录请求
		try {
			ss = new ServerSocket(3838); // 3838端口
			System.out.println("[#]开始等待客户端连接...");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		while (true) {
			try {
				sckt = ss.accept();// 阻塞以等待连接
				System.out.println("[+]客户端地址:" + sckt.getInetAddress());
				// 为这个新客户建立[登陆请求处理线程]对象,启动专为这个客户服务的线程
				new IWannaLogin(sckt).start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}// end run()
}

// [登陆请求处理线程]子线程,每个客户单独一个
class IWannaLogin extends Thread {
	// 从构造器传入的Socket连接对象
	Socket sckt;
	// 数据输入输出流
	DataInputStream dis;
	DataOutputStream dos;
	// 布尔值指示是否登录成功,初始为否
	boolean success = false;

	// 构造器
	IWannaLogin(Socket sckt) {
		this.sckt = sckt;
	}

	@Override
	public void run() {
		try {
			// 用Socket连接对象去初始化输入输出流,将其与客户端输出输入连接
			this.dis = new DataInputStream(sckt.getInputStream());
			this.dos = new DataOutputStream(sckt.getOutputStream());
			// 没能输入正确的账密登录成功时,不停接收(用户可以不断尝试)
			while (success == false) {
				// 从输入流读入用户发来的消息
				String str = dis.readUTF();// 阻塞发生之处
				// 确保其消息格式,以"[login]"开头才是要登录
				if (str.startsWith("[login]") == false)
					return;// 如果对方Socket发来的消息格式不对,这个连接就是危险的,直接结束该线程
				// System.out.println(str);
				// if (str.endsWith("3838438")) {
				String nm = str.substring(str.indexOf("]") + 1, str.indexOf("#"));
				String pswd = str.substring(str.indexOf("#") + 1);
				System.out.println(nm + " " + pswd);
				success = true;
				// }
			}
		} catch (IOException e) {
			System.out.println("[-]放弃登录来自:" + sckt.getInetAddress());
		}
	}
}
