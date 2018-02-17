package allServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

//[客户消息处理线程]子线程,紧跟在在[登陆请求处理线程]的成功登录之后
public class DealWithKernel extends Thread {
	// Socket对象,在构造器里传入[登陆请求处理线程]中连接好的
	Socket sckt = null;
	// 数据输入输出流
	DataInputStream dis;
	DataOutputStream dos;

	// 构造器
	public DealWithKernel(Socket sckt) {
		this.sckt = sckt;
		try {
			// 重建输入输出流
			dis = new DataInputStream(sckt.getInputStream());
			dos = new DataOutputStream(sckt.getOutputStream());
		} catch (IOException e) {
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
				// 客户要下线,发来了"[bye]"+自己的账户名
				if (s.startsWith("[bye]")) {
					// 在哈希表中去掉这个映射,表示这个用户已经不在线
					Main.hm_usrTOip.remove(s.substring(s.indexOf("]") + 1));
				}
				// 客户要向其它客户发送消息
				else if (s.startsWith("[to")) {
					dealSndMsg(s);// 处理发消息
				}
			}
		} catch (IOException e) {
			// 该客户端关闭时会发生此异常
			System.out.println("[-]" + sckt.getInetAddress() + "断开连接");
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
		// 用这两个哈希表判断都可以,考虑到第一张表比较小所以用了第一张(实际上第一张表现在完全可以去掉)
		if (Main.hm_usrTOip.get(str_to) != null) {
			// 目标用户在线时,把消息写入内存中主类第二张静态哈希表的LinkedList里
			Main.hm_usrTOmsg.get(str_to).add(str_frm + "\n\n" + str_msg);// 因为用户没法换行,所以用换行符分隔
			Main.hm_usrTOthrd.get(str_to).interrupt();// 打断它的sleep(),让它立即为用户处理消息
		} else {
			System.out.println("[x]目标用户不在线");
		}
		// System.out.println(Main.hm_usrTOmsg);// 测试输出
	}
}
