package allServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

//[核心处理线程]子线程,紧跟在在[登陆请求处理线程]的成功登录之后
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
			// 不停地从客户端接收
			while (true) {
				s = dis.readUTF();
				System.out.println("[+][接收]" + s);// 测试输出
				dos.writeUTF("[服务器端]接收到了!");
				// 客户要下线,发来了"[bye]"+自己的账户名
				if (s.startsWith("[bye]")) {
					// 在哈希表中去掉这个映射,表示这个用户已经不在线
					Main.hm_usrTOip.remove(s.substring(s.indexOf("]") + 1));
				}
			}
		} catch (IOException e) {
			// 该客户端关闭时会发生此异常
			System.out.println("[-]" + sckt.getInetAddress() + "断开连接");
		}
	}
}
