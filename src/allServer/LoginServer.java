package allServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

//[��½��������߳�],3838�˿ڹ����¼����
public class LoginServer implements Runnable {
	// �����Socket,��������Socket����
	ServerSocket ss = null;
	// Socket���Ӷ���
	Socket sckt = null;
	// sql���Ӷ���
	java.sql.Connection con;
	// PreparedStatement��Statement������,Ҳ��һ��sql������
	java.sql.PreparedStatement ps;

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
			String uri = "jdbc:mysql://192.168.0.106:3306/ChatCatDB?useSSL=true&characterEncoding=utf8";
			String user = "root";// �û���
			String password = "3838438"; // ����
			// ��ָ�������ݿ⽨������(������Ϣ�ַ���,�û���,����)
			con = DriverManager.getConnection(uri, user, password);
			// ʹ��PreparedStatementʱ,�乹�췽����ֱ�Ӵ���Ҫִ�е�sql���,��ʱ��ȷ��ֵ�ĵط�ʹ���ʺ�
			// ����execute(),executeQuery()��executeUpdate()������Ҫ����
			ps = con.prepareStatement("SELECT * FROM SmplMsg WHERE UsrNum=? AND Passwd=?");
			// ��������Socket,�����ں���ѭ���н���Socket����
			ss = new ServerSocket(3838); // ��¼����ʼ��ʹ��3838�˿�,����д��ѭ������
			System.out.println("[#]��ʼ�ȴ��ͻ�������...");
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
				System.out.println("[+]�ͻ��˵�ַ:" + sckt.getInetAddress());
				// Ϊ����¿ͻ�����[��½�������߳�]����,����רΪ����ͻ�������߳�
				new IWannaLogin(sckt, ps).start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}// end run()
}

// [��½�������߳�]���߳�,ÿ���ͻ�����һ��
class IWannaLogin extends Thread {
	// �ӹ����������Socket���Ӷ���
	Socket sckt;
	// �������������
	DataInputStream dis;
	DataOutputStream dos;
	// ����ֵָʾ�Ƿ��¼�ɹ�,��ʼΪ��
	boolean success = false;
	// ���õ�sql������
	java.sql.PreparedStatement ps;
	// ��ѯ���صĽ����,ÿ�β�ѯ���ڸ�����ֵ
	ResultSet rs;

	// ������(Socket���Ӷ���,PreparedStatement���õ�sql������)
	IWannaLogin(Socket sckt, java.sql.PreparedStatement ps) {
		this.sckt = sckt;
		this.ps = ps;
	}

	@Override
	public void run() {
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
				String nm = str.substring(str.indexOf("]") + 1, str.indexOf("#"));
				String pswd = str.substring(str.indexOf("#") + 1);
				// PreparedStatement�����setString�����������ַ����е�n��"?"�ַ����滻
				ps.setString(1, nm);
				ps.setString(2, pswd);
				// ���ؽ����,ע��PreparedStatement�����ִ�з�������Ҫ����
				// ��Ϊsql������PreparedStatement������
				rs = ps.executeQuery();
				// �Է��صĽ�����Ĵ���,��ѯ����ǿ�ʱ˵��������ȷ,�����¼
				// ͬʱrs.next()����һ�е����˵�һ��
				// ʵ���ϲ�ѯ���ֻ����0�л�1��,��Ϊ������������ͬ���ܵ���
				// ����������UsrNum�ϾͲ�������ͬ
				if (rs.next() == true) {
					success = true;// �ǿ�,�����¼,��һ��while����ִ��
					String Name = rs.getString(3);// ��ȡ��(Ψһ��)�û����û���
					int HeadID = rs.getInt(4);// ͷ���ID��
					// ƴ���ַ���,���͸��ͻ���
					this.dos.writeUTF("[v]�û���:" + Name + "," + "ͷ��ID" + HeadID);
				}
				// �����Ϊ��,˵�����������ݿ���û�в�ѯ��ƥ����,��¼ʧ��
				else {
					// ���߿ͻ��˵�¼ʧ��
					this.dos.writeUTF("[x]�˻������������");
				}
			}
			// ��������,˵���ɹ���¼�˷�����
			System.out.println("[v]�ɹ���¼����:" + sckt.getInetAddress());
		} catch (IOException e) {
			// ���ͻ��˻�û�����¼��ť,�͹ر��˴��ڻ�����������ʽǿ�н����˿ͻ��˳���ʱ
			System.out.println("[-]������¼����:" + sckt.getInetAddress());
		} catch (SQLException e) {
			e.printStackTrace();// PreparedStatement�������
		}
	}
}
