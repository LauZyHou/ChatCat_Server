package allServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;

//[客户消息处理线程]子线程,紧跟在在[登陆请求处理线程]的成功登录之后
public class DealWithKernel extends Thread {
	// Socket对象,在构造器里传入[登陆请求处理线程]中连接好的
	Socket sckt = null;
	// 数据输入输出流
	DataInputStream dis;
	DataOutputStream dos;
	// 上级线程死亡前传进来的自己的账户名
	String nm;
	// 加载一次数据库连接,供后面多次使用
	Connection con;

	// 构造器
	public DealWithKernel(Socket sckt, String nm) {
		// 保留遗产:Socket对象和账户名
		this.sckt = sckt;
		this.nm = nm;
		try {
			// 重建输入输出流
			dis = new DataInputStream(sckt.getInputStream());
			dos = new DataOutputStream(sckt.getOutputStream());
			// 加载一次数据库连接,供后面多次使用
			Class.forName("com.mysql.jdbc.Driver");
			String uri = "jdbc:mysql://192.168.0.106:3306/ChatCatDB?useSSL=true&characterEncoding=utf8";
			String user = "root";// 用户名
			String password = "3838438"; // 密码
			con = DriverManager.getConnection(uri, user, password);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// 执行时
	@Override
	public void run() {
		String s = null;// 用来存接收到的消息
		try {
			// 不停地从客户端接收用户发来的消息
			while (true) {
				s = dis.readUTF();// 阻塞发生之处
				System.out.println("[+][接收]" + s);// 测试输出
				dos.writeUTF("[服务器端]接收到了!");// 测试回应
				/* 对于不同的消息头,服务器做不同的事情 */
				// 客户要向其它客户发送消息
				if (s.startsWith("[to")) {
					dealSndMsg(s);// 处理发消息
				}
				// 客户要获取自己的资料卡
				else if (s.equals("[mycard]")) {
					dealGetCard();// 处理获取资料卡
				}
				// 客户要把新的个人资料写入数据库
				else if (s.startsWith("[changecard]")) {
					dealChngCard(s);// 处理修改个人资料
				}
			}
		} catch (IOException e) {
			// 在第一张哈希表中去掉这个用户,表示这个用户已经不在线
			Main.hm_usrTOip.remove(nm);
			// 从第三张哈希表中找到这个线程的引用,立即终止它!
			Main.hm_usrTOthrd.get(nm).stop();
			// 在第三张哈希表中去掉这个用户,表示这个用户已经不在线
			Main.hm_usrTOthrd.remove(nm);
			// 该客户端关闭时会发生此异常
			System.out.println("[-]" + nm + sckt.getInetAddress() + "断开连接");
		}
	}

	// 处理修改个人资料,完整的执行sql语句之间互不影响,不需synchronized保护
	private void dealChngCard(String s) {
		String Name = s.substring(s.indexOf("]") + 1, s.indexOf("#"));
		String HeadID = s.substring(s.indexOf("#") + 1, MyTools.indexOf(s, 2, "#"));
		String Sex = s.substring(MyTools.indexOf(s, 2, "#") + 1, s.lastIndexOf("#"));
		String Signature = s.substring(s.lastIndexOf("#") + 1);
		try {
			// 更新数据库表
			PreparedStatement ps_smpl = con.prepareStatement("UPDATE SmplMsg SET Name='" + Name + "',HeadID=" + HeadID
					+ ",Sex=" + Sex + ",Signature='" + Signature + "' WHERE UsrNum=" + nm);
			int rs = ps_smpl.executeUpdate();
			if (rs == 1) {
				dos.writeUTF("[changecard]success");// 成功信息
			} else {
				dos.writeUTF("[changecard]failed");// 失败信息
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// 处理获取资料卡
	private void dealGetCard() {
		try {
			PreparedStatement ps_smpl = con.prepareStatement("SELECT * FROM SmplMsg WHERE UsrNum=" + nm);
			ResultSet rs = ps_smpl.executeQuery();
			if (rs.next()) {
				String Name = rs.getString(3);// 获取这(唯一行)用户的用户名
				int HeadID = rs.getInt(4);// 头像的ID号
				int Sex = rs.getInt(5);// 性别0女1男
				String Signature = rs.getString(6);// 个性签名
				// 拼成字符串发送
				String send = "[mycard]" + Name + "#" + HeadID + "#" + Sex + "#" + Signature;
				dos.writeUTF(send);
			} else {
				// TODO 似乎不会发生这种情况
				System.out.println("[!]奇异情况发生了");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	// 处理发消息,因为操作了临界资源,所以用synchronized保护
	private synchronized void dealSndMsg(String s) {
		// 解析消息
		String str_to = s.substring(3, s.indexOf("]["));// 消息目标账户
		String str_frm = s.substring(s.indexOf("][im") + 4, MyTools.indexOf(s, 2, "]"));// 消息源账户
		String str_msg = s.substring(MyTools.indexOf(s, 2, "]") + 1);// 要发送的消息,这里+1所以发来的不能为空
		// System.out.println(str_to + "," + str_frm + "," + str_msg);// 测试输出
		// 判断用户是否在线,做出不同的处理
		// 用第一张表(登录状况表)判断
		if (Main.hm_usrTOip.get(str_to) != null) {
			// 目标用户在线时,把消息写入内存中主类第二张静态哈希表的LinkedList里
			Main.hm_usrTOmsg.get(str_to).add(str_frm + "\n\n" + str_msg);// 因为用户没法换行,所以用换行符分隔
			Main.hm_usrTOthrd.get(str_to).interrupt();// 打断它的sleep(),让它立即为用户处理消息
		} else {
			System.out.println("[+mem]" + str_to + "不在线,消息暂存在服务器");
			// 把消息写入内存中主类第二张静态哈希表的LinkedList里
			// 注意要判断是否还没创建,没有创建时要先创建
			if (Main.hm_usrTOmsg.get(str_to) == null) {
				Main.hm_usrTOmsg.put(str_to, new LinkedList<String>());
			}
			Main.hm_usrTOmsg.get(str_to).add(str_frm + "\n\n" + str_msg);// 因为用户没法换行,所以用换行符分隔
			// 因为目标用户不在线,所以一定没有启动[内存消息接收线程]
			// 即Main.hm_usrTOthrd.get(str_to)是null,不必考虑interrupt()
		}
		// System.out.println("[1表]" + Main.hm_usrTOip);// 测试输出
		// System.out.println("[2表]" + Main.hm_usrTOmsg);// 测试输出
		// System.out.println("[3表]" + Main.hm_usrTOthrd);// 测试输出
	}
}
