package allServer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.TreeSet;

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
			String uri = "jdbc:mysql://" + Main.DBIp + ":3306/ChatCatDB?useSSL=true&characterEncoding=utf8";
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

	@Override
	public void run() {
		while (true) {
			try {
				dealWithMsg();// 处理发给自己的消息
				dealWithAdd();// 处理发给自己的好友请求
				dealWithPrompt();// 处理提示/刷新消息
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

	// 处理提示/刷新消息:客户端可能提示消息,此外根据服务器传的信息刷新自己的某些组件
	// 每个线程只处理自己在第五张哈希表中的项目,不需要synchronized保护
	// synchronized保护的是clear()和dealWithAdd()里的add()
	private synchronized void dealWithPrompt() throws IOException, SQLException {
		// 未建立或者建立了但是为空时都不需要刷新
		if (Main.hm_usrTOprmpt.get(nm) == null || Main.hm_usrTOprmpt.get(nm).isEmpty())
			return;
		// 执行至此说明有消息,但这么多消息实际上客户端只要刷新一次就好了!
		// 因为要提示的服务器或数据库里的信息更新并不是在下面的for循环过程中的
		// 而是早就更新完了,所以把"刷新"和"提示"在这个方法中分开
		// 不妨用"[refresh]"表示要刷新
		// 用"[!]提示"的方式表示要显示提示
		dos.writeUTF("[refresh]" + getRefreshMsg());// 刷新一次,传入getRefreshMsg()获取的更新信息
		for (String s : Main.hm_usrTOprmpt.get(nm)) {
			dos.writeUTF("[!]" + s);// 但每次都要提示
		}
		// 清空TreeSet,实际上这样做可能会和其它写入TreeSet者冲突,所以需要synchronized
		// 对于这一个线程对象(成为锁),不同的synchronized方法也不可以并发/并行执行!
		Main.hm_usrTOprmpt.get(nm).clear();
	}

	// 处理发给自己的好友请求,因为可能操作不止自己的哈希表,所以用synchronized
	private synchronized void dealWithAdd() throws SQLException {
		if (Main.hm_usrTOts.get(nm) == null)// 从来没人发给过自己好友请求
			return;// 直接结束,自己无权添加自己的哈希表项,必须等别人发好友请求
		if (Main.hm_usrTOts.get(nm).isEmpty())// 暂时没有需要处理的好友请求
			return;// 直接结束
		// 遍历这个TreeSet中,对于记录的所有请求好友者
		for (String s : Main.hm_usrTOts.get(nm)) {
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
				// 向双方在第五张哈希表中对应的TreeSet,写入成为好友的提示消息
				// 如果未建立项,当场建立,这也是用synchronized保护到的地方
				if (Main.hm_usrTOprmpt.get(nm) == null) {
					Main.hm_usrTOprmpt.put(nm, new TreeSet<String>());
				}
				if (Main.hm_usrTOprmpt.get(s) == null) {
					Main.hm_usrTOprmpt.put(s, new TreeSet<String>());
				}
				Main.hm_usrTOprmpt.get(nm).add("你和" + s + "成为好友");
				Main.hm_usrTOprmpt.get(s).add("你和" + nm + "成为好友");
				// 最终,把这两个好友请求从内存(第四张哈希表)中清除
				// 不然会不停地成为好友,数据表项(竟然可以重复)越来越多
				Main.hm_usrTOts.get(s).remove(nm);
				Main.hm_usrTOts.get(nm).remove(s);
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

	// 获得为客户端更新信息用的信息字符串
	private String getRefreshMsg() throws SQLException {
		// 查询好友账号的sql语句对象
		PreparedStatement ps_frnd = con.prepareStatement(
				"SELECT Usr1 FROM FrndMsg WHERE Usr2=" + nm + " UNION " + "SELECT Usr2 FROM FrndMsg WHERE Usr1=" + nm);
		// 通过好友账号查询详细信息的sql语句对象
		PreparedStatement ps_frndmsg = con.prepareStatement("SELECT * FROM SmplMsg WHERE UsrNum=?");
		// 先查出所有账号来
		ResultSet rs_frnd = ps_frnd.executeQuery();
		// 准备好要返回的字符串
		String str_rtn = "";
		// 对于每个账号
		while (rs_frnd.next()) {
			// 替换掉'?'
			ps_frndmsg.setString(1, "" + rs_frnd.getInt(1));
			// 执行查询
			ResultSet rs_frndmsg = ps_frndmsg.executeQuery();
			// 进到第一行
			rs_frndmsg.next();
			// 添加到要返回的字符串上
			str_rtn = str_rtn + "#" + rs_frndmsg.getInt(1) + "&" + rs_frndmsg.getString(3) + "&" + rs_frndmsg.getInt(4)
					+ "&" + rs_frndmsg.getString(6);
		}
		// System.out.println(str_rtn);
		return str_rtn;
	}
}
