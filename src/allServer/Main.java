package allServer;

import java.util.HashMap;

//����
public class Main {
	// �����еľ�̬��ϣӳ���,��¼��¼�ߵ�<�˺�,ip��ַ>ӳ��,���������ʱ���̼���
	// ��Ϊ������һ��,�Ʊ������û�����,�������ӳ��ֻҪ�����ڴ��������д�����ݿ�
	public static HashMap<String, String> hm_usrTOip = new HashMap<String, String>();

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
