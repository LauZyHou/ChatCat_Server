package allServer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;

//[内存消息接收线程],它和[客户消息处理线程]并排,紧跟在在[登陆请求处理线程]的成功登录之后
//这个线程用来处理那些放进内存(主类中的第二张静态哈希表)中的其他客户发给自己的消息
public class DealWithMem extends Thread {
	Socket sckt;// 从上级线程死亡前传进来的连接好的Socket
	// 这个线程只需要一个发送流,而不需要接收消息的
	// 因为接收客户端的消息和处理已经在[客户消息处理线程]子线程里做了
	DataOutputStream dos = null;
	// 上级线程死亡前传进来的自己的账户名
	String nm;
	// 加载一次数据库连接,供后面多次使用
	Connection con;

	// 构造器
	DealWithMem(Socket sckt, String nm) {
		// 保留遗产:Socket对象和账户名
		this.sckt = sckt;
		this.nm = nm;
		try {
			// 构造发送流
			this.dos = new DataOutputStream(sckt.getOutputStream());
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

	// 虽然都操作了第三张哈希表,但这组线程只会操作自己的LinkedList,不必synchronized
	@Override
	public void run() {
		while (true) {
			try {
				dealWithMsg();// 处理发给自己的消息
				dealWithAdd();// 处理发给自己的好友请求

				// 之所以要做第三张哈希表就是因为这个sleep(),
				// 放进哈希表里让这个线程的引用对DealWithKernel可见
				// 则对方可以在写完后立即interrupt()这个线程
				sleep(1000);// 有了sleep()让服务器CPU压力低一些
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				// 被打断是正常的,不抛出异常
				// 在下一个循环中将对发给自己的消息做处理
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} // end while
	}

	// 处理发给自己的好友请求,因为可能操作不止自己的哈希表,所以用synchronized
	private synchronized void dealWithAdd() throws SQLException {
		if (Main.hm_usrTOts.get(nm) == null)// 从来没人发给过自己好友请求
			return;// 直接结束,自己无权添加自己的哈希表项,必须等别人发好友请求
		if (Main.hm_usrTOts.get(nm).isEmpty())// 暂时没有需要处理的好友请求
			return;// 直接结束
		// 遍历这个TreeSet中,对于记录的所有请求好友者
		for (String s : Main.hm_usrTOts.get(nm)) {
			// TODO
			// 看看自己有没有向他发过请求,(谨防空指针异常,用短路判断)
			if (Main.hm_usrTOts.get(s) != null && Main.hm_usrTOts.get(s).contains(nm)) {
				// 既然双方都向对方发过请求,两者直接成为好友
				int ANum = Integer.parseInt(nm);
				int BNum = Integer.parseInt(s);
				PreparedStatement ps;
				if (ANum < BNum)
					ps = con.prepareStatement("INSERT INTO FrndMsg VALUES (" + ANum + "," + BNum + ")");
				else
					ps = con.prepareStatement("INSERT INTO FrndMsg VALUES (" + BNum + "," + ANum + ")");
				int ok = ps.executeUpdate();// 成功成为好友将1行受影响
				if (ok != 1)// 留给开发者提醒
					System.out.println("[!]自动成为好友出问题");
			}
		}
	}

	// 处理发给自己的消息,每个用户只处理自己的,不用synchronized
	private void dealWithMsg() throws IOException {
		// 从第二张哈希表里拿出自己的那份消息链表
		LinkedList<String> ll = Main.hm_usrTOmsg.get(nm);
		// 判断自己的消息链表是否非空,非空时才发里面的消息
		// 注意,不是做!=null判断,!=null时是在线而不是有消息
		if (!ll.isEmpty()) {
			// 对于消息链表中的每个消息"消息来源\n\n消息内容"
			for (String s : ll) {
				// 发送给客户端,由客户端自己对不同的来源做不同的处理
				dos.writeUTF(s);
			}
			// 处理完成把自己的消息链表清空
			ll.clear();
		}
	}
}
