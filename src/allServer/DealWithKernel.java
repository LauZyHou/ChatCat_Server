package allServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

//[���Ĵ����߳�]���߳�,��������[��½�������߳�]�ĳɹ���¼֮��
public class DealWithKernel extends Thread {
	// Socket����,�ڹ������ﴫ��[��½�������߳�]�����Ӻõ�
	Socket sckt = null;
	// �������������
	DataInputStream dis;
	DataOutputStream dos;

	// ������
	public DealWithKernel(Socket sckt) {
		this.sckt = sckt;
		try {
			// �ؽ����������
			dis = new DataInputStream(sckt.getInputStream());
			dos = new DataOutputStream(sckt.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// ִ��ʱ
	@Override
	public void run() {
		String s = null;// ��������յ�����Ϣ
		try {
			// ��ͣ�شӿͻ��˽���
			while (true) {
				s = dis.readUTF();
				System.out.println("[+][����]" + s);// �������
				dos.writeUTF("[��������]���յ���!");
				// �ͻ�Ҫ����,������"[bye]"+�Լ����˻���
				if (s.startsWith("[bye]")) {
					// �ڹ�ϣ����ȥ�����ӳ��,��ʾ����û��Ѿ�������
					Main.hm_usrTOip.remove(s.substring(s.indexOf("]") + 1));
				}
			}
		} catch (IOException e) {
			// �ÿͻ��˹ر�ʱ�ᷢ�����쳣
			System.out.println("[-]" + sckt.getInetAddress() + "�Ͽ�����");
		}
	}
}
