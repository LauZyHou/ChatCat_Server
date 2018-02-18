package allServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

//[注册请求管理线程],3939端口管理注册请求
public class SignServer implements Runnable {
	// 服务端Socket,用来建立Socket对象
	ServerSocket ss = null;
	// Socket连接对象
	Socket sckt = null;
	// sql连接对象
	java.sql.Connection con;
	// PreparedStatement是Statement的子类,也是一种sql语句对象
	java.sql.PreparedStatement ps_smpl;// 查询SmplMsg表,用于确保账号不重复
	java.sql.PreparedStatement ps_insrt;// 插入SmplMsg表,用于成功注册

	// 构造器
	SignServer() {
		// 准备工作:加载MySQL驱动,准备sql语句对象,准备绑定端口的ServerSocket对象
		try {
			// JVM加载JDBC驱动
			Class.forName("com.mysql.jdbc.Driver");
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
			ps_smpl = con.prepareStatement("SELECT * FROM SmplMsg WHERE UsrNum=?");// 检查
			ps_insrt = con.prepareStatement("INSERT INTO SmplMsg VALUES (?,?,?,?)");// 插入
			// 服务器端Socket,用来在后面循环中建立Socket对象
			ss = new ServerSocket(3939); // 注册服务使用3939端口
			System.out.println("[注册请求管理线程]启动...");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();// JVM加载JDBC驱动失败
		} catch (SQLException e) {
			e.printStackTrace();// 与sql连接对象和sql语句对象有关的异常
		} catch (IOException e) {
			e.printStackTrace();// ServerSocket以该端口实例化失败
		}
	}

	@Override
	public void run() {
		// 在这个[注册请求管理线程]中,不停地监听客户端发来的注册请求
		while (true) {
			try {
				sckt = ss.accept();// 阻塞以等待连接
				System.out.println("[+]申请注册来自:" + sckt.getInetAddress());
				// 为这个新客户建立[注册请求处理线程]对象,启动专为这个客户服务的线程
				new IWannaSignUp(sckt, ps_smpl, ps_insrt).start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}

// [注册请求处理线程],每个注册者单独一个
class IWannaSignUp extends Thread {
	// 从构造器传入的Socket连接对象
	Socket sckt;
	// 数据输入输出流
	DataInputStream dis;
	DataOutputStream dos;
	// 布尔值指示是否注册成功,初始为否
	boolean success = false;
	// 备好的sql语句对象
	java.sql.PreparedStatement ps_smpl;// 查防重复
	java.sql.PreparedStatement ps_insrt;// 成功注册时插入数据库
	// 查询返回的结果集,每次查询都在给它赋值
	ResultSet rs;

	// 构造器(Socket连接对象,PreparedStatement备好的sql语句对象)
	IWannaSignUp(Socket sckt, java.sql.PreparedStatement ps_smpl, java.sql.PreparedStatement ps_insrt) {
		this.sckt = sckt;
		this.ps_smpl = ps_smpl;// 查防重复
		this.ps_insrt = ps_insrt;// 成功注册时插入数据库
	}

	@Override
	public void run() {
		String UsrNum = null;// 账户名
		String Passwd = null;// 密码
		String Name = null;// 网名
		String HeadID = null;// 头像号码

		try {
			// 用Socket连接对象去初始化输入输出流,将其与客户端输出输入连接
			this.dis = new DataInputStream(sckt.getInputStream());
			this.dos = new DataOutputStream(sckt.getOutputStream());
			// 没能注册成功时,不停接收(用户可以不断尝试注册)
			while (success == false) {
				// 从输入流读入用户发来的消息
				String str = dis.readUTF();// 阻塞发生之处
				// 确保其消息格式,以"[sign]"开头才是要注册
				if (str.startsWith("[sign]") == false)
					return;// 如果对方Socket发来的消息格式不对,这个连接就是错误危险的,直接结束该线程
				// 解析消息:得到账号,密码,网名,头像号码
				UsrNum = str.substring(str.indexOf("]") + 1, str.indexOf("#"));
				Passwd = str.substring(str.indexOf("#") + 1, MyTools.indexOf(str, 2, "#"));
				Name = str.substring(MyTools.indexOf(str, 2, "#") + 1, str.lastIndexOf("#"));
				HeadID = str.substring(str.lastIndexOf("#") + 1);
				// 对账号做一个判断
				if (Integer.parseInt(UsrNum) > 30000 || Integer.parseInt(UsrNum) < 10000) {
					this.dos.writeUTF("[x]账号需在10000至30000之间");
					continue;
				}
				// PreparedStatement对象的setString方法定义了字符串中第n个"?"字符的替换
				ps_smpl.setString(1, UsrNum);// 建立好查防重复的sql语句对象
				// 返回结果集,注意PreparedStatement对象的执行方法不需要参数
				// 因为sql语句就在PreparedStatement对象中
				rs = ps_smpl.executeQuery();
				// 对返回的结果集的处理,查询结果非空时说明账户名已经存在,不允许注册
				// 实际上查询结果只能有0行或1行,因为不可能两个相同账号的人
				if (rs.next() == true) {
					this.dos.writeUTF("[x]该账号在数据库中已存在");
				}
				// 结果集为空,说明这个账户是可用的,允许注册,尝试插入数据库表项
				else {
					// 为插入用的sql语句对象的四个'?'替换
					ps_insrt.setString(1, UsrNum);
					ps_insrt.setString(2, Passwd);
					ps_insrt.setString(3, Name);
					ps_insrt.setString(4, HeadID);
					// 执行这条语句,往数据库SmplMsg表里里插入用户提供的注册信息
					int ok = ps_insrt.executeUpdate();// 注意使用的是executeUpdate()方法
					// 返回的数字是受影响的行数,当不为0(即是1)时,说明插入成功,即注册成功
					if (ok == 1) {
						success = true;// 记录,以退出while
						this.dos.writeUTF("[v]注册成功");// 告诉客户端注册成功
					} else {
						this.dos.writeUTF("[x]因为服务端或数据库问题注册失败");
					}
				}
			}
			// 运行至此,说明注册成功
			System.out.println("[v]成功注册来自:" + sckt.getInetAddress());
		} catch (IOException e) {
			// 当客户端还没成功注册,就关闭了窗口或者以其它方式强行结束了客户端程序时发生此异常
			System.out.println("[-]放弃注册来自:" + sckt.getInetAddress());
		} catch (SQLException e) {
			e.printStackTrace();// PreparedStatement对象相关
		}
	}
}
