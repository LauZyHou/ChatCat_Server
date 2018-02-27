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
import java.util.TreeSet;

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
			String uri = "jdbc:mysql://" + Main.DBIp + ":3306/ChatCatDB?useSSL=true&characterEncoding=utf8";
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
				// dos.writeUTF("[��������]���յ���!");// ���Ի�Ӧ
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
				// �ͻ���Ӻ���ʱ��Ҫ�����û�����
				else if (s.startsWith("[addFrnd_")) {
					dealFindMsg(s);// �����������
				}
				// �ͻ�Ҫ���ָ���˺ŵĺ���
				else if (s.startsWith("[add]")) {
					dealAdd(s);// �������ָ���ĺ���
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

	// �������ָ���˻��ĺ���,synchronized��֤����TreeSet��˳��,��������TreeSet�Ľ���
	private synchronized void dealAdd(String s) {
		// ������������
		int myNum = Integer.parseInt(nm);
		int toNum = Integer.parseInt(s.substring(s.indexOf("]") + 1));
		PreparedStatement ps = null;
		try {
			if (myNum < toNum) {
				// ���ﲻ��'*'�����ǽ�ʡ����'*'����һ���ʱ��
				ps = con.prepareStatement("SELECT Usr1 FROM FrndMsg WHERE Usr1=" + myNum + " AND Usr2=" + toNum);
			} else {
				ps = con.prepareStatement("SELECT Usr1 FROM FrndMsg WHERE Usr1=" + toNum + " AND Usr2=" + myNum);
			}
			ResultSet rs = ps.executeQuery();
			// ����ֻ��һ��
			if (rs.next()) {
				dos.writeUTF("[youradd][x]" + toNum + "�Ѿ�����ĺ���");
			} else {
				dos.writeUTF("[youradd][v]" + "�����ѷ���");
				// δ����ʱ�Ƚ���,��֮�浽��Ӧ�û���TreeSet��
				// ע��!ÿ���û����߳���Ȩ�������ϣ���ｨ���Լ�����!ֻ��ͨ�����˷����Լ�����������
				// ��DealWithMem�н����Լ����������Ե��жϡ�ȡ����ʹ��
				if (Main.hm_usrTOts.get("" + toNum) == null) {
					Main.hm_usrTOts.put("" + toNum, new TreeSet<String>());
				}
				Main.hm_usrTOts.get("" + toNum).add("" + myNum);
				System.out.println(Main.hm_usrTOts);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// ��������û�ʱ�Ĳ�������,ֻ�������˺�,�ǳ�,�Ա�
	private void dealFindMsg(String s) {
		// ���˺Ų���
		if (s.startsWith("[addFrnd_U]")) {
			String UsrNum = s.substring(s.indexOf("]") + 1);
			try {
				PreparedStatement ps = con
						.prepareStatement("SELECT UsrNum,Name,Sex From SmplMsg WHERE UsrNum=" + UsrNum);
				ResultSet rs = ps.executeQuery();
				// ����ֻ��һ��
				if (rs.next()) {
					dos.writeUTF("[addFrnd]" + rs.getString(1) + "#" + rs.getString(2) + "#" + rs.getString(3) + "##");
				} else {
					dos.writeUTF("[addFrnd_None]");
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// ���ǳƲ���
		else if (s.startsWith("[addFrnd_N]")) {
			String Name = s.substring(s.indexOf("]") + 1);
			try {
				// ǰ������%,����ƥ��
				PreparedStatement ps = con
						.prepareStatement("SELECT UsrNum,Name,Sex From SmplMsg WHERE Name LIKE '%" + Name + "%'");
				ResultSet rs = ps.executeQuery();
				// �п��ܲ�ֹһ��,��һ������ֵ��¼
				boolean have = false;
				// ����ƴ�Ӷ�����
				String str_rst = "";
				while (rs.next()) {
					have = true;
					// ÿ��ƴ�ӽ��������"#"
					str_rst = str_rst + rs.getString(1) + "#" + rs.getString(2) + "#" + rs.getString(3) + "##";
				}
				if (have) {
					dos.writeUTF("[addFrnd]" + str_rst);
				} else {
					dos.writeUTF("[addFrnd_None]");
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
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
