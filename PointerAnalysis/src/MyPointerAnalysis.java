import java.io.File;

import soot.PackManager;
import soot.Transform;
import soot.toolkits.graph.DirectedGraph;


public class MyPointerAnalysis {
	static String codepath = "PointerAnalysis\\code";//"/Users/zqh/Downloads/project1/code";
	static String classname = "test.Hello";
	public static String mainClass;


	public static void main(String[] args) {
		//String jdkLibPath = System.getProperty("java.home")+"/lib/"; // "/usr/lib/jvm/java-7-openjdk-amd64/jre/lib/";
		if(args.length == 2) {
			codepath = args[0];
			classname = args[1];
		}
		mainClass = classname;
		String classpath = codepath
				+ File.pathSeparator + codepath + File.separator + "rt.jar"
				+ File.pathSeparator + codepath + File.separator + "jce.jar";
		//System.out.println(classpath);
		PackManager.v().getPack("wjtp").add(new Transform("wjtp.mypta", new WholeProgramTransformer()));
		soot.Main.main(new String[] {
			"-w",
			"-p", "cg.spark", "enabled:true",
			"-p", "wjtp.mypta", "enabled:true",
			"-soot-class-path", classpath,
				classname//args[1]
		});
	}

}
