package allServer;

import java.util.HashMap;
import java.util.LinkedList;

//����
public class Main {
	// �����еľ�̬��ϣӳ���,��¼��¼�ߵ�<�˺�,ip��ַ>ӳ��,���������ʱ���̼���
	// ��Ϊ������һ��,�Ʊ������û�����,�������ӳ��ֻҪ�����ڴ��������д�����ݿ�
	public static HashMap<String, String> hm_usrTOip = new HashMap<String, String>();
	// �ڶ��Ź�ϣ���¼<Ŀ���û��˺�,���͸����û�����ϢList>
	public static HashMap<String, LinkedList<String>> hm_usrTOmsg = new HashMap<String, LinkedList<String>>();

	public static void main(String[] args) {
		// [��½��������߳�]
		Thread thrd_lgn;

		// [��½��������߳�]��Ŀ�����
		LoginServer ls = new LoginServer();
		// [��½��������߳�]���̶߳���
		thrd_lgn = new Thread(ls);
		// ����[��½��������߳�]
		thrd_lgn.start();
	}

}
