package allServer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

//������,���һЩ�Զ���ķ���
public class MyTools {
	// ���ַ���str�е�k�γ����Ӵ�s��λ��,ʹ������
	public static int indexOf(String str, int k, String s) {
		Matcher slashMatcher = Pattern.compile(s).matcher(str);
		int mIdx = 0;
		while (slashMatcher.find()) {
			mIdx++;
			if (mIdx == k)// ���ȷʵ���ҵ���k��
				return slashMatcher.start();
		}
		// �Ҳ�����k��ʱ�򷵻�-1
		return -1;
	}
}
