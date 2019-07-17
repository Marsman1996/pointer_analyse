import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.util.queue.QueueReader;

public class WholeProgramTransformer extends SceneTransformer {
	@Override
	protected void internalTransform(String arg0, Map<String, String> arg1) {
		TreeMap<Integer, ObjectInfo> queries = new TreeMap<Integer, ObjectInfo>();
		SootClass mainClass =  Scene.v().getSootClass(MyPointerAnalysis.mainClass);
		SootMethod sm = mainClass.getMethodByName("main");
		UnitGraph graph = new ExceptionalUnitGraph(sm.retrieveActiveBody());
		MyFlowAnalysis flowAnalysis = new MyFlowAnalysis(graph);
		queries = flowAnalysis.getTotalQueries();
		flowAnalysis.dumpQueries(queries);
		String answer = flowAnalysis.generateAnswer();
		AnswerPrinter.printAnswer(answer);
	}
}



// 下面的没什么用，可以删掉，是写Anderson分析的时候写的。
// 下面的没什么用，可以删掉，是写Anderson分析的时候写的。
class WholeProgramTransformerAnderson extends SceneTransformer {
	TreeMap<Integer, String> queries = new TreeMap<Integer, String>();
	Anderson anderson = new Anderson();
	String emptyStr = new String();

	@Override
	protected void internalTransform(String arg0, Map<String, String> arg1) {

		ReachableMethods reachableMethods = Scene.v().getReachableMethods();
		QueueReader<MethodOrMethodContext> qr = reachableMethods.listener();
		while (qr.hasNext()) {
			SootMethod sm = qr.next().method();

			if (!sm.getName().equals("main")) {
				continue;
			}
			analyzeMethod(sm);
			System.out.println(sm);
			break;
		}

		anderson.showNewConstraintList();
		anderson.showAssignConstraintList();
		anderson.run();
		anderson.showPTS();

		String answer = "";
		for (Entry<Integer, String> q : queries.entrySet()) {
			TreeSet<Integer> result = anderson.getPointsToSet(q.getValue());
			answer += q.getKey().toString() + ":";
			if (result.isEmpty()) {
				answer += "\n";
				continue;
			}

			for (Integer i : result) {
				answer += " " + i;
			}
			answer += "\n";
		}
		System.out.println(answer);
		AnswerPrinter.printAnswer(answer);
	}
    String generateName(String methodname, String valuename) {
		return methodname + "_" + valuename;
	}
	void analyzeMethod(SootMethod sm) {
		String methodname = sm.toString();
		int allocId = 0;
		if (sm.hasActiveBody()) {
			for (Unit u : sm.getActiveBody().getUnits()) {
				System.out.print("----------------------------\n");
				System.out.println(u.getClass());
				System.out.println("S: " + u);
				if (u instanceof InvokeStmt) {
					InvokeExpr ie = ((InvokeStmt) u).getInvokeExpr();
					if (ie.getMethod().toString().equals("<benchmark.internal.Benchmark: void alloc(int)>")) {
						System.out.println("Invoke*****alloc\n");
						allocId = ((IntConstant)ie.getArgs().get(0)).value;
						System.out.println("allocID = " + allocId);
					}
					if (ie.getMethod().toString().equals("<benchmark.internal.Benchmark: void test(int,java.lang.Object)>")) {
						System.out.println("Invoke*****test\n");
						Value v = ie.getArgs().get(1);
						int id = ((IntConstant)ie.getArgs().get(0)).value;
						System.out.println(id + "," + v.toString());
						queries.put(id, generateName(methodname, v.toString()));
					}

				}
				if (u instanceof DefinitionStmt) {
					if (((DefinitionStmt)u).getRightOp() instanceof NewExpr) {
						System.out.println("DefinitionStmt***NewExpr\n");
						anderson.addNewConstraint(allocId, generateName(methodname, ((DefinitionStmt) u).getLeftOp().toString()));
					}
					if (((DefinitionStmt)u).getLeftOp() instanceof Local && ((DefinitionStmt)u).getRightOp() instanceof Local) {
						System.out.println("DefinitionStmt***local_local\n");
						anderson.addAssignConstraint(generateName(methodname, ((DefinitionStmt) u).getRightOp().toString()), generateName(methodname, ((DefinitionStmt) u).getLeftOp().toString()));
						System.out.println(((DefinitionStmt) u).getRightOp().toString());
						System.out.println((((DefinitionStmt) u).getLeftOp().toString()));

					}
					if (((DefinitionStmt)u).getLeftOp() instanceof InstanceFieldRef && ((DefinitionStmt)u).getRightOp() instanceof Local) {
						System.out.println("DefinitionStmt***local_field\n");
						anderson.addAssignConstraint(generateName(methodname, ((DefinitionStmt) u).getRightOp().toString()), generateName(methodname, ((DefinitionStmt) u).getLeftOp().toString()));
						System.out.println(((DefinitionStmt) u).getRightOp().toString());
						System.out.println((((DefinitionStmt) u).getLeftOp().toString()));
					}
					if (((DefinitionStmt)u).getLeftOp() instanceof Local && ((DefinitionStmt)u).getRightOp() instanceof InstanceFieldRef) {
						System.out.println("Hello***field_local\n");
						anderson.addAssignConstraint(generateName(methodname, ((DefinitionStmt) u).getRightOp().toString()), generateName(methodname, ((DefinitionStmt) u).getLeftOp().toString()));
						System.out.println(((DefinitionStmt) u).getRightOp().toString());
						System.out.println((((DefinitionStmt) u).getLeftOp().toString()));
					}
				}
			}
		}
	}

}