package allServer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
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

	// 构造器
	DealWithMem(Socket sckt, String nm) {
		// 保留遗产:Socket对象和账户名
		this.sckt = sckt;
		this.nm = nm;
		try {
			// 构造发送流
			this.dos = new DataOutputStream(sckt.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// 虽然都操作了第三张哈希表,但这组线程只会操作自己的LinkedList,不必synchronized
	@Override
	public void run() {
		// 不停地判断,发送,sleep()
		while (true) {
			// 从第二张哈希表里拿出自己的那份消息链表
			LinkedList<String> ll = Main.hm_usrTOmsg.get(nm);
			try {
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
			} catch (IOException e) {
				e.printStackTrace();
			}
			// 之所以要做第三张哈希表就是因为这个sleep(),
			// 放进哈希表里让这个线程的引用对DealWithKernel可见,则它可以在写完后立即interrupt()这个线程
			try {
				sleep(1000);// 有了sleep()让服务器CPU压力低一些
			} catch (InterruptedException e) {
				// 被打断是正常的,不抛出异常
				// 在下一个循环中将对发给自己的消息做处理
			}
		}
	}
}
