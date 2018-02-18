package allServer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;

//[�ڴ���Ϣ�����߳�],����[�ͻ���Ϣ�����߳�]����,��������[��½�������߳�]�ĳɹ���¼֮��
//����߳�����������Щ�Ž��ڴ�(�����еĵڶ��ž�̬��ϣ��)�е������ͻ������Լ�����Ϣ
public class DealWithMem extends Thread {
	Socket sckt;// ���ϼ��߳�����ǰ�����������Ӻõ�Socket
	// ����߳�ֻ��Ҫһ��������,������Ҫ������Ϣ��
	// ��Ϊ���տͻ��˵���Ϣ�ʹ����Ѿ���[�ͻ���Ϣ�����߳�]���߳�������
	DataOutputStream dos = null;
	// �ϼ��߳�����ǰ���������Լ����˻���
	String nm;

	// ������
	DealWithMem(Socket sckt, String nm) {
		// �����Ų�:Socket������˻���
		this.sckt = sckt;
		this.nm = nm;
		try {
			// ���췢����
			this.dos = new DataOutputStream(sckt.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// ��Ȼ�������˵����Ź�ϣ��,�������߳�ֻ������Լ���LinkedList,����synchronized
	@Override
	public void run() {
		// ��ͣ���ж�,����,sleep()
		while (true) {
			// �ӵڶ��Ź�ϣ�����ó��Լ����Ƿ���Ϣ����
			LinkedList<String> ll = Main.hm_usrTOmsg.get(nm);
			try {
				// �ж��Լ�����Ϣ�����Ƿ�ǿ�,�ǿ�ʱ�ŷ��������Ϣ
				// ע��,������!=null�ж�,!=nullʱ�����߶���������Ϣ
				if (!ll.isEmpty()) {
					// ������Ϣ�����е�ÿ����Ϣ"��Ϣ��Դ\n\n��Ϣ����"
					for (String s : ll) {
						// ���͸��ͻ���,�ɿͻ����Լ��Բ�ͬ����Դ����ͬ�Ĵ���
						dos.writeUTF(s);
					}
					// ������ɰ��Լ�����Ϣ�������
					ll.clear();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			// ֮����Ҫ�������Ź�ϣ�������Ϊ���sleep(),
			// �Ž���ϣ����������̵߳����ö�DealWithKernel�ɼ�,����������д�������interrupt()����߳�
			try {
				sleep(1000);// ����sleep()�÷�����CPUѹ����һЩ
			} catch (InterruptedException e) {
				// �������������,���׳��쳣
				// ����һ��ѭ���н��Է����Լ�����Ϣ������
			}
		}
	}
}
