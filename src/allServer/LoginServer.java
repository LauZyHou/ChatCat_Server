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
	// 服务端Socket,用来建立Socket对象
	ServerSocket ss = null;
	// Socket连接对象
	Socket sckt = null;
	// sql连接对象
	java.sql.Connection con;
	// PreparedStatement是Statement的子类,也是一种sql语句对象
	java.sql.PreparedStatement ps_smpl;// 查询SmplMsg表,用于验证登录
	java.sql.PreparedStatement ps_frnd;// 查询FrndMsg表,用于在登录后提供好友列表
	java.sql.PreparedStatement ps_prmy_smpl;// 只用主键(账号)查询SmplMsg表,用于获取好友信息

	@Override
	public void run() {
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
			ps_smpl = con.prepareStatement("SELECT * FROM SmplMsg WHERE UsrNum=? AND Passwd=?");
			ps_frnd = con.prepareStatement(
					"SELECT Usr1 FROM FrndMsg WHERE Usr2=? " + "UNION " + "SELECT Usr2 FROM FrndMsg WHERE Usr1=?");
			ps_prmy_smpl = con.prepareStatement("SELECT * FROM SmplMsg WHERE UsrNum=?");
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
				new IWannaLogin(sckt, ps_smpl, ps_frnd, ps_prmy_smpl).start();
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
	// 备好的sql语句对象
	java.sql.PreparedStatement ps_smpl;
	java.sql.PreparedStatement ps_frnd;
	java.sql.PreparedStatement ps_prmy_smpl;
	// 查询返回的结果集,每次查询都在给它赋值
	ResultSet rs;

	// 构造器(Socket连接对象,PreparedStatement备好的sql语句对象)
	IWannaLogin(Socket sckt, java.sql.PreparedStatement ps_smpl, java.sql.PreparedStatement ps_frnd,
			java.sql.PreparedStatement ps_prmy_smpl) {
		this.sckt = sckt;
		this.ps_smpl = ps_smpl;// 验证登录
		this.ps_frnd = ps_frnd;// 好友列表
		this.ps_prmy_smpl = ps_prmy_smpl;// 主键索引查询(好友信息)
	}

	// [登陆请求处理线程]子线程运行
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
				// 截取出账户名和密码
				String nm = str.substring(str.indexOf("]") + 1, str.indexOf("#"));
				String pswd = str.substring(str.indexOf("#") + 1);
				// PreparedStatement对象的setString方法定义了字符串中第n个"?"字符的替换
				ps_smpl.setString(1, nm);
				ps_smpl.setString(2, pswd);
				// 返回结果集,注意PreparedStatement对象的执行方法不需要参数
				// 因为sql语句就在PreparedStatement对象中
				rs = ps_smpl.executeQuery();
				// 对返回的结果集的处理,查询结果非空时说明账密正确,允许登录
				// 同时rs.next()下移一行到达了第一行
				// 实际上查询结果只能有0行或1行,因为不可能两个相同账密的人
				// 甚至在主键UsrNum上就不可能相同
				if (rs.next() == true) {
					success = true;// 非空,允许登录,下一个while将不执行
					String Name = rs.getString(3);// 获取这(唯一行)用户的用户名
					int HeadID = rs.getInt(4);// 头像的ID号
					String str_send = "[v]#用户名:" + Name + "#头像ID:" + HeadID;// 拼给要发送的字符串
					// 当账密正确时,即在这个if块内,对好友进行查询,返回好友列表
					ps_frnd.setString(1, nm);// 替换'?'
					ps_frnd.setString(2, nm);// 替换'?'
					ResultSet rs_frnd = ps_frnd.executeQuery();// 执行查询
					// 再拿这个好友列表中的每一项去查询SmplMsg表,得到好友的(账号,网名,头像ID)投影
					ResultSet rs_prmy_smpl = null;// 暂存每次的查询结果
					while (rs_frnd.next() == true) {
						int frndUsrNum = rs_frnd.getInt(1);// 从好友列表中只能得到该好友的账号
						ps_prmy_smpl.setString(1, "" + frndUsrNum);// 把这个索引替换'?'处
						rs_prmy_smpl = ps_prmy_smpl.executeQuery();// 执行查询得到结果集
						// 确保存在,只要数据库表管理的好这里if可以去掉!
						if (rs_prmy_smpl.next() == true) {
							String frndName = rs_prmy_smpl.getString(3);// 第3列:该好友的网名
							int frndHeadID = rs_prmy_smpl.getInt(4);// 第4列:该好友的头像ID
							str_send = str_send + "#" + frndUsrNum + "," + frndName + "," + frndHeadID;// 拼给要发送的字符串
						}
					}
					// 把最后拼出来的要发送的字符串发送给客户端
					this.dos.writeUTF(str_send);
				}
				// 结果集为空,说明账密在数据库中没有查询到匹配项,登录失败
				else {
					// 告诉客户端登录失败
					this.dos.writeUTF("[x]账户名或密码错误");
				}
			}
			// 运行至此,说明成功登录了服务器
			System.out.println("[v]成功登录来自:" + sckt.getInetAddress());
		} catch (IOException e) {
			// 当客户端还没点过登录按钮,就关闭了窗口或者以其它方式强行结束了客户端程序时
			System.out.println("[-]放弃登录来自:" + sckt.getInetAddress());
		} catch (SQLException e) {
			e.printStackTrace();// PreparedStatement对象相关
		}
	}
}
