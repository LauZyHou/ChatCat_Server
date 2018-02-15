package allServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

//[��½��������߳�],3838�˿ڹ����¼����
public class LoginServer implements Runnable {
	ServerSocket ss = null;// �����Socket,��������Socket����
	Socket sckt = null;// Socket���Ӷ���

	@Override
	public void run() {
		// �����[��½��������߳�]��,��ͣ�ؼ����ͻ��˷����ĵ�¼����
		try {
			ss = new ServerSocket(3838); // 3838�˿�
			System.out.println("[#]��ʼ�ȴ��ͻ�������...");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		while (true) {
			try {
				sckt = ss.accept();// �����Եȴ�����
				System.out.println("[+]�ͻ��˵�ַ:" + sckt.getInetAddress());
				// Ϊ����¿ͻ�����[��½�������߳�]����,����רΪ����ͻ�������߳�
				new IWannaLogin(sckt).start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}// end run()
}

// [��½�������߳�]���߳�,ÿ���ͻ�����һ��
class IWannaLogin extends Thread {
	// �ӹ����������Socket���Ӷ���
	Socket sckt;
	// �������������
	DataInputStream dis;
	DataOutputStream dos;
	// ����ֵָʾ�Ƿ��¼�ɹ�,��ʼΪ��
	boolean success = false;

	// ������
	IWannaLogin(Socket sckt) {
		this.sckt = sckt;
	}

	@Override
	public void run() {
		try {
			// ��Socket���Ӷ���ȥ��ʼ�����������,������ͻ��������������
			this.dis = new DataInputStream(sckt.getInputStream());
			this.dos = new DataOutputStream(sckt.getOutputStream());
			// û��������ȷ�����ܵ�¼�ɹ�ʱ,��ͣ����(�û����Բ��ϳ���)
			while (success == false) {
				// �������������û���������Ϣ
				String str = dis.readUTF();// ��������֮��
				// ȷ������Ϣ��ʽ,��"[login]"��ͷ����Ҫ��¼
				if (str.startsWith("[login]") == false)
					return;// ����Է�Socket��������Ϣ��ʽ����,������Ӿ���Σ�յ�,ֱ�ӽ������߳�
				// System.out.println(str);
				// if (str.endsWith("3838438")) {
				String nm = str.substring(str.indexOf("]") + 1, str.indexOf("#"));
				String pswd = str.substring(str.indexOf("#") + 1);
				System.out.println(nm + " " + pswd);
				success = true;
				// }
			}
		} catch (IOException e) {
			System.out.println("[-]������¼����:" + sckt.getInetAddress());
		}
	}
}
