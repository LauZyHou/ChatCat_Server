package allServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;

//[�ͻ���Ϣ�����߳�]���߳�,��������[��½�������߳�]�ĳɹ���¼֮��
public class DealWithKernel extends Thread {
	// Socket����,�ڹ������ﴫ��[��½�������߳�]�����Ӻõ�
	Socket sckt = null;
	// �������������
	DataInputStream dis;
	DataOutputStream dos;
	// �ϼ��߳�����ǰ���������Լ����˻���
	String nm;

	// ������
	public DealWithKernel(Socket sckt, String nm) {
		// �����Ų�:Socket������˻���
		this.sckt = sckt;
		this.nm = nm;
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
			// ��ͣ�شӿͻ��˽����û���������Ϣ
			while (true) {
				s = dis.readUTF();// ��������֮��
				System.out.println("[+][����]" + s);// �������
				dos.writeUTF("[��������]���յ���!");// ���Ի�Ӧ
				/* ���ڲ�ͬ����Ϣͷ,����������ͬ������ */
				// �ͻ�Ҫ�������ͻ�������Ϣ
				if (s.startsWith("[to")) {
					dealSndMsg(s);// ������Ϣ
				}
			}
		} catch (IOException e) {
			// �ÿͻ��˹ر�ʱ�ᷢ�����쳣
			System.out.println("[-]" + nm + sckt.getInetAddress() + "�Ͽ�����");
			// �ڵ�һ�Ź�ϣ����ȥ������û�,��ʾ����û��Ѿ�������
			Main.hm_usrTOip.remove(nm);
		}
	}

	// ������Ϣ,��Ϊ�������ٽ���Դ,������synchronized����
	private synchronized void dealSndMsg(String s) {
		// ������Ϣ
		String str_to = s.substring(3, s.indexOf("]["));// ��ϢĿ���˻�
		String str_frm = s.substring(s.indexOf("][im") + 4, MyTools.indexOf(s, 2, "]"));// ��ϢԴ�˻�
		String str_msg = s.substring(MyTools.indexOf(s, 2, "]") + 1);// Ҫ���͵���Ϣ,����+1���Է����Ĳ���Ϊ��
		// System.out.println(str_to + "," + str_frm + "," + str_msg);// �������
		// �ж��û��Ƿ�����,������ͬ�Ĵ���
		// �õ�һ�ű�(��¼״����)�ж�
		if (Main.hm_usrTOip.get(str_to) != null) {
			// Ŀ���û�����ʱ,����Ϣд���ڴ�������ڶ��ž�̬��ϣ���LinkedList��
			Main.hm_usrTOmsg.get(str_to).add(str_frm + "\n\n" + str_msg);// ��Ϊ�û�û������,�����û��з��ָ�
			Main.hm_usrTOthrd.get(str_to).interrupt();// �������sleep(),��������Ϊ�û�������Ϣ
		} else {
			System.out.println("[+mem]" + str_to + "������,��Ϣ�ݴ��ڷ�����");
			// ����Ϣд���ڴ�������ڶ��ž�̬��ϣ���LinkedList��
			// ע��Ҫ�ж��Ƿ�û����,û�д���ʱҪ�ȴ���
			if (Main.hm_usrTOmsg.get(str_to) == null) {
				Main.hm_usrTOmsg.put(str_to, new LinkedList<String>());
			}
			Main.hm_usrTOmsg.get(str_to).add(str_frm + "\n\n" + str_msg);// ��Ϊ�û�û������,�����û��з��ָ�
			// ��ΪĿ���û�������,����һ��û������[�ڴ���Ϣ�����߳�]
			// ��Main.hm_usrTOthrd.get(str_to)��null,���ؿ���interrupt()
		}
		// System.out.println(Main.hm_usrTOmsg);// �������
	}
}
