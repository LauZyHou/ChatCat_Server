package allServer;

//�������н��������߳�,һ�����������½��,һ�����������������ͨ��
public class Main {

	public static void main(String[] args) {
		// [��½��������߳�],[����ͨ���߳�]
		Thread thrd_lgn, thrd_cht;

		// [��½��������߳�]��Ŀ�����
		LoginServer ls = new LoginServer();
		// [��½��������߳�]���̶߳���
		thrd_lgn = new Thread(ls);
		// ����[��½��������߳�]
		thrd_lgn.start();
	}

}
