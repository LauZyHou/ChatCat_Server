package allServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

//[登陆请求管理线程],3838端口管理登录请求
public class LoginServer implements Runnable {
	ServerSocket ss = null;// 服务端Socket,用来建立Socket对象
	Socket sckt = null;// Socket连接对象

	@Override
	public void run() {
		// 准备工作:加载数据库驱动
		try {
			// JVM加载JDBC驱动
			Class.forName("com.mysql.jdbc.Driver");
			// sql连接对象
			java.sql.Connection con;
			// PreparedStatement是Statement的子类,也是一种sql语句对象
			java.sql.PreparedStatement ps;
			// 结果集
			ResultSet rs;
			// 连接信息字符串,MySQL数据库服务器的默认端口号是3306
			// jdbc:mysql://ip地址:端口号/要连接的数据库名?其它选项
			// useSSL参数指明数据通道是否要加密处理
			// characterEncoding参数指明连接编码,要和数据库编码,数据库表的编码,数据库系统的编码一致
			String uri = "jdbc:mysql://192.168.0.106:3306/ChatCatDB?useSSL=true&characterEncoding=utf8";
			String user = "root";// 用户名
			String password = "3838438"; // 密码
			// 和指定的数据库建立连接(连接信息字符串,用户名,密码)
			con = DriverManager.getConnection(uri, user, password);
			// 使用PreparedStatement时,其构造方法中直接传入要执行的sql语句,暂时不确定值的地方使用问号
			// 所以execute(),executeQuery()和executeUpdate()不再需要参数
			ps = con.prepareStatement("SELECT * FROM SmplMsg WHERE UsrNum=? AND Passwd=?");
			// 服务器端Socket,用来在后面循环中建立Socket对象
			ss = new ServerSocket(3838); // 登录服务始终使用3838端口,不必写入循环体中
			System.out.println("[#]开始等待客户端连接...");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();// JVM加载JDBC驱动失败
		} catch (SQLException e) {
			e.printStackTrace();// 与sql连接对象和sql语句对象有关的异常
		} catch (IOException e) {
			e.printStackTrace();// ServerSocket以该端口实例化失败
		}
		// 在这个[登陆请求管理线程]中,不停地监听客户端发来的登录请求
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
					return;// 如果对方Socket发来的消息格式不对,这个连接就是错误危险的,直接结束该线程
				// System.out.println(str);
				// if (str.endsWith("3838438")) {
				String nm = str.substring(str.indexOf("]") + 1, str.indexOf("#"));
				String pswd = str.substring(str.indexOf("#") + 1);
				System.out.println(nm + " " + pswd);
				success = true;
				// }
			}
		} catch (IOException e) {
			// 当客户端还没点过登录按钮,就关闭了窗口或者以其它方式强行结束了客户端程序时
			System.out.println("[-]放弃登录来自:" + sckt.getInetAddress());
		}
	}
}
