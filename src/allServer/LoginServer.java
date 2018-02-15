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
		while (true) {
			try {
				ss = new ServerSocket(3838);// 3838�˿�
				System.out.println("�ȴ��ͻ�������...");
				sckt = ss.accept();// �����Եȴ�����
				System.out.println("[+]�ͻ��˵�ַ:" + sckt.getInetAddress());
				// Ϊ����¿ͻ�����[��½�������߳�]����,����רΪ����ͻ�������߳�
				new IWannaLogin(sckt).start();
			} catch (IOException e) {
				// �����ظ�����ServerSocket
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
			// �������������û���������Ϣ
			String str = dis.readUTF();// ��������֮��
			// ȷ������Ϣ��ʽ,��"[login]"��ͷ����Ҫ��¼
			if (str.startsWith("[login]") == false)
				return;
			System.out.println(str);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
