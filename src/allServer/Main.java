package allServer;

//在主类中建立两个线程,一个用来管理登陆者,一个用来管理和其他人通信
public class Main {

	public static void main(String[] args) {
		// [登陆请求管理线程],[单独通信线程]
		Thread thrd_lgn, thrd_cht;

		// [登陆请求管理线程]的目标对象
		LoginServer ls = new LoginServer();
		// [登陆请求管理线程]的线程对象
		thrd_lgn = new Thread(ls);
		// 启动[登陆请求管理线程]
		thrd_lgn.start();
	}

}
