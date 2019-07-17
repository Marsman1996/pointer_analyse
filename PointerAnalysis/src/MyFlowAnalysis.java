import org.omg.CORBA.OBJ_ADAPTER;
import soot.*;
import soot.jimple.*;
import soot.options.Options;
import soot.toolkits.graph.DominatorsFinder;
import soot.toolkits.graph.MHGDominatorsFinder;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.awt.*;
import java.util.*;
import java.util.List;

public class MyFlowAnalysis
{
	TreeMap<Integer, ObjectInfo> totalQueries;
	PointToSet outputSet;

	public MyFlowAnalysis(UnitGraph graph)
	{

		MyForwardFlow analysis = new MyForwardFlow(graph, new PointToSet());
		totalQueries = analysis.getQueries();
		// build map
		outputSet = analysis.outputSet;
	}

	PointToSet getOutputSet() {
		return outputSet;
	}

	TreeMap<Integer, ObjectInfo> getTotalQueries() {
		return totalQueries;
	}

	String generateAnswer() {
		String answer = "";
		for (Map.Entry<Integer, ObjectInfo> q : totalQueries.entrySet()) {
			Set<ObjectInfo> resultSet = outputSet.getKey(q.getValue());
			//TreeSet<Integer> result = anderson.getPointsToSet(q.getValue());
			answer += q.getKey().toString() + ":";
			if (resultSet.isEmpty()) {
				answer += " \n";
				continue;
			}

			Iterator entries = resultSet.iterator();
			while(entries.hasNext()){
				ObjectInfo value = (ObjectInfo)entries.next();
				answer += " " + value.allocID;
			}
			answer += "\n";
		}
		System.out.println(answer);
		return answer;
	}

	static void dumpQueries(TreeMap<Integer, ObjectInfo> q) {
		for (Map.Entry<Integer, ObjectInfo> entry : q.entrySet()) {
			System.out.println("ID: " + entry.getKey());
			entry.getValue().dumpObjectInfo();
		}
	}
}

class MyForwardFlow extends ForwardFlowAnalysis
{

	//ArraySparseSet<PointToSet> emptySet = new ArraySparseSet<PointToSet>();
	PointToSet inputSet = new PointToSet();
	PointToSet outputSet = new PointToSet();
	UnitGraph graph;
	Integer allocID;
	TreeMap<Integer, ObjectInfo> queries = new TreeMap<Integer, ObjectInfo>();

	MyForwardFlow(UnitGraph graph, PointToSet inputSet) {
		super(graph);
		this.graph = graph;
		inputSet.copy(inputSet);
		doAnalysis();
	}

	TreeMap<Integer, ObjectInfo> getQueries() {
		return queries;
	}



	public void showUnit(UnitGraph graph) {
		Map<String, Set<String>> r;
		Iterator unitIt = graph.iterator();
		while (unitIt.hasNext()) {
			System.out.println(unitIt);
		}
	}

	/**
	 * All INs are initialized to the empty set.
	 **/
	protected Object newInitialFlow() {
		return inputSet.deepClone();
	}

	/**
	 * IN(Start) is the empty set
	 **/
	protected Object entryInitialFlow() {
		return inputSet.deepClone();
	}

	/**
	 * OUT is the same as IN plus the genSet.
	 **/
	protected void flowThrough(Object inValue, Object unit, Object outValue) {
		PointToSet
				in = (PointToSet) inValue,
				out = (PointToSet) outValue;
		in.copy(out);


		Unit u = (Unit) unit;

		System.out.println(unit);
		if (u instanceof InvokeStmt) { // TODO: 主要在这里补充，估计还需要补充200行左右
			InvokeExpr ie = ((InvokeStmt) u).getInvokeExpr();
			if (ie.getMethod().toString().equals("<benchmark.internal.Benchmark: void alloc(int)>")) {
				// System.out.println("Invoke*****alloc\n");
				allocID = ((IntConstant)ie.getArgs().get(0)).value;
				System.out.println("allocID = " + allocID);
			}
			if (ie.getMethod().toString().equals("<benchmark.internal.Benchmark: void test(int,java.lang.Object)>")) {
				//System.out.println("Invoke*****test\n");
				Value v = ie.getArgs().get(1);
				int id = ((IntConstant)ie.getArgs().get(0)).value;
				ObjectInfo o = new ObjectInfo(0, v.toString(), "default", "main");
				queries.put(id, o);
				System.out.println("test invoke" + ": " + id + "," + v.toString());
			}
			// TODO: 处理函数调用。如果不是上述两种情况，则需要进入将参数拷贝给方法的形参，进入到函数内部分析
			// TODO: 可以参照大玮哥发的代码，他们在这里写的还不错
		}
		if (u instanceof DefinitionStmt) {
			DefinitionStmt dsu = (DefinitionStmt)u;
			Value rightOp = dsu.getRightOp();
			Value leftOp = dsu.getLeftOp();
			ObjectInfo leftObj = new ObjectInfo(0, leftOp.toString(), "default", "main");

			if (rightOp instanceof NewExpr) {
				//System.out.println("DefinitionStmt***NewExpr\n");
				ObjectInfo rightObj = new ObjectInfo(allocID, rightOp.toString(), "default", "main");
				Set<ObjectInfo> newSet = new HashSet<ObjectInfo>();
				newSet.clear();
				newSet.add(rightObj);
				out.insert(leftObj, newSet);
			}
			if (leftOp instanceof Local && rightOp instanceof Local) {
				//System.out.println("DefinitionStmt***local_local\n");
				ObjectInfo rightObj = new ObjectInfo(0, rightOp.toString(), "default", "main");
				Set<ObjectInfo> rightSet = out.getKey(rightObj);
				Set<ObjectInfo> newSet = PointToSet.deepCloneSet(rightSet);
				out.insert(leftObj, newSet);
				//anderson.addAssignConstraint(generateName(methodname, ((DefinitionStmt) u).getRightOp().toString()), generateName(methodname, ((DefinitionStmt) u).getLeftOp().toString()));
				//System.out.println(((DefinitionStmt) u).getRightOp().toString());
				//System.out.println((((DefinitionStmt) u).getLeftOp().toString()));
			}
			// TODO: 如果rightOP不是local而是InstanceFieldRef等，则需要考虑域敏感分析
		}
		outputSet = out;
		System.out.println("Dump In: ");
		in.dumpPointToSet();
		System.out.println("\nDump Out: ");
		out.dumpPointToSet();

		// perform generation (kill set is empty)
		// in.union(genSet, out);
	}

	/**
	 * All paths == Intersection.
	 **/
	protected void merge(Object in1, Object in2, Object out) {
		PointToSet
				inSet1 = (PointToSet) in1,
				inSet2 = (PointToSet) in2,
				outSet = (PointToSet) out;

		inSet1.merge(inSet2, outSet);
	}

	protected void copy(Object source, Object dest) {
		PointToSet
				sourceSet = (PointToSet) source,
				destSet = (PointToSet) dest;

		sourceSet.copy(destSet);
	}
}

// 保存变量的信息，主要由两类，allocID==0表示引用；大于零表示对象，同时表示对象分配点的值。
// 例如 $r3 = new benchmark.objects.A
// 则 $r3的信息为：allocID: 0 object name: $r3 in METHOD main of CLASS default
// 分配的对象的信息：allocID: 1 object name: new benchmark.objects.A in METHOD main of CLASS default
// 表示该对象是在1处分配的
// 定义了一个类来表示信息优点是在域敏感和过程间分析的时候可能会简单一点；但是这样也有缺点，在指向集中可能会重复保存具有相同信息的对象
// 所以保存的时候需要进行一下判断，已经写好了相关的方法。

 class ObjectInfo {
	int allocID ; // 0: variable in test invokestmt; >0: object
	String objectName;
	String inClassName;
	String inMethodName;
	//Set<ObjectInfo> field;
	ObjectInfo(int id, String objectname, String inclassname, String inmethodname) {
		allocID = id;
		objectName = objectname;
		inClassName = inclassname;
		inMethodName = inmethodname;
		//field = new HashSet<ObjectInfo>();
	}

	int getAllocID() {
		return allocID;
	}

	String getBaseClassName() {
		return objectName;
	}

	String getInMethodName() {
		return inMethodName;
	}

	void dumpObjectInfo() {
		System.out.println("\tallocID: " + allocID + " object name: " + objectName
				+ " in METHOD " + inMethodName + " of CLASS " + inClassName);
	}
	boolean equal(ObjectInfo obj) {
		if (obj == null) {
			return false;
		}
		if (this.allocID == obj.allocID && this.objectName.equals(obj.objectName) &&
		   obj.inClassName.equals(obj.inClassName) && this.inMethodName.equals(obj.inMethodName)) {
			return true;
		}
		return false;
	}

	 ObjectInfo deepClone() {
		 ObjectInfo newObj = new ObjectInfo(this.allocID, this.objectName, this.inClassName, this.inMethodName);
		 return newObj;
	 }
 }
//
class PointToSet
{
	Map<ObjectInfo, Set<ObjectInfo>> pointSet;

	PointToSet() {
		pointSet = new HashMap<ObjectInfo, Set<ObjectInfo>>();
		pointSet.clear();

	}

	Map<ObjectInfo, Set<ObjectInfo>> getPointSet() {
		return pointSet;
	}

	boolean containsKey(ObjectInfo key) {
		for (ObjectInfo pointer : pointSet.keySet()) {
			if (pointer.equal(key)) {
				return true;
			}
		}
		return false;
	}

	Set<ObjectInfo> getKey(ObjectInfo key) {
		ObjectInfo pointer = aliasKey(key);
		return pointSet.get(pointer);
	}

	ObjectInfo aliasKey(ObjectInfo key) {
		for (ObjectInfo pointer : pointSet.keySet()) {
			if (pointer.equal(key)) {
				return pointer;
			}
		}
		Set<ObjectInfo> valueSet = new HashSet<ObjectInfo>();
		valueSet.clear();
		pointSet.put(key, valueSet);
		return key;
	}

	PointToSet deepClone() {
		PointToSet newSet = new PointToSet();
		for(ObjectInfo pointer : this.pointSet.keySet()) {
			ObjectInfo newKey = pointer.deepClone();
			Set<ObjectInfo> newValueSet = deepCloneSet(getKey(pointer));
			newSet.insert(newKey, newValueSet);
		}
		return newSet;
	}

	static Set<ObjectInfo> deepCloneSet(Set<ObjectInfo> oldSet) {
		Set<ObjectInfo> newValueSet = new HashSet<ObjectInfo>();
		newValueSet.clear();
		Iterator entries = oldSet.iterator();
		while(entries.hasNext()){
			ObjectInfo value = (ObjectInfo)entries.next();
			newValueSet.add(value.deepClone());
		}
		return newValueSet;
	}

	void assign(PointToSet pSet) {
		pSet.pointSet = this.pointSet;
	}

	void copy(PointToSet pSet) {
		PointToSet newSet = this.deepClone();
		pSet.pointSet = newSet.pointSet;
	}

	void insert(ObjectInfo key, Set<ObjectInfo> value) {
		ObjectInfo pointer = aliasKey(key);
		pointSet.remove(pointer);
		pointSet.put(pointer, value);
	}

	void append(ObjectInfo key, Set<ObjectInfo> valueSet) {
		Set<ObjectInfo> oldValueSet = getKey(key);
		Iterator entries = valueSet.iterator();
		while(entries.hasNext()){
			ObjectInfo value = (ObjectInfo)entries.next();
			if (!setContains(oldValueSet, value)) {
				oldValueSet.add(value.deepClone());
			}
		}
	}

	void merge(PointToSet pSet, PointToSet newSet) {
		this.copy(newSet);
		for(ObjectInfo pointer : pSet.getPointSet().keySet()) {
			ObjectInfo newKey = pointer.deepClone();
			Set<ObjectInfo> newValueSet = deepCloneSet(pSet.getKey(pointer));
			newSet.append(newKey, newValueSet);
		}
	}

	static boolean setContains(Set<ObjectInfo> valueSet, ObjectInfo value) {
		Iterator entries = valueSet.iterator();
		while(entries.hasNext()){
			ObjectInfo e = (ObjectInfo)entries.next();
			if (e.equal(value)) {
				return true;
			}
		}
		return false;
	}

	void dumpPointToSet() {
		for (ObjectInfo pointer : pointSet.keySet()) {
			dumpKey(pointer);
		}
	}

	void dumpKey(ObjectInfo key) {
		System.out.println("Pointer: ");
		key.dumpObjectInfo();
		Set<ObjectInfo> valueSet = getKey(key);

		System.out.println("Object Set: ");
		Iterator entries = valueSet.iterator();
		while(entries.hasNext()){
			ObjectInfo value = (ObjectInfo)entries.next();
			value.dumpObjectInfo();
		}
		System.out.println("\n");
	}
}
