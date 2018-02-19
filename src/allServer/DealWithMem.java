package allServer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
	// ����һ�����ݿ�����,��������ʹ��
	Connection con;

	// ������
	DealWithMem(Socket sckt, String nm) {
		// �����Ų�:Socket������˻���
		this.sckt = sckt;
		this.nm = nm;
		try {
			// ���췢����
			this.dos = new DataOutputStream(sckt.getOutputStream());
			// ����һ�����ݿ�����,��������ʹ��
			Class.forName("com.mysql.jdbc.Driver");
			String uri = "jdbc:mysql://192.168.0.106:3306/ChatCatDB?useSSL=true&characterEncoding=utf8";
			String user = "root";// �û���
			String password = "3838438"; // ����
			con = DriverManager.getConnection(uri, user, password);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// ��Ȼ�������˵����Ź�ϣ��,�������߳�ֻ������Լ���LinkedList,����synchronized
	@Override
	public void run() {
		while (true) {
			try {
				dealWithMsg();// �������Լ�����Ϣ
				dealWithAdd();// �������Լ��ĺ�������

				// ֮����Ҫ�������Ź�ϣ�������Ϊ���sleep(),
				// �Ž���ϣ����������̵߳����ö�DealWithKernel�ɼ�
				// ��Է�������д�������interrupt()����߳�
				sleep(1000);// ����sleep()�÷�����CPUѹ����һЩ
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				// �������������,���׳��쳣
				// ����һ��ѭ���н��Է����Լ�����Ϣ������
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} // end while
	}

	// �������Լ��ĺ�������,��Ϊ���ܲ�����ֹ�Լ��Ĺ�ϣ��,������synchronized
	private synchronized void dealWithAdd() throws SQLException {
		if (Main.hm_usrTOts.get(nm) == null)// ����û�˷������Լ���������
			return;// ֱ�ӽ���,�Լ���Ȩ����Լ��Ĺ�ϣ����,����ȱ��˷���������
		if (Main.hm_usrTOts.get(nm).isEmpty())// ��ʱû����Ҫ����ĺ�������
			return;// ֱ�ӽ���
		// �������TreeSet��,���ڼ�¼���������������
		for (String s : Main.hm_usrTOts.get(nm)) {
			// TODO
			// �����Լ���û��������������,(������ָ���쳣,�ö�·�ж�)
			if (Main.hm_usrTOts.get(s) != null && Main.hm_usrTOts.get(s).contains(nm)) {
				// ��Ȼ˫������Է���������,����ֱ�ӳ�Ϊ����
				int ANum = Integer.parseInt(nm);
				int BNum = Integer.parseInt(s);
				PreparedStatement ps;
				if (ANum < BNum)
					ps = con.prepareStatement("INSERT INTO FrndMsg VALUES (" + ANum + "," + BNum + ")");
				else
					ps = con.prepareStatement("INSERT INTO FrndMsg VALUES (" + BNum + "," + ANum + ")");
				int ok = ps.executeUpdate();// �ɹ���Ϊ���ѽ�1����Ӱ��
				if (ok != 1)// ��������������
					System.out.println("[!]�Զ���Ϊ���ѳ�����");
			}
		}
	}

	// �������Լ�����Ϣ,ÿ���û�ֻ�����Լ���,����synchronized
	private void dealWithMsg() throws IOException {
		// �ӵڶ��Ź�ϣ�����ó��Լ����Ƿ���Ϣ����
		LinkedList<String> ll = Main.hm_usrTOmsg.get(nm);
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
	}
}
