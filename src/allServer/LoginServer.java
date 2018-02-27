package allServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;

//[��½��������߳�],3838�˿ڹ����¼����
public class LoginServer implements Runnable {
	// �����Socket,��������Socket����
	ServerSocket ss = null;
	// Socket���Ӷ���
	Socket sckt = null;
	// sql���Ӷ���
	java.sql.Connection con;
	// PreparedStatement��Statement������,Ҳ��һ��sql������
	java.sql.PreparedStatement ps_smpl;// ��ѯSmplMsg��,������֤��¼
	java.sql.PreparedStatement ps_frnd;// ��ѯFrndMsg��,�����ڵ�¼���ṩ�����б�
	java.sql.PreparedStatement ps_prmy_smpl;// ֻ������(�˺�)��ѯSmplMsg��,���ڻ�ȡ������Ϣ

	@Override
	public void run() {
		// ׼������:����MySQL����,׼��sql������,׼���󶨶˿ڵ�ServerSocket����
		try {
			// JVM����JDBC����
			Class.forName("com.mysql.jdbc.Driver");
			// ������Ϣ�ַ���,MySQL���ݿ��������Ĭ�϶˿ں���3306
			// jdbc:mysql://ip��ַ:�˿ں�/Ҫ���ӵ����ݿ���?����ѡ��
			// useSSL����ָ������ͨ���Ƿ�Ҫ���ܴ���
			// characterEncoding����ָ�����ӱ���,Ҫ�����ݿ����,���ݿ��ı���,���ݿ�ϵͳ�ı���һ��
			String uri = "jdbc:mysql://" + Main.DBIp + ":3306/ChatCatDB?useSSL=true&characterEncoding=utf8";
			String user = "root";// �û���
			String password = "3838438"; // ����
			// ��ָ�������ݿ⽨������(������Ϣ�ַ���,�û���,����)
			con = DriverManager.getConnection(uri, user, password);
			// ʹ��PreparedStatementʱ,�乹�췽����ֱ�Ӵ���Ҫִ�е�sql���,��ʱ��ȷ��ֵ�ĵط�ʹ���ʺ�
			// ����execute(),executeQuery()��executeUpdate()������Ҫ����
			ps_smpl = con.prepareStatement("SELECT * FROM SmplMsg WHERE UsrNum=? AND Passwd=?");
			ps_frnd = con.prepareStatement(
					"SELECT Usr1 FROM FrndMsg WHERE Usr2=? " + "UNION " + "SELECT Usr2 FROM FrndMsg WHERE Usr1=?");
			ps_prmy_smpl = con.prepareStatement("SELECT * FROM SmplMsg WHERE UsrNum=?");
			// ��������Socket,�����ں���ѭ���н���Socket����
			ss = new ServerSocket(3838); // ��¼����ʼ��ʹ��3838�˿�,����д��ѭ������
			System.out.println("[��¼��������߳�]����...");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();// JVM����JDBC����ʧ��
		} catch (SQLException e) {
			e.printStackTrace();// ��sql���Ӷ����sql�������йص��쳣
		} catch (IOException e) {
			e.printStackTrace();// ServerSocket�Ըö˿�ʵ����ʧ��
		}
		// �����[��½��������߳�]��,��ͣ�ؼ����ͻ��˷����ĵ�¼����
		while (true) {
			try {
				sckt = ss.accept();// �����Եȴ�����
				System.out.println("[+]���Ե�¼����:" + sckt.getInetAddress());
				// Ϊ����¿ͻ�����[��½�������߳�]����,����רΪ����ͻ�������߳�
				new IWannaLogin(sckt, ps_smpl, ps_frnd, ps_prmy_smpl).start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}// end run()
}

// [��½�������߳�],ÿ���ͻ�����һ��
class IWannaLogin extends Thread {
	// �ӹ����������Socket���Ӷ���
	Socket sckt;
	// �������������
	DataInputStream dis;
	DataOutputStream dos;
	// ����ֵָʾ�Ƿ��¼�ɹ�,��ʼΪ��
	boolean success = false;
	// ���õ�sql������
	java.sql.PreparedStatement ps_smpl;
	java.sql.PreparedStatement ps_frnd;
	java.sql.PreparedStatement ps_prmy_smpl;
	// ��ѯ���صĽ����,ÿ�β�ѯ���ڸ�����ֵ
	ResultSet rs;

	// ������(Socket���Ӷ���,PreparedStatement���õ�sql������)
	IWannaLogin(Socket sckt, java.sql.PreparedStatement ps_smpl, java.sql.PreparedStatement ps_frnd,
			java.sql.PreparedStatement ps_prmy_smpl) {
		this.sckt = sckt;
		this.ps_smpl = ps_smpl;// ��֤��¼
		this.ps_frnd = ps_frnd;// �����б�
		this.ps_prmy_smpl = ps_prmy_smpl;// ����������ѯ(������Ϣ)
	}

	// [��½�������߳�]����
	@Override
	public void run() {
		String nm = null;// �˻���
		String pswd = null;// ����
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
					return;// ����Է�Socket��������Ϣ��ʽ����,������Ӿ��Ǵ���Σ�յ�,ֱ�ӽ������߳�
				// ��ȡ���˻���������
				nm = str.substring(str.indexOf("]") + 1, str.indexOf("#"));
				pswd = str.substring(str.indexOf("#") + 1);
				// PreparedStatement�����setString�����������ַ����е�n��"?"�ַ����滻
				ps_smpl.setString(1, nm);
				ps_smpl.setString(2, pswd);
				// ���ؽ����,ע��PreparedStatement�����ִ�з�������Ҫ����
				// ��Ϊsql������PreparedStatement������
				rs = ps_smpl.executeQuery();
				// �Է��صĽ�����Ĵ���,��ѯ����ǿ�ʱ˵��������ȷ,�����¼
				// ͬʱrs.next()����һ�е����˵�һ��
				// ʵ���ϲ�ѯ���ֻ����0�л�1��,��Ϊ������������ͬ���ܵ���
				// ����������UsrNum�ϾͲ�������ͬ
				if (rs.next() == true) {
					success = true;// �ǿ�,�����¼,��һ��while����ִ��
					String Name = rs.getString(3);// ��ȡ��(Ψһ��)�û����û���
					int HeadID = rs.getInt(4);// ͷ���ID��
					String str_send = "[v]#�û���:" + Name + "#ͷ��ID:" + HeadID;// ƴ��Ҫ���͵��ַ���
					// ��������ȷʱ,�������if����,�Ժ��ѽ��в�ѯ,���غ����б�
					ps_frnd.setString(1, nm);// �滻'?'
					ps_frnd.setString(2, nm);// �滻'?'
					ResultSet rs_frnd = ps_frnd.executeQuery();// ִ�в�ѯ
					// ������������б��е�ÿһ��ȥ��ѯSmplMsg��,�õ����ѵ�(�˺�,����,ͷ��ID)ͶӰ
					ResultSet rs_prmy_smpl = null;// �ݴ�ÿ�εĲ�ѯ���
					while (rs_frnd.next() == true) {
						int frndUsrNum = rs_frnd.getInt(1);// �Ӻ����б���ֻ�ܵõ��ú��ѵ��˺�
						ps_prmy_smpl.setString(1, "" + frndUsrNum);// ����������滻'?'��
						rs_prmy_smpl = ps_prmy_smpl.executeQuery();// ִ�в�ѯ�õ������
						// ȷ������,ֻҪ���ݿ�����ĺ�����if����ȥ��!
						if (rs_prmy_smpl.next() == true) {
							String frndName = rs_prmy_smpl.getString(3);// ��3��:�ú��ѵ�����
							int frndHeadID = rs_prmy_smpl.getInt(4);// ��4��:�ú��ѵ�ͷ��ID
							str_send = str_send + "#" + frndUsrNum + "," + frndName + "," + frndHeadID;// ƴ��Ҫ���͵��ַ���
						}
					}
					// �����ƴ������Ҫ���͵��ַ������͸��ͻ���
					this.dos.writeUTF(str_send);
				}
				// �����Ϊ��,˵�����������ݿ���û�в�ѯ��ƥ����,��¼ʧ��
				else {
					// ���߿ͻ��˵�¼ʧ��
					this.dos.writeUTF("[x]�˻������������");
				}
			}
			// ��������,˵���ɹ���¼�˷�����,ҪΪ���û��ķ�����һЩ׼������
			System.out.println("[v]�ɹ���¼����:" + sckt.getInetAddress());
			// 1:������״̬��¼�������HashMap��,ip��ַҪȥ��ͷ����б��'/'
			Main.hm_usrTOip.put(nm, sckt.getInetAddress().toString().substring(1));
			System.out.println("[v]��ǰ����״̬:" + Main.hm_usrTOip);
			// 2:�ڽ�����Ϣ�õ������HashMap��������һ��,�½�һ��������Ϣ����
			// ������־:��ʵ�ֶԲ����ߵ��û�����Ϣʱ,Ҫ�����ﴴ�����û�����Ϣ����
			// ��������Ҫ�ж������Ƿ��Ѿ�����,û�д���(����ȫ��û�˸��Լ�����Ϣ)ʱ�Ŵ���
			if (Main.hm_usrTOmsg.get(nm) == null) {
				Main.hm_usrTOmsg.put(nm, new LinkedList<String>());
			}
			// 3:��ȡ���ڴ滺����Ϣ�õ������HashMap��������һ��
			DealWithMem dwm = new DealWithMem(sckt, nm);// �½�һ��[�ڴ���Ϣ�����߳�]���߳�
			dwm.start();// ����
			Main.hm_usrTOthrd.put(nm, dwm);// �����ô���
			// 4:��[��½�������߳�]���߳̽���֮ǰ,Ϊ����û��¿�һ��[�ͻ���Ϣ�����߳�]���߳�
			// �������Ӻõ�Socket����,�Ա��������֤��¼�ɹ���TCP����
			// �����¼�ߵ��˻���,���ڿͻ��˹ر�ʱ�Ͽ�����ǰ�ӹ�ϣ����ȥ����Ӧ����Ŀ
			new DealWithKernel(sckt, nm).start();
		} catch (IOException e) {
			// ���ͻ��˻�û�ɹ���¼,�͹ر��˴��ڻ�����������ʽǿ�н����˿ͻ��˳���ʱ
			System.out.println("[-]������¼����:" + sckt.getInetAddress());
		} catch (SQLException e) {
			e.printStackTrace();// PreparedStatement�������
		}
	}
}
