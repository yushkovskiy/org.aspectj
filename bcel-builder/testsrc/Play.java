import java.io.File;
import java.io.FileInputStream;

import org.aspectj.apache.bcel.classfile.Attribute;
import org.aspectj.apache.bcel.classfile.ClassParser;
import org.aspectj.apache.bcel.classfile.Field;
import org.aspectj.apache.bcel.classfile.JavaClass;
import org.aspectj.apache.bcel.classfile.Method;
import org.aspectj.apache.bcel.classfile.Unknown;


public class Play {

  public static void printBytes(byte[] bs) {
    final StringBuilder sb = new StringBuilder("Bytes:" + bs.length + "[");
    for (int i = 0; i < bs.length; i++) {
      if (i > 0) sb.append(" ");
      sb.append(bs[i]);
    }
    sb.append("]");
    System.out.println(sb);
  }

  public static void main(String[] args) throws Exception {
    if (args == null || args.length == 0) {
      System.out.println("Specify a file");
      return;
    }
    if (!args[0].endsWith(".class")) {
      args[0] = args[0] + ".class";
    }
    final FileInputStream fis = new FileInputStream(new File(args[0]));
    final ClassParser cp = new ClassParser(fis, args[0]);
    final JavaClass jc = cp.parse();
    final Attribute[] attributes = jc.getAttributes();
    printUsefulAttributes(attributes);
    System.out.println("Fields");
    final Field[] fs = jc.getFields();
    if (fs != null) {
      for (Field f : fs) {
        System.out.println(f);
        printUsefulAttributes(f.getAttributes());
      }
    }
    System.out.println("Methods");
    final Method[] ms = jc.getMethods();
    if (ms != null) {
      for (Method m : ms) {
        System.out.println(m);
        printUsefulAttributes(m.getAttributes());
        System.out.println("Code attributes:");
        printUsefulAttributes(m.getCode().getAttributes());
      }
    }
//		Method[] ms = jc.getMethods();
//		for (Method m: ms) {
//			System.out.println("==========");
//			System.out.println("Method: "+m.getName()+" modifiers=0x"+Integer.toHexString(m.getModifiers()));
//			Attribute[] as = m.getAttributes();
//			for (Attribute a: as) {
//				if (a.getName().toLowerCase().contains("synthetic")) {
//					System.out.println("> "+a.getName());
//				}
//			}
//		}
  }

  private static void printUsefulAttributes(Attribute[] attributes) {
    for (Attribute attribute : attributes) {
      final String n = attribute.getName();
      if (n.equals("RuntimeInvisibleTypeAnnotations") ||
          n.equals("RuntimeVisibleTypeAnnotations")) {
        final Unknown unknown = (Unknown) attribute;
        final byte[] bs = unknown.getBytes();
        printBytes(bs);
      }
    }
  }
}
