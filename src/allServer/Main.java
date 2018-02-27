package allServer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;

//主类
public class Main {
	// 数据库内网ip
	public static String DBIp = "192.168.1.100";

	/* 提供给[登录请求管理线程]及其分支使用的资源 */
	// 主类中的静态哈希映射表,记录登录者的<账号,ip地址>映射,在主类加载时即刻加载
	// 因为服务器一关,势必所有用户掉线,所以这个映射只要放在内存里而不需写进数据库
	public static HashMap<String, String> hm_usrTOip = new HashMap<String, String>();
	// 第二张哈希表记录<目标用户账号,发送给该用户的消息LinkedList引用>
	public static HashMap<String, LinkedList<String>> hm_usrTOmsg = new HashMap<String, LinkedList<String>>();
	// 第三张哈希表记录<目标用户账号,目标用户的[内存消息接收线程]的引用>
	public static HashMap<String, DealWithMem> hm_usrTOthrd = new HashMap<String, DealWithMem>();
	// 第四张哈希表记录<目标用户账号,存向该用户发送请求的所有账号组成的TreeSet>,TreeSet可以保证有序
	public static HashMap<String, TreeSet<String>> hm_usrTOts = new HashMap<String, TreeSet<String>>();
	// 第五张哈希表记录<目标用户账号,存向该用户发的所有提示消息组成的TreeSet>,TreeSet可以保证有序
	public static HashMap<String, TreeSet<String>> hm_usrTOprmpt = new HashMap<String, TreeSet<String>>();

	public static void main(String[] args) {
		// [登陆请求管理线程]
		Thread thrd_lgn;
		// [登陆请求管理线程]的目标对象
		LoginServer ls = new LoginServer();
		// [登陆请求管理线程]的线程对象
		thrd_lgn = new Thread(ls);
		// 启动[登陆请求管理线程]
		thrd_lgn.start();

		// [注册请求管理线程]
		Thread thrd_sgn;
		// [注册请求管理线程]的目标对象
		SignServer ss = new SignServer();
		// [注册请求管理线程]的线程对象
		thrd_sgn = new Thread(ss);
		// 启动[注册请求管理线程]
		thrd_sgn.start();
	}

}
