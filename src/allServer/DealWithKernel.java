package allServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
	// ����һ�����ݿ�����,��������ʹ��
	Connection con;

	// ������
	public DealWithKernel(Socket sckt, String nm) {
		// �����Ų�:Socket������˻���
		this.sckt = sckt;
		this.nm = nm;
		try {
			// �ؽ����������
			dis = new DataInputStream(sckt.getInputStream());
			dos = new DataOutputStream(sckt.getOutputStream());
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
				// �ͻ�Ҫ��ȡ�Լ������Ͽ�
				else if (s.equals("[mycard]")) {
					dealGetCard();// �����ȡ���Ͽ�
				}
				// �ͻ�Ҫ���µĸ�������д�����ݿ�
				else if (s.startsWith("[changecard]")) {
					dealChngCard(s);// �����޸ĸ�������
				}
			}
		} catch (IOException e) {
			// �ڵ�һ�Ź�ϣ����ȥ������û�,��ʾ����û��Ѿ�������
			Main.hm_usrTOip.remove(nm);
			// �ӵ����Ź�ϣ�����ҵ�����̵߳�����,������ֹ��!
			Main.hm_usrTOthrd.get(nm).stop();
			// �ڵ����Ź�ϣ����ȥ������û�,��ʾ����û��Ѿ�������
			Main.hm_usrTOthrd.remove(nm);
			// �ÿͻ��˹ر�ʱ�ᷢ�����쳣
			System.out.println("[-]" + nm + sckt.getInetAddress() + "�Ͽ�����");
		}
	}

	// �����޸ĸ�������,������ִ��sql���֮�以��Ӱ��,����synchronized����
	private void dealChngCard(String s) {
		String Name = s.substring(s.indexOf("]") + 1, s.indexOf("#"));
		String HeadID = s.substring(s.indexOf("#") + 1, MyTools.indexOf(s, 2, "#"));
		String Sex = s.substring(MyTools.indexOf(s, 2, "#") + 1, s.lastIndexOf("#"));
		String Signature = s.substring(s.lastIndexOf("#") + 1);
		try {
			// �������ݿ��
			PreparedStatement ps_smpl = con.prepareStatement("UPDATE SmplMsg SET Name='" + Name + "',HeadID=" + HeadID
					+ ",Sex=" + Sex + ",Signature='" + Signature + "' WHERE UsrNum=" + nm);
			int rs = ps_smpl.executeUpdate();
			if (rs == 1) {
				dos.writeUTF("[changecard]success");// �ɹ���Ϣ
			} else {
				dos.writeUTF("[changecard]failed");// ʧ����Ϣ
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// �����ȡ���Ͽ�
	private void dealGetCard() {
		try {
			PreparedStatement ps_smpl = con.prepareStatement("SELECT * FROM SmplMsg WHERE UsrNum=" + nm);
			ResultSet rs = ps_smpl.executeQuery();
			if (rs.next()) {
				String Name = rs.getString(3);// ��ȡ��(Ψһ��)�û����û���
				int HeadID = rs.getInt(4);// ͷ���ID��
				int Sex = rs.getInt(5);// �Ա�0Ů1��
				String Signature = rs.getString(6);// ����ǩ��
				// ƴ���ַ�������
				String send = "[mycard]" + Name + "#" + HeadID + "#" + Sex + "#" + Signature;
				dos.writeUTF(send);
			} else {
				// TODO �ƺ����ᷢ���������
				System.out.println("[!]�������������");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
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
		// System.out.println("[1��]" + Main.hm_usrTOip);// �������
		// System.out.println("[2��]" + Main.hm_usrTOmsg);// �������
		// System.out.println("[3��]" + Main.hm_usrTOthrd);// �������
	}
}
