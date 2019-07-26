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
		MyForwardFlow flowAnalysis = new MyForwardFlow(graph, new PointToSet(), sm.getName(), "main");
		queries = flowAnalysis.getQueries();
		flowAnalysis.dumpQueries(queries);
		String answer = flowAnalysis.generateAnswer();
		AnswerPrinter.printAnswer(answer);
	}
}