package allServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

//[ע����������߳�],3939�˿ڹ���ע������
public class SignServer implements Runnable {
	// �����Socket,��������Socket����
	ServerSocket ss = null;
	// Socket���Ӷ���
	Socket sckt = null;
	// sql���Ӷ���
	java.sql.Connection con;
	// PreparedStatement��Statement������,Ҳ��һ��sql������
	java.sql.PreparedStatement ps_smpl;// ��ѯSmplMsg��,����ȷ���˺Ų��ظ�
	java.sql.PreparedStatement ps_insrt;// ����SmplMsg��,���ڳɹ�ע��

	// ������
	SignServer() {
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
			ps_smpl = con.prepareStatement("SELECT * FROM SmplMsg WHERE UsrNum=?");// ���
			ps_insrt = con.prepareStatement("INSERT INTO SmplMsg VALUES (?,?,?,?)");// ����
			// ��������Socket,�����ں���ѭ���н���Socket����
			ss = new ServerSocket(3939); // ע�����ʹ��3939�˿�
			System.out.println("[ע����������߳�]����...");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();// JVM����JDBC����ʧ��
		} catch (SQLException e) {
			e.printStackTrace();// ��sql���Ӷ����sql�������йص��쳣
		} catch (IOException e) {
			e.printStackTrace();// ServerSocket�Ըö˿�ʵ����ʧ��
		}
	}

	@Override
	public void run() {
		// �����[ע����������߳�]��,��ͣ�ؼ����ͻ��˷�����ע������
		while (true) {
			try {
				sckt = ss.accept();// �����Եȴ�����
				System.out.println("[+]����ע������:" + sckt.getInetAddress());
				// Ϊ����¿ͻ�����[ע���������߳�]����,����רΪ����ͻ�������߳�
				new IWannaSignUp(sckt, ps_smpl, ps_insrt).start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}

// [ע���������߳�],ÿ��ע���ߵ���һ��
class IWannaSignUp extends Thread {
	// �ӹ����������Socket���Ӷ���
	Socket sckt;
	// �������������
	DataInputStream dis;
	DataOutputStream dos;
	// ����ֵָʾ�Ƿ�ע��ɹ�,��ʼΪ��
	boolean success = false;
	// ���õ�sql������
	java.sql.PreparedStatement ps_smpl;// ����ظ�
	java.sql.PreparedStatement ps_insrt;// �ɹ�ע��ʱ�������ݿ�
	// ��ѯ���صĽ����,ÿ�β�ѯ���ڸ�����ֵ
	ResultSet rs;

	// ������(Socket���Ӷ���,PreparedStatement���õ�sql������)
	IWannaSignUp(Socket sckt, java.sql.PreparedStatement ps_smpl, java.sql.PreparedStatement ps_insrt) {
		this.sckt = sckt;
		this.ps_smpl = ps_smpl;// ����ظ�
		this.ps_insrt = ps_insrt;// �ɹ�ע��ʱ�������ݿ�
	}

	@Override
	public void run() {
		String UsrNum = null;// �˻���
		String Passwd = null;// ����
		String Name = null;// ����
		String HeadID = null;// ͷ�����

		try {
			// ��Socket���Ӷ���ȥ��ʼ�����������,������ͻ��������������
			this.dis = new DataInputStream(sckt.getInputStream());
			this.dos = new DataOutputStream(sckt.getOutputStream());
			// û��ע��ɹ�ʱ,��ͣ����(�û����Բ��ϳ���ע��)
			while (success == false) {
				// �������������û���������Ϣ
				String str = dis.readUTF();// ��������֮��
				// ȷ������Ϣ��ʽ,��"[sign]"��ͷ����Ҫע��
				if (str.startsWith("[sign]") == false)
					return;// ����Է�Socket��������Ϣ��ʽ����,������Ӿ��Ǵ���Σ�յ�,ֱ�ӽ������߳�
				// ������Ϣ:�õ��˺�,����,����,ͷ�����
				UsrNum = str.substring(str.indexOf("]") + 1, str.indexOf("#"));
				Passwd = str.substring(str.indexOf("#") + 1, MyTools.indexOf(str, 2, "#"));
				Name = str.substring(MyTools.indexOf(str, 2, "#") + 1, str.lastIndexOf("#"));
				HeadID = str.substring(str.lastIndexOf("#") + 1);
				// ���˺���һ���ж�
				if (Integer.parseInt(UsrNum) > 30000 || Integer.parseInt(UsrNum) < 10000) {
					this.dos.writeUTF("[x]�˺�����10000��30000֮��");
					continue;
				}
				// PreparedStatement�����setString�����������ַ����е�n��"?"�ַ����滻
				ps_smpl.setString(1, UsrNum);// �����ò���ظ���sql������
				// ���ؽ����,ע��PreparedStatement�����ִ�з�������Ҫ����
				// ��Ϊsql������PreparedStatement������
				rs = ps_smpl.executeQuery();
				// �Է��صĽ�����Ĵ���,��ѯ����ǿ�ʱ˵���˻����Ѿ�����,������ע��
				// ʵ���ϲ�ѯ���ֻ����0�л�1��,��Ϊ������������ͬ�˺ŵ���
				if (rs.next() == true) {
					this.dos.writeUTF("[x]���˺������ݿ����Ѵ���");
				}
				// �����Ϊ��,˵������˻��ǿ��õ�,����ע��,���Բ������ݿ����
				else {
					// Ϊ�����õ�sql��������ĸ�'?'�滻
					ps_insrt.setString(1, UsrNum);
					ps_insrt.setString(2, Passwd);
					ps_insrt.setString(3, Name);
					ps_insrt.setString(4, HeadID);
					// ִ���������,�����ݿ�SmplMsg����������û��ṩ��ע����Ϣ
					int ok = ps_insrt.executeUpdate();// ע��ʹ�õ���executeUpdate()����
					// ���ص���������Ӱ�������,����Ϊ0(����1)ʱ,˵������ɹ�,��ע��ɹ�
					if (ok == 1) {
						success = true;// ��¼,���˳�while
						this.dos.writeUTF("[v]ע��ɹ�");// ���߿ͻ���ע��ɹ�
					} else {
						this.dos.writeUTF("[x]��Ϊ����˻����ݿ�����ע��ʧ��");
					}
				}
			}
			// ��������,˵��ע��ɹ�
			System.out.println("[v]�ɹ�ע������:" + sckt.getInetAddress());
		} catch (IOException e) {
			// ���ͻ��˻�û�ɹ�ע��,�͹ر��˴��ڻ�����������ʽǿ�н����˿ͻ��˳���ʱ�������쳣
			System.out.println("[-]����ע������:" + sckt.getInetAddress());
		} catch (SQLException e) {
			e.printStackTrace();// PreparedStatement�������
		}
	}
}
